(ns oliver-game.views
  (:require
   [clojure.string :as str]
   [oliver-game.common :as common]
   [oliver-game.events :as events]
   [oliver-game.subs :as subs]
   [re-frame.core :as re-frame]))

(defn panel []
  (let [show-hamburgers? (re-frame/subscribe [::subs/show? :show-hamburgers?])
        show-new-trades? (re-frame/subscribe [::subs/show? :show-new-trades?])
        show-ice-creams? (re-frame/subscribe [::subs/show? :show-ice-creams?])
        show-milk-shakes? (re-frame/subscribe [::subs/show? :show-milk-shakes?])]
    (fn []
      [:div
       [:h3 "Oliver game page"]
       [:svg {:width 1000 :viewBox "0 0 1316 933"}
        [:image {:x 0 :y 0 :xlink-href "/img/background.png" :width 1316 :height 933}]
        [:image {:x 200 :y 130 :xlink-href "/img/coffees.png" :width 194 :height 172}]
        [:rect {:x 960 :y 800 :width 100 :height 130 :stroke "orange" :fill "orange" :fill-opacity "0.04"
                :on-click #(re-frame/dispatch [::events/toggle :show-hamburgers?])}]
        [:rect {:x 1200 :y 800 :width 115 :height 130 :stroke "red" :fill "red" :fill-opacity "0.04"
                :on-click #(re-frame/dispatch [::events/toggle :show-new-trades?])}]
        (when @show-hamburgers?
          [:image {:x 170 :y 130 :xlink-href "/img/hamburgers.png" :width 582 :height 313}])
        (when @show-new-trades?
          [:image {:x 170 :y 130 :xlink-href "/img/new-trades.png" :width 598 :height 419}])
        [:image {:x 1050 :y 200 :xlink-href "/img/ice-creams.png" :width 285 :height 137
                 :on-click #(re-frame/dispatch [::events/toggle :show-ice-creams?])
                 :opacity (if @show-ice-creams? "1.0" "0.2")}]
        [:image {:x 1050 :y 400 :xlink-href "/img/milk-shakes.png" :width 242 :height 187
                 :on-click #(re-frame/dispatch [::events/toggle :show-milk-shakes?])
                 :opacity (if @show-milk-shakes? "1.0" "0.2")}]
        [:image {:x 1050 :y 600 :xlink-href "/img/hot-dogs.png" :width 290 :height 163
                 :on-click #(re-frame/dispatch [::events/toggle :show-milk-shakes?])
                 :opacity (if @show-milk-shakes? "1.0" "0.2")}]
        
        ]])))
