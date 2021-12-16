(ns lf.auth.login-events
  (:require
    [re-frame.core :as rf]
    [lf.ajax.ajax :as lajax]
    [taoensso.timbre :as log]
    [goog.crypt.base64 :as base64]))

;; Assumes the following entities to be defined by user of this lib.
;; - Effects: :common/after-login! :common/navigate-fx!
;; - Db entries: :common/error


;; Not using reg-event-db as suggested by https://github.com/day8/re-frame-http-fx,
;; as in the end we get the result returned by the server in `(second event)`
(rf/reg-event-fx
  ::successful-login
  (fn [cofx event]
    (log/info "-- successful login")
    (let [token (-> event second :user :token)
          id (-> event second :user :id)
          db (:db cofx)]
      {:db                  (-> db
                                (assoc :login/token token)
                                (assoc :login/id id)
                                (assoc :login/date (js/Date.)))
       :common/after-login! nil})))



(rf/reg-event-fx
  ::failed-login
  (fn [cofx [_ {status :status {:keys [message]} :response} :as event]]
    (log/error "ERROR: Login Failed: " event)
    {:db                  (assoc (:db cofx)
                            :common/error (or message "That email and password combination is incorrect.")
                            :common/errorStatus (case status
                                                  400 :not-verified
                                                  401 :login-failed
                                                  :unknown))
     :common/navigate-fx! [:login]}))

(rf/reg-event-fx
  ::successful-verification-resent
  (fn [cofx [_ {{:keys [message]} :response} :as event]]
    (log/info "-- successful verification resent")
    {:db                  (assoc (:db cofx)
                            :common/error message
                            :common/errorStatus nil)
     :common/navigate-fx! [:verification-resent-confirmed]}))

(rf/reg-event-fx
  ::failed-verification-resent
  (fn [cofx [_ {{:keys [message]} :response} :as event]]
    (log/error "ERROR: Verification Failed: " event)
    {:db                  (assoc (:db cofx)
                            :common/error (or message "Account could not be verified.")
                            :common/errorStatus nil)
     :common/navigate-fx! [:login]}))

(rf/reg-event-fx
  :login
  (fn [{{:keys [:login/email :login/password]} :db} _]
    {:http-xhrio (lajax/as-transit {:method     :get
                                    :uri        "/api/login"
                                    :headers    {:Authorization (str "Basic " (base64/encodeString (str email ":" password)))}
                                    :on-success [::successful-login]
                                    :on-failure [::failed-login]})}))

(rf/reg-event-fx
  :resent-verification
  (fn [{{:keys [:login/email :login/password]} :db} _]
    {:http-xhrio (lajax/as-transit {:method     :get
                                    :uri        "/api/resent-verification"
                                    :headers    {:Authorization (str "Basic " (base64/encodeString (str email ":" password)))}
                                    :on-success [::successful-verification-resent]
                                    :on-failure [::failed-verification-resent]})}))

(rf/reg-event-db
  :login/email
  (fn [db [_ email]]
    (assoc db :login/email email)))

(rf/reg-event-db
  :login/password
  (fn [db [_ password]]
    (assoc db :login/password password)))

(defn logout
  [db _]
  (-> db
      (dissoc :login/token)
      (dissoc :login/password)
      (dissoc :login/email)
      (dissoc :common/error)))


(rf/reg-event-db
  :logout
  logout)


(rf/reg-sub
  :login/token
  (fn [db _]
    (-> db :login/token)))

(rf/reg-sub
  :login/password
  (fn [db _]
    (-> db :login/password)))

(rf/reg-sub
  :login/email
  (fn [db _]
    (-> db :login/email)))
(rf/reg-sub
  :common/errorStatus
  (fn [db _]
    (-> db :common/errorStatus)))

(rf/reg-sub
 :login/date
 (fn [db _]
   (-> db :login/date)))
