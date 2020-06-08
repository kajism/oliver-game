(ns ^:figwheel-hooks oliver-game.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [oliver-game.common :as common]
   [oliver-game.config :as config]
   [oliver-game.events :as events]
   [oliver-game.routes :as routes]
   [oliver-game.subs :as subs]
   [oliver-game.views :as views]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn- panels [panel-name]
  (if-not panel-name
    [common/loading]
    (case panel-name
      :home-panel [views/panel]
      [:div "Unknown panel"])))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [panels @active-panel]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [main-panel]
                  (.getElementById js/document "app")))

(defn ^:after-load re-render []
  (mount-root))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize])
  (re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (dev-setup)
  (mount-root))
