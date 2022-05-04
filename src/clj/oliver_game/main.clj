(ns oliver-game.main
  (:require [clojure.tools.logging :as log]
            [oliver-game.system :refer [config]]
            [integrant.core :as ig])
  (:gen-class))

(def system (atom nil))

(defn -main []
  (reset! system (ig/init config))
  (let [^Runnable stop-fn #(ig/halt! @system)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-fn))))

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error "Uncaught exception in thread" (.getName thread) ":" ex))))
