(ns oliver-game.middleware
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [ring.util.response :as response]))

(def access-denied-response {:status 403
                             :headers {"Content-Type" "text/plain;charset=utf-8"}
                             :body "Přístup odmítnut. Aktualizujte stránku a přihlašte se."})

(def login-redirect (response/redirect "/login"))

(defn wrap-auth [handler]
  (fn [request]
    (let [user (get-in request [:session :user])
          login? (= "/login" (:uri request))
          api-call? (re-find #"/ws" (:uri request))]
      (cond
        (or user login? api-call?)
        (handler request)
        ;; api-call? ;; this is solved in server.ws-cmd-handler
        ;; access-denied-response
        :else
        login-redirect))))
