(ns oliver-game.main
  (:require [clojure.tools.logging :as log]
            [oliver-game.server :as server])
  (:gen-class))

(defn -main [& args]
  (server/start-all)
  (.addShutdownHook (Runtime/getRuntime) (Thread. #'server/stop-all)))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error "Uncaught exception in thread" (.getName thread) ":" ex))))
