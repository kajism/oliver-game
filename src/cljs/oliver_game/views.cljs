(ns oliver-game.views
  (:require
   [clojure.string :as str]
   [oliver-game.common :as common :refer [etv]]
   [oliver-game.events :as events]
   [oliver-game.subs :as subs]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]))

(def burgery [{:img "classic-burger.png"
               :cena 20}
              {:img "kecup-burger.png"
               :cena 24}
              {:img "file-burger.png"
               :cena 15}
              {:img "burger-kure.png"
               :cena 25}
              {:img "big-mac.png"
               :cena 80}])

(defn button [x y label on-click color]
  [:g {:transform (str "translate(" x ", " y ")") :on-click on-click}
   [:rect {:width 200 :height 50 :stroke "black" :fill color}]
   [:text {:x 10 :y 35 :fill "black" :style {:font-size "200%" :cursor "pointer" :user-select "none"}} label]])


(defn inventory []
  (let [x0 200
        y0 200]
    [:g
     [:rect {:x x0 :y y0 :width 500 :height 500 :stroke "black" :fill "lightgray"}]
     (doall
      (for [x (range 5)
            y (range 5)]
        [:rect {:x (+ x0 (* 100 x)) :y (+ y0 (* 100 y)) :width 100 :height 100 :stroke "black" :fill "yellow"}]))]))

