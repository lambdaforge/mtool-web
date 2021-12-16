(ns lf.auth.verify-account-events
  (:require
   [re-frame.core :as rf]
   [lf.helper :as h]
   [taoensso.timbre :as log]
   [lf.ajax.ajax :as lajax]))


(rf/reg-event-fx
  ::successful-verif
  (fn [_ _]
    (log/info "-- successful acc. verification")
    {:common/navigate-fx! [:account-confirmed]}))


(rf/reg-event-fx
  ::failed-verif
  (fn [cofx event]
    (let [db (:db cofx)]
      (log/error "-- Error: Acc. verif failed." event)
      {:db (h/set-error db "Error: Account verification failed.")})))

(rf/reg-event-fx
  :verify-account/verify
  (fn [cofx [_ id token]]
    {:http-xhrio (lajax/as-transit
                   {:method :put
                    :headers    {:authorization (str "Token " token)}
                    :uri (str  "/api/accounts/" id "/verify")
                    :on-success [::successful-verif]
                    :on-failure [::failed-verif]})}))
