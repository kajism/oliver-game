(ns oliver-game.core
  (:require [reagent.dom]
            [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as re-frame]
            [oliver-game.common :as common]
            [oliver-game.config :as config]
            [oliver-game.events :as events]
            [oliver-game.subs :as subs]
            [oliver-game.views :as views]
            [oliver-game.websocket :as websocket]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent-dom/render [views/panel {:shadow (js/Date.now)}]
                      (.getElementById js/document "app")))

(defn init []
  (re-frame/dispatch-sync [::events/initialize])
  (dev-setup)
  (mount-root))

