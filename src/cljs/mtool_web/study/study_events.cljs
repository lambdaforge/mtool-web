(ns mtool-web.study.study-events
  (:require [clojure.spec.alpha :as s]
            [lf.ajax.ajax :as lajax]
            [mtool-web.study.mapping-events :as me]
            [mtool-web.study.study-model :as sm]
            [mtool-web.events]
            [mtool-web.validation]
            [mtool-web.study.study-validation :as v]
            [lf.helper :as h]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

(rf/reg-event-db
 :study.current/reset
 (fn [db _]))


(rf/reg-sub
 :study/current
 (fn [db _]
   (:study/current db)))

(rf/reg-sub
 :study/id
 (fn [db _]
   (-> db :study/current :id)))


(rf/reg-sub
 :study/link
 (fn [db _]
   (-> db :study/current :link)))

(rf/reg-event-db
 :study/name
 (fn [db [_ name]]
   (assoc-in db [:study/current :topic] name)))


(rf/reg-sub
 :study/name
 (fn [db _]
   (-> db :study/current :topic)))


(rf/reg-event-db
 :study/consentText
 (fn [db [_ consent-text]]
   (assoc-in db [:study/current :consentText] consent-text)))

(rf/reg-sub
 :study/consentText
 (fn [db _]
   (-> db :study/current :consentText)))

(rf/reg-event-db
 :study/consentLink
 (fn [db [_ consent-link]]
   (assoc-in db [:study/current :consentLink] consent-link)))

(rf/reg-sub
 :study/consentLink
 (fn [db _]
   (-> db :study/current :consentLink)))

(rf/reg-event-db
 :study/survey
 (fn [db [_ survey]]
   (assoc-in db [:study/current :survey] survey)))

(rf/reg-sub
 :study/survey
 (fn [db _]
   (-> db :study/current :survey)))

(rf/reg-event-db
 :study/desc
 (fn [db [_ desc]]
   (assoc-in db [:study/current :description] desc)))

(rf/reg-sub
 :study/desc
 (fn [db _]
   (-> db :study/current :description)))


(rf/reg-event-db
 :study/thankyou-msg
 (fn [db [_ msg]]
   (assoc-in db (sm/thanks-msg-path) msg)))


(rf/reg-sub
 :study/thankyou-msg
 (fn [db _]
   (get-in db (sm/thanks-msg-path))))


(rf/reg-event-db
 :study/welcome-msg
 (fn [db [_ msg]]
   (assoc-in db (sm/welcome-msg-path) msg)))


(rf/reg-sub
 :study/welcome-msg
 (fn [db _]
   (get-in db (sm/welcome-msg-path))))


(rf/reg-event-db
 :study/mapping-1-checkbox
 (fn [db [_ _]]
   (me/toggle-mapping-presence db [nil 1])))


(rf/reg-sub
 :study/mapping-1-checkbox
 (fn [db _]
   (some? (me/mapping-instance db 1))))


(rf/reg-event-db
 :study/mapping-2-checkbox
 (fn [db [_ _]]
   (me/toggle-mapping-presence db [nil 2])))


(rf/reg-sub
 :study/mapping-2-checkbox
 (fn [db _]
   (some? (me/mapping-instance db 2))))


(rf/reg-event-db
 :study/neutral-checkbox
 (fn [db [_ neutral-checkbox]]
   (-> db
       (assoc-in [:study/current :positiveArrows?] true)
       (assoc-in [:study/current :negativeArrows?] false))))


(rf/reg-sub
 :study/neutral-checkbox
 (fn [db _]
   (not
    (and
     (-> db :study/current :positiveArrows?)
     (-> db :study/current :negativeArrows?)))))

(rf/reg-event-db
 :study/signed-checkbox
 (fn [db [_ signed-checkbox]]
   (-> db
       (assoc-in [:study/current :positiveArrows?] true)
       (assoc-in [:study/current :negativeArrows?] signed-checkbox))))


(rf/reg-sub
 :study/signed-checkbox
 (fn [db _]
   (and
    (-> db :study/current :positiveArrows?)
    (-> db :study/current :negativeArrows?))))


(rf/reg-event-db
 :study/double-headed-checkbox
 (fn [db [_ double-headed-checkbox]]
   (assoc-in db [:study/current :doubleHeadedArrows?] double-headed-checkbox)))


(rf/reg-sub
 :study/double-headed-checkbox
 (fn [db _]
   (-> db :study/current :doubleHeadedArrows?)))

(rf/reg-event-fx
 :study/new
 (fn [cofx _]
   {:media/load-all! nil
    :db (-> (assoc (:db cofx) :study/current sm/new-study)
          (assoc :study-screen/mode :creation)
          (assoc :study-screen/title "Study creation"))}))

(rf/reg-event-fx ::create-success
 (fn [cofx event]
   (let [db (:db cofx)
         activate-study? (:study/activation-checkbox db)
         id (-> event second :study-id)]
     (rf/dispatch [:study/all])
     ;; when asked to activate right after creation
     (when (or (nil? activate-study?) activate-study?)
       (rf/dispatch [:study/activate! id]))
     {:db (-> (h/set-message db "Study created successfully.")
              (assoc :study/activation-checkbox true)
              (assoc :common/show-warning false))
      :common/navigate-fx! [:study-overview]})))

(rf/reg-event-fx
 ::create-failure
 (fn [cofx event]
   (let [db (:db cofx)]
     (log/error "----- study creation failed: " event)
     {:db (-> db
              (h/set-error "Error: Could not create study. Make sure fields are correctly set.")
              (assoc :common/show-warning true))})))

(rf/reg-event-fx
 :study/invalid-input
 (fn [{:keys [db]} [_ msg]]
   {:db (h/set-error db msg)}))

(rf/reg-event-fx
 :study/create!
 (fn [cofx _]
   (let [db (:db cofx)
         token (:login/token db)
         study (reduce (fn [m [k v]] (if (nil? v)
                                       m
                                       (if (seqable? v)
                                         (if (empty? v)
                                           m
                                           (assoc m k v))
                                         (assoc m k v)))) {} (:study/current db))]
     (rf/dispatch [:common/init-view]) ;; clear error and status msg
     (.scrollTo js/window 0 0) ;; Move to top of page
     (if-let [msg (v/check-study study)]
       (rf/dispatch [:study/invalid-input msg])
       {:http-xhrio (lajax/as-transit
                     {:method :post
                      :uri (str "/api/studies")
                      :headers {:authorization (str "Token " token)}
                      :params {:study study}
                      :on-success [::create-success]
                      :on-failure [::create-failure]})}))))


;; videos-and-audio

(rf/reg-event-db
 :video-audio/dropdown-modified
 (fn [db [_ db-path new-val]]
   (assoc-in db db-path new-val)))

(rf/reg-sub
 :video-audio/dropdown-value
 (fn [db [_ db-path]]
   (get-in db db-path)))


;; bar-chart

(defn bar-chart-instance
  [db]
  (-> db :study/current :barChart))

(rf/reg-sub
 :bar-chart/instance
 (fn [db _]
   (bar-chart-instance db)))


(defn toggle-bar-chart-presence
  [db]
  (let [present? (bar-chart-instance db)]
    (if present?
      (assoc db :study/current (dissoc (:study/current db) :barChart))
      (assoc-in db [:study/current :barChart] (sm/new-bar-chart)))))


(rf/reg-event-db
 :study/bar-chart-checkbox
 (fn [db _]
   (toggle-bar-chart-presence db)))


(rf/reg-sub
 :study/bar-chart-checkbox
 (fn [db _]
   (some? (bar-chart-instance db))))


;; activation
;;
(rf/reg-event-db
 :study/activation-checkbox
 (fn [db [_ checked]]
   (assoc db :study/activation-checkbox checked)))


(rf/reg-sub
 :study/activation-checkbox
 (fn [db _]
   (:study/activation-checkbox db true)))

;; active-inactive

(rf/reg-sub
  :study/active?
  (fn [db _]
   (-> db :study/current :active?)))

;; the title

(rf/reg-event-db
 :study-screen/title
 (fn [db [_ title]]
   (assoc db :study-screen/title title)))

(rf/reg-sub
  :study-screen/title
  (fn [db _]
   (:study-screen/title db)))


;; screen-mode

;; mode is either :creation or :edit
(rf/reg-event-db
 :study-screen/mode
 (fn [db [_ mode]]
   (assoc db :study-screen/mode mode)))

(rf/reg-sub
  :study-screen/mode
  (fn [db _]
   (:study-screen/mode db)))
