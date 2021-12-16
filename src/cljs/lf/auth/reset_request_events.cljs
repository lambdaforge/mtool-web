(ns lf.auth.reset-request-events
  (:require
   [re-frame.core :as rf]
   [lf.ajax.ajax :as lajax]
   [lf.helper :as h]
   [clojure.spec.alpha :as s]
   [taoensso.timbre :as log]))


(s/def ::email #(re-matches #".+@.+\..+" %))


;; TODO: Needs testing
(rf/reg-event-fx
  ::successful-request
  (fn [cofx _]
    {:db (h/set-message (:db cofx) "A mail has been sent to reset your password.")}))

(rf/reg-event-fx
  ::failed-request
  (fn [cofx event]
    (log/error "--- ERROR: Reset request failed: " event)
    {:db (h/set-error (:db cofx) "Error: Cannot reset password")}))


(rf/reg-event-fx
  :reset-req/reset-required
  (fn [cofx _]
    (let [{{:keys [:reset-req/email]} :db} cofx
          db (:db cofx)]
      (if (and (not (empty? email))
            (s/valid? ::email email))
        {:db (h/set-error db nil)
         :http-xhrio (lajax/as-transit {:method :get
                                        :uri "/api/reset-request"
                                        :params {:email email}
                                        :on-success [::successful-request]
                                        :on-failure [::failed-request]})}
        {:db (h/set-error db "Error: Email format is wrong.")}))))


(rf/reg-event-db
  :reset-req/email
  (fn [db [_ email]]
    (-> (h/set-error db nil)
      (assoc :reset-req/email email))))

(rf/reg-sub
  :reset-req/email
  (fn [db _]
    (-> db :reset-req/email)))
