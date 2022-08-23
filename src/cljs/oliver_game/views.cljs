(ns oliver-game.views
  (:require
   [clojure.string :as str]
   [oliver-game.common :refer [etv]]
   [oliver-game.events :as events]
   [oliver-game.subs :as subs]
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]))

(defn hexagon [& {:keys [x y s color from-idx to-idx pattern] :or {s 2.3 color "gray" from-idx 0 to-idx 7 pattern "coconut"}}]
  (let [polygon? (= 7 (- to-idx from-idx))
        river? (= color "blue")
        points (-> (str/split "50,0 100,25 100,75 50,100 0,75 0,25 50,0" #"\s")
                   (vec)
                   (subvec from-idx to-idx))]
    [:g {:transform (str "translate(" x "," y "), scale(" s " " s ")")}
     [(if polygon? :polygon :polyline)
      (cond->
        {:points (str/join " " points) :fill "none" :stroke color :stroke-width s :stroke-linecap "round"}
        (and pattern (not river?))
        (assoc :fill (str "url(#" pattern ")")))]
     (when river?
       (doall
         (for [p (set points)
               :let [[x y] (str/split p #",")]]
           ^{:key [x y]}
           [:circle {:cx x :cy y :r 10 :stroke "gray" :fill "none"}])))])
  )

(defn sea-travel [sea-travel?]
  [:div
   [:h2 "Sea travel"]
   (let [x0 20
         y0 20
         dx 320
         dy 245
         x02 155
         s 3.2]
     [:svg {:width 1000 :viewBox "0 0 1535 1104"}
      [:defs
       [:pattern {:id "coconut" :height "100%" :width "100%" :patern-content-units "objectBoundingBox"}
        [:image {:height "92" :width "68" :preserve-aspect-ratio "none" :xlink-href "/img/file-burger.png"}]]]
      #_[:image {:x 0 :y 0 :xlink-href "/img/mapa-hry.png" :width 1509 :height 1104}]
      (doall
        (for [x (->> (range 4) (map (partial * dx)))]
          ^{:key x}
          [hexagon :x (+ x x0 x02) :y (+ y0)]))

      (doall
        (for [x (->> (range 5) (map (partial * dx)))]
          ^{:key x}
          [hexagon :x (+ x x0 0) :y (+ y0 dy)]))

      (doall
        (for [x (->> (range 4) (map (partial * dx)))]
          ^{:key x}
          [hexagon :x (+ x x0 x02) :y (+ y0 (* dy 2))]))

      ;;river
      (doall
        (for [x (->> (range 3) (map (partial * dx)))]
          ^{:key x}
          [hexagon :x (+ x x0 x02 -45) :y -15 :color "blue" :s s :from-idx 1 :to-idx 3]))
      (doall
        (for [x (->> (range 3) (map (partial * dx)))]
          ^{:key x}
          [hexagon :x (+ x dx -30 0) :y (+ y0 -40 dy) :color "blue" :s s]))
      [hexagon :x -30 :y (+ y0 -40 dy) :color "blue" :s s :from-idx 0 :to-idx 4]
      [hexagon :x (+ -30 (* 4 dx)) :y (+ y0 -40 dy) :color "blue" :s s :from-idx 3 :to-idx 7]
      (doall
        (for [x (->> (range 3) (map (partial * dx)))]
          ^{:key x}
          [hexagon :x (+ x x0 x02 -45) :y 465 :color "blue" :s s :from-idx 1 :to-idx 3]))

      [:circle {:cx 450 :cy 65 :r 35 :stroke "none" :fill "purple"}]
      [:circle {:cx 1090 :cy 65 :r 35 :stroke "none" :fill "orange"}]
      [:circle {:cx 1090 :cy 705 :r 35 :stroke "none" :fill "red"}]
      [:circle {:cx 130 :cy 545 :r 35 :stroke "none" :fill "yellow"}]
      #_[:line {:x1 150 :y1 230 :x2 290 :y2 305 :stroke "blue" :stroke-width (* s s)}]

      ])
   [:button {:on-click #(reset! sea-travel? false)} "Zpět na Homepage"]])

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

(defn show-user [u]
  [:span [:span {:style {:color (get-in events/users [u :color])}} (get-in events/users [u :title])]
   " " u])

(defn panel []
  (let [celkem (reagent/atom 0)
        ;;pokladna (reagent/atom (int (rand 84)))
        burger-idx (reagent/atom (int (rand 3)))
        show-game? (reagent/atom false)
        mini-adventures? (reagent/atom false)
        sea-travel? (reagent/atom true)
        chat-input (reagent/atom "")
        user (re-frame/subscribe [::subs/get-in :user])]
    (fn []
      [:div
       [:h1 "Oliver's Game Page"]
       (cond
         @sea-travel?
         [sea-travel sea-travel?]
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
          (if-not @user
            [:div
             (let [nickname @(re-frame/subscribe [::subs/get-in [:login :nickname]])
                   pwd @(re-frame/subscribe [::subs/get-in [:login :pwd]])]
               [:div
                [:h4 "VIP Přihlášení"]
                [:div"Přezdívka: "
                 [:input {:type "text" :on-change #(re-frame/dispatch [::events/set-in [:login :nickname] (etv %)])
                          :value nickname
                          :style {:width "200px"}} ]]
                [:div"Heslo: "
                 [:input {:type "password" :on-change #(re-frame/dispatch [::events/set-in [:login :pwd] (etv %)])
                          :value pwd
                          :style {:width "200px"}} ]]
                (when-let [err-msg @(re-frame/subscribe [::subs/get-in [:login :error]])]
                  [:div {:style {:color "red"}} err-msg])
                [:button {:on-click #(re-frame/dispatch [::events/login])} "Přihlásit se"]])
             [:br]
             (let [nickname @(re-frame/subscribe [::subs/get-in :nickname])]
               [:div
                [:h4 "Přihlášení pod jménem"]
                [:div"Přezdívka: "
                 [:input {:type "text" :on-change #(re-frame/dispatch [::events/set-in :nickname (etv %)])
                          :value nickname
                          :style {:width "200px"}} ]]
                [:button {:on-click #(re-frame/dispatch [::events/set-in :user nickname])} "Zaregistrovat"]])]
            [:h1 [show-user @user]
             [:button {:on-click #(re-frame/dispatch [::events/set-in :user nil])} "Odhlásit se"]])
          [:h3 "Chat"]
          (let [chat @(re-frame/subscribe [::subs/chat])
                lines (str/split chat #"\n")]
            (doall
             (for [line lines
                   :let [[u text] (str/split line #":")]
                   :when (not (str/blank? text))]
               [:div [show-user u] ": " text]))
            )
          (when @user
            [:div
             @user ": "
             [:input {:type "text" :on-change #(reset! chat-input (etv %)) :value @chat-input
                      :style {:width "600px"}} ]
             [:button {:on-click #(do
                                    (re-frame/dispatch [::events/send (str @user ": " @chat-input)])
                                    (reset! chat-input ""))} "Odeslat"]])
          [:br]
          [:br]
          [:button {:on-click #(reset! sea-travel? true)} "Hrát hru Sea travel!"]
          ;;[:button {:on-click #(reset! show-game? true)} "Hrát hru"][:br][:br]
          ;;[:button {:on-click #(reset! mini-adventures? true)} "Hrát hru Mini adventures!"]
          ]

         )])))
