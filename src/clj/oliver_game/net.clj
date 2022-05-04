(ns oliver-game.net
  (:require [clojure.tools.logging :as log]
            [cognitect.transit :as t]
            [ring.adapter.undertow.websocket :as ws])
  (:import java.io.ByteArrayOutputStream
           java.io.ByteArrayInputStream
           java.net.SocketException
           (java.util.concurrent ArrayBlockingQueue BlockingQueue)))

(defn localhost? [host]
  (#{"127.0.0.1" "localhost" "::1"} host))

;; Transit
(defn from-transit [msg]
  (t/read (t/reader (ByteArrayInputStream. (.getBytes msg "UTF-8")) :json)))

(defn to-transit [msg]
  (let [out (ByteArrayOutputStream.)]
    (t/write (t/writer out :json) msg)
    (.toString out "UTF-8")))

;; Socket threads running
(defn run-loop [loop-name sleep-ms running?-fn looped-fn]
  (future
    (log/info loop-name " Starting")
    (while (running?-fn)
      (try
        (when-not (zero? sleep-ms)
          (Thread/sleep sleep-ms))
        (looped-fn)
        (catch SocketException e
          (log/info (str loop-name ": " (.getMessage e))))
        (catch Exception e
          (.printStackTrace e)
          (log/error (str loop-name " Exception: " (.getClass e) ": " (.getMessage e))))))
    (log/info loop-name " Finished")))

;; websockets
(def ws-timeout 10000)
(defonce ws-channels (atom #{}))
(comment
  (count @ws-channels)
  (type (first @ws-channels))
  (map #(.isOpen %) @ws-channels)
  (map #(.getBufferSize (.getBufferPool %)) @ws-channels))

(defn ws-send [channel msg]
  (ws/send-text (to-transit msg) channel))

(defn ws-connect! [channel]
  (log/info "WS channel created" (.getHostString (.getSourceAddress channel))
            "(" (.getHostString (.getDestinationAddress channel)) ")")
  (swap! ws-channels conj channel))

(defn ws-disconnect! [channel]
  (log/info "WS channel closed" (.getHostString (.getSourceAddress channel)))
  (swap! ws-channels disj channel))

(defn ws-send-to-clients [{:keys [type] :as msg}]
  (doseq [channel @ws-channels]
    (if (.isOpen channel)
      (ws/send-text (to-transit msg) ;;edn: (pr-str msg)
                    channel
                    nil
                    ws-timeout)
      (ws-disconnect! channel))))

(defn ws-handler [on-message-fn]
  {:undertow/websocket
   {:on-open (fn [{:keys [channel]}]
               (ws-connect! channel))
    :on-message (fn [{:keys [channel data]}]
                  (on-message-fn channel data))
    :on-close (fn [{:keys [ws-channel]}]
                (ws-disconnect! ws-channel))}})

;; event queue
(defn create-queue [capacity]
  (ArrayBlockingQueue. capacity true))

(defn offer [^BlockingQueue queue msg]
  (let [result (.offer queue msg)]
    (when-not result
      (log/error "Event queue did not accept msg" msg))
    result))

(defn consume [^BlockingQueue queue]
  (.take queue))

