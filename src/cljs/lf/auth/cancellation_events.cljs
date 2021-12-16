(ns lf.auth.cancellation-events
  (:require
   [re-frame.core :as rf]
   [lf.helper :as h]
   [taoensso.timbre :as log]
   [lf.ajax.ajax :as lajax]))


(rf/reg-event-fx
  ::success
  (fn [cofx event]
    (log/info "-- successful cancellation")
    (rf/dispatch [:login/logout])))


(rf/reg-event-fx
  ::failure
  (fn [cofx event]
    (let [db (:db cofx)]
      (log/error "-- account cancellation failed: " event)
      {:db (h/set-error db "Error: Account cancellation failed.")})))


(rf/reg-event-fx
  :account/cancel
  (fn [cofx _]
    (let [db (:db cofx)]
      {:db (assoc db :common/error nil)
       :http-xhrio (lajax/as-transit {:method :delete
                                      :uri (str  "/api/accounts/" (:login/id db))
                                      :headers {:authorization (str "Token " (:login/token db))}
                                      :on-success [::success]
                                      :on-failure [::failure]})})))
