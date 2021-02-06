(ns oliver-game.server
  (:require [aleph.http :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cognitect.transit :as t]
            [environ.core :refer [env]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources not-found]]
            [manifold.bus :as bus]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [nrepl.server]
            [ring.util.request :as req]
            [ring.util.response :as resp]
            [ring.middleware.defaults :as ring-defaults]
            [oliver-game.app :as app]
            [oliver-game.common :as common]
            [oliver-game.hiccup :as hiccup]
            [oliver-game.middleware :as middleware])
  (:import java.io.ByteArrayOutputStream
           java.io.ByteArrayInputStream
           java.net.JarURLConnection
           java.util.Date
           java.util.Locale
           java.text.SimpleDateFormat))

(defn try-put! [s x]
  (s/try-put! s x 1500))

(defn app-version-info
  "Returns a map with :date instant and :version number string"
  []
  (let [pom-url (io/resource "META-INF/maven/oliver-game/oliver-game/pom.properties")
        pom (some-> pom-url
                    (slurp)
                    (str/split #"\n")
                    (->> (map #(str/split % #"="))
                         (filter #(= 2 (count %)))
                         (into {})))
        date-parser (SimpleDateFormat. "EEE MMM dd HH:mm:ss z yyyy" Locale/US)]
    {:date (let [c (.openConnection pom-url)]
             (Date.
              (if (instance? JarURLConnection c)
                (.getTime (.getJarEntry ^JarURLConnection c))
                (let [ms (.getLastModified c)]
                  (.close c)
                  ms))))
     :version (get pom "version")}))

;; command message execution and publishing

(defmulti exec! (fn [track msg]
                  (:type msg)))

(def db-filename "./db.txt")

(defmethod exec! :chat [_ msg]
  (spit db-filename (str (:msg msg) "\n") :append true))

(defmethod exec! :default [track msg]
  (log/error "Unknown exec msg type!" msg))

(defn publish! [{:keys [trckn ack-ts type trn] :as msg}]
  (let [{:keys [db event-bus tracks] :as ctx} @app/ctx
        now (Date.)
        msg (cond-> msg
              :allways
              (assoc :timestamp now)
              ack-ts
              (assoc :round-trip-ms (- (.getTime now) (.getTime ack-ts))))]

    (bus/publish! event-bus ::trains msg)))

;; websockets

(defn from-transit [msg]
  (t/read (t/reader (ByteArrayInputStream. (.getBytes msg "UTF-8")) :json)))

(defn to-transit [msg]
  (let [out (ByteArrayOutputStream.)]
    (t/write (t/writer out :json) msg)
    (.toString out "UTF-8")))

(defn ws-put! [conn msg]
  (try-put! conn (to-transit msg)))

(defn put-all-state! [conn tracks user]
  (ws-put! conn {:type :app-domains
                 :result (env :app-domains)})
  (ws-put! conn {:type :init-info
                 :result {:version-info (app-version-info)
                          :user user}})
  (doseq [line #p(str/split (slurp db-filename) #"\n")]
    (ws-put! conn #p{:type :chat
                   :msg line})))

(defn ws-msg-consumer [{:keys [remote-addr session] :as req} conn msg]
  (let [{:keys [trckn type trn] :as msg} (from-transit msg)] ;;edn: (edn/read-string msg)
    (case type
      :get-init-info
      (put-all-state! conn (:tracks @app/ctx) (:user session))
      ;;default
      (let [tracks (:tracks @app/ctx)
            {:keys [trains-by-name table-tags] :as track} (get tracks trckn)
            train (if-not trn ::no-name-msg (get trains-by-name trn ::not-found))]
        (log/debug "Server received by WS:" msg)
        (cond
          :default
          (publish! msg))))))

(defn ws-handler [{:keys [remote-addr session] :as req}]
  (d/let-flow [conn (d/catch
                     (http/websocket-connection req)
                     (fn [_] nil))]
              (if-not conn
                {:status 400 ;; if it wasn't a valid websocket handshake, return an error
                 :headers {"content-type" "application/text"}
                 :body "Expected a websocket request."}
                (do
                  (log/info "Creating WS connection with" remote-addr "and subscribing to ::trains topic")

                  ;; take all messages from the ::trains topic, and feed them to the client
                  (s/connect-via
                   (bus/subscribe (:event-bus @app/ctx) ::trains)
                   (fn bus-to-ws [{:keys [type] :as msg}]
                     #_(log/debug "Trying to put to websocket " remote-addr ":" msg)
                     (if-not (or (= type :update-waiting-secs)
                                 (and (= type :udp/tag)
                                      (= (:tag msg) :repeat)))
                       (s/put! conn (to-transit msg)) ;;edn: (pr-str msg)
                       (d/success-deferred true)))
                   conn
                   {:timeout 10e3})

                  ;; consume all messages from the client
                  (s/consume
                   (partial #'ws-msg-consumer req conn)
                   conn)

                  nil))))

(defn bus-events-consumer [{:keys [trckn trn host type] :as msg}]
  (let [{:keys [listen-to-bus-events? tracks] :as ctx} @app/ctx]
    (when listen-to-bus-events?
      ;; bus events logging
      (log/info "BUS EVENT:" (pr-str msg))
      (try
        (let [{:keys [publish publish-n publish-later]}
              (swap! app/ctx #(let [{:keys [track publish publish-n publish-later]}
                                    (exec! (get-in % [:tracks trckn]) msg)]
                                (cond-> %
                                  track
                                  (assoc-in [:tracks trckn] track)
                                  :always
                                  (assoc :publish publish
                                         :publish-n publish-n
                                         :publish-later publish-later))))]
          (future
            (doseq [msg (conj publish-n publish)
                    :when msg]
              (publish! msg)))
          (doseq [{:keys [ms publish]} publish-later]
            (future
              (Thread/sleep ms)
              (publish! publish))))
        (catch Exception e
          (log/error e))))))

;; HTTP server

(defroutes routes
  (GET "/" [] (hiccup/cljs-landing-page (:version (app-version-info))))

  (GET "/css/site.css/:version" [version]
    (-> (resp/resource-response "site.css" {:root "public/css"})
        (resp/content-type "text/css")
        (resp/header "ETag" (str "Version-" (:version (app-version-info))))))

  (GET "/js/compiled/app.js/:version" [version]
    (-> (resp/resource-response "app-main.js" {:root "public/cljs-out"})
        (resp/content-type "application/javascript")
        (resp/header "ETag" (str "Version-" (:version (app-version-info))))))

  (GET "/ws" [] ws-handler)
  (resources "/")
  (not-found "Page not found."))

(def handler (-> #'routes
                 ;;middleware-json/wrap-json-response
                 ;;middleware/wrap-auth
                 (ring-defaults/wrap-defaults (->  ring-defaults/site-defaults
                                                   (assoc :proxy true
                                                          :security {:anti-forgery false})))))

(defn start-http-server [port]
  (log/info "Starting HTTP server on port" port)
  (http/start-server handler {:port port}))

(defn create-event-bus []
  (let [buffer-size 50]  ;;buffer needed? deadlocks?!
    (log/info "Creating event bus with buffer" buffer-size)
    (let [event-bus (bus/event-bus #(s/stream buffer-size))]
      (s/consume
       #'bus-events-consumer
       (bus/subscribe event-bus ::trains))
      event-bus)))

(def config (array-map
             :http-server [(partial start-http-server (Integer/parseInt (or (env :port) "3002")))
                           #(.close %)]
             :nrepl-server [#(nrepl.server/start-server :port 7042)
                            nrepl.server/stop-server]
             :event-bus [create-event-bus nil]
             ))

(defn start-all []
  (run! (fn [[k [start-fn stop-fn]]]
          (try
            (log/info "Starting" k)
            (swap! app/ctx update k #(do
                                       (when (and stop-fn %)
                                         (stop-fn %))
                                       (start-fn)))
            (catch Exception e
              (log/error "Error starting" k e))))
        config))

(defn stop-all []
  (run! (fn [[k [start-fn stop-fn]]]
          (try
            (log/info "Stopping" k)
            (swap! app/ctx update k #(when (and stop-fn %)
                                       (stop-fn %)))
            (catch Exception e
              (log/error "Error stopping" k e))))
        (reverse config)))

