(ns lf.auth.password-reset-events
  (:require
   [re-frame.core :as rf]
   [lf.helper :as h]
   [lf.ajax.ajax :as lajax]
   [taoensso.timbre :as log]))

(rf/reg-event-db
  :pass-reset/user-id
  (fn [db [_ id]]
    (assoc db :pass-reset/user-id id)))


(rf/reg-event-db
  :pass-reset/token
  (fn [db [_ token]]
    (assoc db :pass-reset/token token)))


(rf/reg-event-fx
  ::successful-reset
  (fn [cofx _]
    (log/info "-- successful reset")
    {:db (h/set-message (:db cofx) "Successful password reset!")
     :common/navigate-fx! [:login]}))


(rf/reg-event-fx
  ::failed-reset
  (fn [cofx event]
    (let [db (:db cofx)]
      (log/error (str  "-- ERROR: pass reset failed. " event))
      {:db (h/set-error db "Password reset Error")})))



(rf/reg-event-fx
  :pass-reset/reset
  (fn [cofx _]
    (let [{{:keys [pass-reset/password
                   pass-reset/confirmation-password]} :db} cofx
          db (:db cofx)
          id (:pass-reset/user-id db)
          token (:pass-reset/token db)]
      (if (= password confirmation-password)
        {:db (-> (assoc (:db cofx) :common/error nil))
         :http-xhrio (lajax/as-transit
                       {:method :put
                        :headers {:authorization (str "Token " token)}
                        :uri (str "/api/accounts/" id "/reset-password" )
                        :params {:password password}
                        :on-success [::successful-reset]
                        :on-failure [::failed-reset]})}
        {:db (h/set-error db "Error: Passwords do not match")}))))




(rf/reg-event-db
  :pass-reset/password
  (fn [db [_ password]]
    (-> (assoc db :common/error nil)
      (assoc :pass-reset/password password))))

(rf/reg-event-db
  :pass-reset/confirmation-password
  (fn [db [_ confirmation-password]]
    (-> (assoc db :common/error nil)
      (assoc :pass-reset/confirmation-password confirmation-password))))


(rf/reg-sub
  :pass-reset/password
  (fn [db _]
    (-> db :pass-reset/password)))

(rf/reg-sub
  :pass-reset/confirmation-password
  (fn [db _]
    (-> db :pass-reset/confirmation-password)))
