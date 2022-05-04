(ns oliver-game.system
  (:require [ring.adapter.undertow :as http]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [nrepl.server]
            [oliver-game.common :as common]
            [oliver-game.endpoint.routes :as routes]
            [oliver-game.server :as server])
  (:import (io.undertow Undertow)))

(def config
  {:nrepl/server {:port (parse-long (or (env :nrepl-port) "7044"))}
   :app/event-queue {:capacity 100}
   :app/ctx {}
   :app/server {:event-queue (ig/ref :app/event-queue)
                :ctx (ig/ref :app/ctx)}
   :app/handler {:event-queue (ig/ref :app/event-queue)
                 :app/server (ig/ref :app/server) ;; everything should be started before handling requests
                 }
   :http/server {:handler (ig/ref :app/handler)
                 :port (parse-long (or (env :port) "3002"))}})

(defmethod ig/init-key :nrepl/server [_ {:keys [port]}]
  (nrepl.server/start-server :port port))

(defmethod ig/halt-key! :nrepl/server [_ server]
  (nrepl.server/stop-server server))

(defmethod ig/init-key :app/event-queue [_ {:keys [capacity]}]
  (server/create-queue capacity))

(defmethod ig/init-key :app/ctx [_ _]
  (server/create-ctx))

(defmethod ig/init-key :app/handler [_ deps]
  (routes/make-handler deps))

(defmethod ig/init-key :http/server [_ {:keys [handler port]}]
  (log/info "Starting HTTP server on port" port)
  (http/run-undertow handler {:host "0.0.0.0"
                              :port port}))

(defmethod ig/halt-key! :http/server [_ ^Undertow server]
  (.stop server))

(defmethod ig/init-key :app/server [_ subsystems]
  (server/starting subsystems))

(defmethod ig/halt-key! :app/server [_ subsystems]
  (server/stopping subsystems))