(defn mini-adventures []
  (let [show-villager? (reagent/atom false)
        show-inventory? (reagent/atom false)]
    (fn []
      [:svg {:width 1000 :viewBox "0 0 1509 1104"}
       [:image {:x 0 :y 0 :xlink-href "/img/mapa-hry.png" :width 1509 :height 1104}]
       (when @show-villager?
         [:image {:x 400 :y 350 :width 310 :height 467 :xlink-href "/img/villager.png" :on-click #(reset! show-villager? false)}])
       [:rect {:x 643 :y 430 :width 20 :height 100 :fill "yellow" :fill-opacity "0.04" :stroke "none" :on-click #(reset! show-villager? true)}]
       [button 50 50 "Inventář" #(swap! show-inventory? not) "lightgray"]
       (when @show-inventory?
         [inventory])])))

(defn panel []
  (let [show-milk-shakes? (re-frame/subscribe [::subs/show? :show-milk-shakes?])
        celkem (reagent/atom 0)
        ;;pokladna (reagent/atom (int (rand 84)))
        burger-idx (reagent/atom (int (rand 3)))
        show-game? (reagent/atom false)
        mini-adventures? (reagent/atom true)
        chat-input (reagent/atom "")]
    (fn []
      [:div
       [:h1 "Oliver's Game Page"]
       (cond
         @mini-adventures?
         [mini-adventures]
         @show-game?
         [:svg {:width 1000 :viewBox "0 0 1316 933"}
          [:image {:x 0 :y 0 :xlink-href "/img/background.png" :width 1316 :height 933}]
          [:circle {:cx 990 :cy 170 :r 27 :fill "#f0f0f0" :on-click #(re-frame/dispatch [::events/toggle :show-mayonaise?])
                    :opacity (if @(re-frame/subscribe [::subs/show? :show-mayonaise?]) "0.0" "1.0")}]
          #_[:image {:x 200 :y 130 :xlink-href "/img/coffees.png" :width 194 :height 172}]
          [:rect {:x 960 :y 800 :width 100 :height 130 :stroke "orange" :fill "orange" :fill-opacity "0.04"
                  :style {:cursor "pointer"}
                  :on-click #(re-frame/dispatch [::events/toggle :show-hamburgers?])}]
          [:rect {:x 1090 :y 800 :width 100 :height 130 :stroke "yellow" :fill "yellow" :fill-opacity "0.04"
                  :style {:cursor "pointer"}
                  :on-click #(re-frame/dispatch [::events/toggle :show-new-trades?])}]
          #_[:rect {:x 1200 :y 800 :width 115 :height 130 :stroke "red" :fill "red" :fill-opacity "0.04"
                    :style {:cursor "pointer"}
                    :on-click #(re-frame/dispatch [::events/toggle :show-new-trades?])}]
          (when @(re-frame/subscribe [::subs/show? :show-new-trades?])
            [:image {:x 170 :y 130 :xlink-href "/img/new-trades.png" :width 598 :height 419}])
          (when @(re-frame/subscribe [::subs/show? :show-hamburgers?])
            [:image {:x 170 :y 130 :xlink-href "/img/hamburgers.png" :width 582 :height 313}])
          #_[:image {:x 1050 :y 200 :xlink-href "/img/ice-creams.png" :width 285 :height 137
                     :style {:cursor "pointer"}
                     :on-click #(re-frame/dispatch [::events/toggle :show-ice-creams?])
                     :opacity (if @(re-frame/subscribe [::subs/show? :show-ice-creams?]) "1.0" "0.2")}]
          #_[:image {:x 1050 :y 400 :xlink-href "/img/milk-shakes.png" :width 242 :height 187
                     :style {:cursor "pointer"}
                     :on-click #(re-frame/dispatch [::events/toggle :show-milk-shakes?])
                     :opacity (if @show-milk-shakes? "1.0" "0.2")}]
          #_[:image {:x 1050 :y 600 :xlink-href "/img/hot-dogs.png" :width 290 :height 163
                     :style {:cursor "pointer"}
                     :on-click #(re-frame/dispatch [::events/toggle :show-milk-shakes?])
                     :opacity (if @show-milk-shakes? "1.0" "0.2")}]
          [:image {:x 880 :y 115 :xlink-href (str "/img/" (get-in burgery [@burger-idx :img])) :width 100 :height 80
                   :style {:cursor "pointer"}}]
          [:text {:x 660 :y 35 :fill "black" :style {:font-size "300%" :cursor "pointer" :font-weight "bold"}}
           (if (>= @celkem 100) "2" "1")]
          #_[:g {:transform "translate(800, 2)" :on-click #()}
             [:rect {:width 110 :height 50 :stroke "black" :fill "lightgray"}]
             [:text {:x 10 :y 35 :fill "black" :style {:font-size "200%" :cursor "pointer" :user-select "none"}} "MENU"]]
          [button 1080 2 "ODMÍTNOUT" #(do
                                        (re-frame/dispatch [::events/hide-all])
                                        (reset! burger-idx (int (rand 3)))
                                        (swap! celkem (partial + -5)))
           "pink"]
          [:g {:transform "translate(10, 12)" :on-click #()}
           [:rect {:width 60 :height 35 :stroke "white" :fill "white" :fill-opacity "0.5"}]
           [:text {:x 0 :y 27 :fill "DarkGreen"
                   :style {:font-size "180%" :font-weight "bold" :cursor "pointer" :user-select "none"}
                   :on-click #()}
            "$" @celkem]]
          [:g {:transform "translate(20, 335)" :on-click #(re-frame/dispatch [::events/hide-all])}
           [:rect {:width 60 :height 35 :stroke "white" :fill "white" :fill-opacity "0.5"}]
           [:text {:x 0 :y 27 :fill "DarkGreen"
                   :style {:font-size "180%" :font-weight "bold" :cursor "pointer" :user-select "none"}
                   ;;:on-click #(reset! pokladna (int (rand 84)))
                   }
            "$" (get-in burgery [@burger-idx :cena])]]
          
          ]
         :default
         [:div
          [:h3 "Vážení návštěvníci, rád bych vám sdělil, že moje hra sice není úplně dokončená, ale můžete si vyskoušet tuto hru a postupně ji budeme vylepšovat."]
          [:h3 "Chat"]
          [:pre @(re-frame/subscribe [::subs/chat])]
          [:input {:type "text" :on-change #(reset! chat-input (etv %)) :value @chat-input
                   :style {:width "600px"}} ]
          [:button {:on-click #(do
                                 (re-frame/dispatch [::events/send @chat-input])
                                 (reset! chat-input ""))} "Odeslat"]
          [:br]
          [:br]
          [:button {:on-click #(reset! show-game? true)} "Hrát hru"][:br][:br]
          [:button {:on-click #(reset! mini-adventures? true)} "Hrát hru Mini adventures!"]]

         )])))
