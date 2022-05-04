(ns user
  (:require [clojure.tools.namespace.repl]
            [figwheel.main]
            [figwheel.main.api]
            [integrant.repl :as ig-repl :refer [halt]]
            [integrant.repl.state :as ig-state]
            [oliver-game.system :as system]))

(clojure.tools.namespace.repl/set-refresh-dirs "src/clj" "src/cljc" "test/clj" "test/cljc")

(ig-repl/set-prep! (fn [] system/config))

(defonce figwheel-started? (atom false))

(defn reset []
  (ig-repl/reset)
  (when-not @figwheel-started?
    (figwheel.main.api/start {:mode :serve} "app")
    (reset! figwheel-started? true)))

(comment
  (figwheel.main.api/start {:mode :serve} "app")
  (figwheel.main.api/cljs-repl "app")
  (figwheel.main.api/stop "app")
  (figwheel.main/status)
  (figwheel.main/clean "app")

  (reset)
  (halt)

  (require '[reitit.core :as r])
  (require '[reitit.ring :as rring])
  (-> (:app/handler ig-state/system) (rring/get-router) (r/compiled-routes))

  (remove-ns 'user)

  (clojure.tools.namespace.repl/refresh-all)
  (clojure.tools.namespace.repl/clear)
  )

(defn ctx []
  (:app/ctx ig-state/system))

(defn db []
  (:sql/db ig-state/system))

(defn event-queue []
  (:app/event-queue ig-state/system))
