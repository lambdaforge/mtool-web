(ns mtool-web.study.study-overview-events
  (:require
   [re-frame.core :as rf]
   [lf.ajax.ajax :as lajax]
   [taoensso.timbre :as log]
   [lf.helper :as h]
   [ajax.core :as ajax]
   [luminus-transit.time :as time]
   [mtool-web.study.study-model :as sm]))



(rf/reg-event-fx
 ::get-all-success
 (fn [cofx event]
   (let [db (:db cofx)
         studies (-> event second :studies)]
     {:db (assoc db :user/studies studies)})))


(defn get-studies-failure
  [cofx event]
  (let [db (:db cofx)]
    (log/error "----- ERROR: get-all studies failed: " event)
    {:db (-> (h/set-error
              db
              (str "Sorry. Studies could not be retrieved."
                   (when (= (-> event second :response :error) "Unauthorized")
                     " Please login again.")))
             (assoc :user/studies nil))}))

(rf/reg-event-fx
 ::get-all-failure
 get-studies-failure)



(rf/reg-event-fx
 :study/all
 (fn [cofx _]
   (let [db (:db cofx)
         token (:login/token db)]
     {:http-xhrio
      (lajax/as-transit {:method :get
                         :uri "/api/studies"
                         :headers {:authorization (str "Token " token)}
                         :on-success [::get-all-success]
                         :on-failure [::get-all-failure]})})))



(rf/reg-sub
 :user/studies
 (fn [db _]
   (:user/studies db)))



;; Activation

(rf/reg-event-fx
 ::activation-success
 (fn [cofx _]
   (let [db (:db cofx)]
     (rf/dispatch [:study/all])
     {:db  (-> (h/set-message db "Successfully activated study!"))})))


(rf/reg-event-fx
 ::activation-failure
 (fn [cofx event]
   (let [db (:db cofx)]
     (log/error "----- ERROR: study activation failed: " event)
     {:db  (h/set-error db "Sorry, study could not be activated.")})))


(rf/reg-event-fx
 :study/activate!
 (fn [cofx [_ id]]
   (let [db (:db cofx)
         token (:login/token db)]
     {:http-xhrio
      (lajax/as-transit {:method :put
                         :uri (str "/api/studies/" id "/activate")
                         :headers {:authorization (str "Token " token)}
                         :on-success [::activation-success]
                         :on-failure [::activation-failure]})})))


(rf/reg-event-db
 :study-overview/show-activation-modal
 (fn [db [_ id]]
   (h/toggle-component "activation-modal") ;; opens the modal
   (let [study (sm/get-study db id)]
     (assoc db :study/current study))))

;; Deletion

(rf/reg-event-db
 :study-overview/show-deletion-modal
 (fn [db [_ id]]
   (h/toggle-component "deletion-modal")
   (let [study (sm/get-study db id)]
     (assoc db :study/deletion study))))

(rf/reg-event-fx
 ::study-deletion-success
 (fn [{:keys [db]} _]
   (h/toggle-component "deletion-modal")
   (rf/dispatch [:study/all])
   {:db  (h/set-message db "Study successfully deleted!")}))


(rf/reg-event-fx
 ::study-deletion-failure
 (fn [cofx [_ {{:keys [message]} :response} :as event]]
   (let [db (:db cofx)]
     (log/error "----- ERROR: deletion failed: " event)
     (h/toggle-component "deletion-modal")
     (rf/dispatch [:study/all])
     {:db (-> db
              (h/set-error (or message "Deletion failed!"))
              (assoc :upload/is-loading? false)
              (assoc :common/show-warning true))})))

(rf/reg-event-fx
 :study/delete!
 (fn [cofx [_ id]]
   (let [db (:db cofx)
         token (:login/token db)]
     {:http-xhrio
      (lajax/as-transit {:method :delete
                         :uri (str "/api/studies/" id)
                         :headers {:authorization (str "Token " token)}
                         :on-success [::study-deletion-success]
                         :on-failure [::study-deletion-failure]})})))

(rf/reg-sub
 :study/deletion
 (fn [db _]
   (:study/deletion db)))

;; Deactivation

(rf/reg-event-fx
 ::deactivation-success
 (fn [cofx _]
   (let [db (:db cofx)
         id (-> db :study/current :id)]
     (rf/dispatch [:study/all])
     {:db  (h/set-message db "Successfully deactivated study!")})))


(rf/reg-event-fx
 ::deactivation-failure
 (fn [cofx event]
   (let [db (:db cofx)]
     (log/error "----- ERROR: study deactivation failed: " event)
     {:db  (h/set-error db "Sorry, study could not be deactivated.")})))


(rf/reg-event-fx
 :study/deactivate!
 (fn [cofx [_ id]]
   (let [db (:db cofx)
         token (:login/token db)]
     {:http-xhrio
      (lajax/as-transit {:method :put
                         :uri (str "/api/studies/" id "/deactivate")
                         :headers {:authorization (str "Token " token)}
                         :on-success [::deactivation-success]
                         :on-failure [::deactivation-failure]})})))

;; download

(rf/reg-sub
 :download/csv-link
 (fn [db _]
   (:download/csv-link db)))

(rf/reg-event-fx
 ::download-success
 (fn [cofx event]
   (let [db (:db cofx)
         csv-link (-> event second :link)]
     (h/toggle-component "csv-link-modal")
     {:db (assoc db :download/csv-link csv-link)})))


(rf/reg-event-fx
 ::download-failure
 (fn [cofx event]
   (let [db (:db cofx)]
     (log/error "----- ERROR: study download failed: " event)
     {:db  (h/set-error db "Sorry, study could not be downloaded.")})))

(rf/reg-event-fx
 :study/download!
 (fn [{:keys [db]} [_ id]]
   (let [token (:login/token db)]
     {:http-xhrio
      (lajax/as-transit {:method :get
                         :uri (str "/api/studies/" id "/session-data")
                         :headers {:authorization (str "Token " token)}
                         :on-success [::download-success]
                         :on-failure [::download-failure]})})))


;; Study link


(rf/reg-event-db
 :study-overview/show-link-modal
 (fn [db [_ id]]
   (h/toggle-component "link-modal") ;; opens the modal
   (let [study (sm/get-study db id)]
     (assoc db :study/current study))))

(rf/reg-sub
 :link/copied?
 (fn [db _]
   (:link/copied? db)))

(rf/reg-event-db
 :link/copied!
 (fn [db [_ link]]
   (-> (.writeText (.-clipboard (.-navigator js/window)) link)
       (.then #(.log js/console "Link copied!"))
       (.catch #(.log js/console "Link copy failed!")))
   (assoc db :link/copied? (some? link))))
