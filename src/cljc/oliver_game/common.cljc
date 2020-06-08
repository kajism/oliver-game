(ns oliver-game.common
  #?@(:clj
      [(:require
        [clj-time.coerce :as tc]
        [clj-time.core :as t]
        [clj-time.format :as tf]
        [clj-time.predicates :as tp]
        [clojure.edn :as edn]
        [clojure.string :as str])
       (:import java.util.Locale)]
      :cljs
      [(:require
        [cljs-time.coerce :as tc]
        [cljs-time.core :as t]
        [cljs-time.format :as tf]
        [cljs-time.predicates :as tp]
        [cljs.tools.reader.edn :as edn]
        [clojure.string :as str])]))

(def html-title "Oliver Game")

(defn parse-int [s]
  (when-not (str/blank? s)
    #?(:clj  (Long/parseLong s)
       :cljs (js/parseInt s))))

(defn parse-float [s]
  (when-not (str/blank? s)
    #?(:clj  (Float/parseFloat s)
       :cljs (js/parseFloat s))))

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

#?(:clj (def clj-tz (t/time-zone-for-id "Europe/Prague")))

(def dtfmt (tf/formatter "dd.MM.yyyy HH:mm:ss" #?(:clj clj-tz)))
(def dfmt (tf/formatter "dd.MM.yyyy" #?(:clj clj-tz)))
(def tfmt (tf/formatter "HH:mm:ss.SSS" #?(:clj clj-tz)))

(defn date-format [formatter date]
  (some->> date
           (tc/from-date)
           #?(:cljs t/to-default-time-zone)
           (tf/unparse formatter)))

(defn date-format-utc [formatter date]
  (some->> date (tc/from-date) (tf/unparse formatter)))

