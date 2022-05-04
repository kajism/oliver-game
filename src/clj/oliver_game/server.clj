(ns oliver-game.server
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [oliver-game.common :as common]
            [oliver-game.net :as net]
            [oliver-game.version :as version])
  (:import java.util.Date))

;; command message execution and publishing
(defmulti process-event! (fn [_ msg]
                           (:type msg)))

(defmethod process-event! :default [_ msg]
  (log/error "Unknown exec msg type!" msg))

(def db-filename "./db.txt")

(defmethod process-event! :chat [_ msg]
  (spit db-filename (str (:msg msg) "\n") :append true))

(defn publish! [queue msg]
  (net/offer queue (assoc msg :timestamp (Date.))))

;; websockets
(defn put-all-state! [chann user]
  (net/ws-send chann {:type :init-info
                      :result {:version-info (version/app-version-info)}})
  (doseq [line (str/split (slurp db-filename) #"\n")]
    (net/ws-send chann {:type :chat
                        :msg line})))

(defn ws-process-client-only-msg [chann user {:keys [type] :as msg}]
  (case type
    :ws-client/get-init-info
    (put-all-state! chann user)))

(defn ws-consumer [event-queue {:keys [remote-addr session] :as req} chann msg]
  (let [{:keys [trckn type trn] :as msg} (net/from-transit msg) ;;(edn/read-string msg)
        type-ns (namespace type)]
    (log/debug "WS received: " msg)
    (if (= type-ns "ws-client")
      (ws-process-client-only-msg chann (:user session) msg)
      (publish! event-queue msg))))

(defn msg-server-side-only? [{:keys [type tag]}]
  (or (contains? #{:update-waiting-secs :send-all-bridge-cmds :bridge-reserved} type)
      (and (= type :udp/tag) (= tag :repeat))))

(defn event-consumer [event-queue ctx {:keys [trckn] :as msg}]
  (when-not (msg-server-side-only? msg)
    (net/ws-send-to-clients msg))

  (let [{:keys [listen-to-events? db-skip? tracks]} @ctx]
    (when listen-to-events?
      (log/info "EVENT:" (pr-str msg))

      (let [{:keys [track publish publish-n publish-later socket-tx]}
            (process-event! (get tracks trckn) msg)]
        (doseq [msg (conj publish-n publish)
                :when msg]
          (event-consumer event-queue ctx (assoc msg :timestamp (Date.))))

        (doseq [{:keys [ms publish]} publish-later]
          (future
            (Thread/sleep ms)
            (publish! event-queue publish)))))))

(defn create-queue [capacity]
  (log/info "Creating queue with capacity" capacity)
  (net/create-queue capacity))

(defn create-ctx []
  (atom {:running? true
         :listen-to-events? true}))

(defn starting [{:keys [event-queue ctx] :as subsystems}]
  (log/info "Starting. Connecting subsystems...")

  (net/run-loop
    "System queue consumer Loop" 0 #(:running? @ctx)
    #(->> (net/consume event-queue)
          (#'event-consumer event-queue ctx)))

  subsystems)

(defn stopping [{:keys [event-queue ctx]}]
  (log/info "Stopping system...")
  (publish! event-queue {:type :chat
                         :msg "Server stopping!"})
  (Thread/sleep 400)
  (log/info "Stopping finished."))
