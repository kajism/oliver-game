(ns oliver-game.websocket
  (:require [cognitect.transit :as t]
            [re-frame.core :as re-frame]
            [re-frame.db :refer [app-db]]))

(defonce ws-conn (atom nil))

(def t-writer (t/writer :json))
(def t-reader (t/reader :json))

(defn ws-receive [msg]
  (let [msg (.-data msg)
        data (t/read t-reader msg) ;; edn: [cljs.reader :as reader] (reader/read-string msg)
        data (if (map? data) [data] data)]
    #_(println "Browser received WS data '" msg "'")
    (doseq [msg data]
      (re-frame/dispatch [:oliver-game.events/ws-received msg]))))

(defn ws-error [evt]
  (.error js/console "WebSocket close observed:" evt)
  (re-frame/dispatch [:oliver-game.events/ws-error? true]))

(defn make-websocket []
  (let [url (str (if (= (.-protocol js/location) "https:") "wss:" "ws:") "//" (.-host js/location) "/ws")]
    #_(println "Attempting to connect " url)
    (if-let [chan (js/WebSocket. url)]
      (do
        (set! (.-onmessage chan) ws-receive)
        (set! (.-onerror chan) #(.log js/console "WebSocket error observed:" %))
        (set! (.-onclose chan) ws-error)
        (reset! ws-conn chan)
        #_(println "Websocket connection established with: " url))
      (throw (js/Error. "Websocket connection failed!")))))

(defn ws-open? [ws]
  (when ws
    (= (.-readyState ws) (.-OPEN ws))))

(defn ws-connecting? [ws]
  (when ws
    (= (.-readyState ws) (.-CONNECTING ws))))

(defn check-websocket []
  #_(println "Ws state: " (and @ws-conn (.-readyState @ws-conn)))
  (when (or (nil? @ws-conn)
            (not (or (ws-open? @ws-conn) (ws-connecting? @ws-conn))))
    (make-websocket)))

(defn ws-send [msg]
  (check-websocket)
  (if (ws-connecting? @ws-conn)
    (do
      #_(println "Ws still connecting, trying later")
      (js/setTimeout #(ws-send msg) 500))
    (do
      #_(println "Sending by ws: " msg)
      (.send @ws-conn (t/write t-writer (assoc msg :user (get-in @app-db [:init-info :user :username])))))))

(defn server-call-effect [msgs]
  (let [msgs (if (sequential? msgs) msgs [msgs])]
    (doseq [msg msgs]
      (ws-send msg))))

(re-frame/reg-fx :ws-call server-call-effect)
