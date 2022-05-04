(ns oliver-game.common
  #?@(:clj
      [(:require
        [clojure.edn :as edn]
        [clojure.string :as str])
       (:import java.util.Locale)]
      :cljs
      [(:require
        [cljs.tools.reader.edn :as edn]
        [clojure.string :as str])]))

(def html-title "Oliver's Game")

(defn etv [ev]
  (-> ev .-target .-value))

(defn hours-mins [hours]
  (let [h (int hours)]
    (str h "h " (int (* (- hours h) 60)) "m")))

(defn loading []
  [:div
   [:div.ui.active.centered.huge.text.loader "Loading"]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]])
