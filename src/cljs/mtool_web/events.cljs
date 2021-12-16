(ns mtool-web.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [lf.helper :as h]))



;; Call-back from :login-events/successful-login

(rf/reg-fx
  :common/after-login!
  (fn [_]
    (rf/dispatch [:study/all])
    (rf/dispatch [:media/all])
    (rf/dispatch [:common/navigate! :home])))


;;dispatchers

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

;; Clears error and confirmation messages
(rf/reg-event-fx
  :common/init-view
  (fn [cofx _]
    {:db (-> (h/set-error (:db cofx) nil)
           (h/set-message nil))}))


(rf/reg-event-db
 :common/show-warning
 (fn [db [_ status]]
   (assoc db :common/show-warning status)))

;;subscriptions

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(rf/reg-sub
  :common/message
  (fn [db _]
    (:common/message db)))

(rf/reg-sub
 :common/show-warning
 (fn [db _]
   (:common/show-warning db false)))
