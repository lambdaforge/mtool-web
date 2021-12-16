(ns lf.auth.registration-events
  (:require
   [re-frame.core :as rf]
   [lf.helper :as h]
   [lf.ajax.ajax :as lajax]
   [taoensso.timbre :as log]))


(rf/reg-event-fx
  ::successful-signup
  (fn [_ _]
    (log/info "-- successful signup")
    {:common/navigate-fx! [:registration-confirmed]}))


(rf/reg-event-fx
  ::failed-signup
  (fn [cofx event]
    (let [db (:db cofx)]
      (log/error "-- signup failed")
      (if (= [:email] (-> event second :response :problems first :path))
        {:db (h/set-error db "Error: Email format is wrong.")}
        {:db (h/set-error db "Signup Error")}))))

(def terms-not-accepted "Error: Please accept the terms and conditions.")
(def pass-no-match "Error: Passwords do not match.")

(def pass-too-short "Error: Password is too short. Minimum 8 characters.")

(defn signup
  [cofx _]
  (let [{{:keys [registration/email registration/password
                 registration/confirmation-password
                 registration/terms-accepted?]} :db} cofx
        db (:db cofx)]
    (if (= password confirmation-password)
      (if (> (count password) 7)
        (if terms-accepted?
          {:db (-> (assoc (:db cofx) :common/error nil))
           :http-xhrio (lajax/as-transit {:method :post
                                          :uri "/api/accounts"
                                          :params {:email email
                                                   :password password}
                                          :on-success [::successful-signup]
                                          :on-failure [::failed-signup]})}
          {:db (h/set-error db terms-not-accepted)})
        {:db (h/set-error db pass-too-short)})
      {:db (h/set-error db pass-no-match)})))


(rf/reg-event-fx
  :registration/signup
  signup)

(rf/reg-event-db
  :registration/email
  (fn [db [_ email]]
    (-> (assoc db :common/error nil)
      (assoc :registration/email email))))

(rf/reg-event-db
  :registration/password
  (fn [db [_ password]]
    (-> (assoc db :common/error nil)
      (assoc :registration/password password))))

(rf/reg-event-db
  :registration/confirmation-password
  (fn [db [_ confirmation-password]]
    (-> (assoc db :common/error nil)
      (assoc :registration/confirmation-password confirmation-password))))

(rf/reg-event-db
  :registration/terms
  (fn [db [_ terms-accepted?]]
    (assoc db :registration/terms-accepted? terms-accepted?)))

(rf/reg-sub
  :registration/password
  (fn [db _]
    (-> db :registration/password)))

(rf/reg-sub
  :registration/email
  (fn [db _]
    (-> db :registration/email)))

(rf/reg-sub
  :registration/confirmation-password
  (fn [db _]
    (-> db :registration/confirmation-password)))


(rf/reg-sub
  :registration/terms-accepted?
  (fn [db _]
    (-> db :registration/terms-accepted?)))
