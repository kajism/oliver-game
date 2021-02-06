(ns oliver-game.subs
  (:require
   [re-frame.core :as re-frame]
   [clojure.string :as str]
   [oliver-game.common :as common]
   [oliver-game.db :as db]))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::show?
 (fn [db [_ kw]]
   (get db kw)))

(re-frame/reg-sub
 ::chat
 (fn [db [_]]
   (:chat db)))

(re-frame/reg-sub
 ::get-in
 (fn [db [_ path]]
   (let [path (if (vector? path) path [path])]
     (get-in db path))))
