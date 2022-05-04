(ns oliver-game.events
  (:require [re-frame.core :as re-frame]
            [oliver-game.db :as db]))

(re-frame/reg-event-fx
 ::initialize
 (fn [_ _]
   (let [trckn (:trckn db/default-db)]
     {:db db/default-db
      :ws-call {:type :ws-client/get-init-info}})))

(re-frame/reg-event-fx
 ::check-init-info
 (fn [{:keys [db]} _]
   (when-not (:init-info db)
     {:ws-call {:type :ws-client/get-init-info}})))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
 ::toggle
 (fn [db [_ kw]]
   (update db kw not)))

(re-frame/reg-event-fx
 ::send
 (fn [_ [_ msg]]
   {:ws-call {:type :chat
              :msg msg}}))

(re-frame/reg-event-db
 ::hide-all
 (fn [db [_ kw]]
   (assoc db
          :show-hamburgers? false
          :show-new-trades? false
          :show-milk-shakes? false
          :show-hot-dogs? false
          :show-ice-creams? false)))

(re-frame/reg-event-db
 ::set-in
 (fn [db [_ path v]]
   (let [path (if (vector? path) path [path])]
     (assoc-in db path v))))

(def users {"Olda" {:pwd "Ferdafrog"
                    :title "Owner"
                    :color "green"}
            "kajism" {:pwd "ahoj"
                      :title "Programátor"
                      :color "green"}
            "DobryVecer" {:pwd "testujem"
                          :title "Tester"
                          :color "yellow"}
            "Domácí nudle" {:pwd "ŽabákFerda"
                            :title "Admin"
                            :color "red"}
            "yt/Filip" {:pwd "bry*F"
                        :title "Admin"
                        :color "orange"}
            "MATYWOSN" {:pwd "MATYWOSN"
                        :title "Admin"
                        :color "red"}})

(re-frame/reg-event-db
 ::login
 (fn [db [_]]
   (let [nickname (get-in db [:login :nickname])]
     (if (= (get-in db [:login :pwd]) (get-in users [nickname :pwd]))
       (assoc db :user nickname
              :login nil)
       (-> db
           (assoc :login {:error "Neplatné uživatelské jméno nebo heslo"
                          :nickname ""
                          :pwd ""}))))))

(re-frame/reg-event-fx
 ::ws-error?
 (fn [{:keys [db]} [_ state]]
   (cond-> {:db (assoc db :ws-error? state)}
     (and (true? state) (not= (:ws-error? db) state))
     (assoc :ws-call {:type :ws-client/get-init-info}))))

(re-frame/reg-event-fx
 ::ws-received
 (fn [{:keys [db]} [_ {:keys [type] :as msg}]]
   (let [[in-path value]
         (case type
           :init-info [[:init-info] (:result msg)]
           :app-domains [[:app-domains] (:result msg)]
           :chat [[:chat] #(str % "\n" (:msg msg))]
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
