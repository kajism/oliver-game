(ns oliver-game.events
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.core :as re-frame]
            [re-pressed.core :as rp]
            [oliver-game.common :as common]
            [oliver-game.db :as db]
            [oliver-game.websocket :as websocket]))

(re-frame/reg-event-fx
 ::initialize
 (fn [_ _]
   (let [trckn (:trckn db/default-db)]
     {:db db/default-db
      :ws-call {:type :get-init-info}
      :dispatch [::select-track trckn]})))

(re-frame/reg-event-fx
 ::check-init-info
 (fn [{:keys [db]} _]
   (when-not (:init-info db)
     {:ws-call {:type :get-init-info}})))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-fx
 ::ws-error?
 (fn [{:keys [db]} [_ state]]
   (cond-> {:db (assoc db :ws-error? state)}
     (and (true? state) (not= (:ws-error? db) state))
     (assoc :ws-call {:type :get-init-info}))))

(re-frame/reg-event-fx
 ::ws-received
 (fn [{:keys [db]} [_ {:keys [type] :as msg}]]
   (let [[in-path value]
         (case type
           :init-info [[:init-info] (:result msg)]
           :app-domains [[:app-domains] (:result msg)]
           nil)]
     (cond-> {:db (cond-> (assoc db :ws-error? false)
                    (fn? value)
                    (update-in in-path value)
                    (not (fn? value))
                    (assoc-in in-path value))}
       (= type :access-denied)
       (assoc :location.pathname "/login")))))

(defn set-windows-location-pathname-effect [pathname]
  (set! js/window.location.pathname pathname))

(re-frame/reg-fx :location.pathname set-windows-location-pathname-effect)
