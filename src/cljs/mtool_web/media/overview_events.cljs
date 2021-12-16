(ns mtool-web.media.overview-events
  (:require
   [re-frame.core :as rf]
   [lf.ajax.ajax :as lajax]
   [taoensso.timbre :as log]
   [lf.helper :as h]))


;;dispatchers

(rf/reg-fx
  ::after-delete!
  (fn [_]
    (rf/dispatch [:media/all])))

(rf/reg-event-fx
  ::delete-success
  (fn [cofx _]
    (let [db (:db cofx)]
      (log/info "----- Successfull delete")
      {:db (h/set-message db " Successfully deleted file!")
       ::after-delete! nil})))

(rf/reg-event-fx
  ::delete-failure
  (fn [cofx event]
    (let [db (:db cofx)]
      (log/error "----- ERROR: Delete failed: " event)
      {:db (h/set-error db "Could not delete file! Currently in use in study.")
       ::after-delete! nil})))



(rf/reg-event-fx
 :media/delete!
 (fn [cofx [_ id]]
   (let [db (:db cofx)
         token (:login/token db)]
     {:http-xhrio
      (lajax/as-transit {:method :delete
                         :uri (str "/api/media/" id)
                         :headers {:authorization (str "Token " token)}
                         :on-success [::delete-success]
                         :on-failure [::delete-failure]})})))


(rf/reg-event-fx
 ::get-all-success
 (fn [cofx event]
   (let [db (:db cofx)
         media (-> event second :media)]
     {:db (assoc db :user/media media)
      ;; !!! DON'T navigate back to [:media-overview] because this code is all called
      ;; by e.g. 'new study'
      })))


(defn get-media-failure
  [cofx event]
  (let [db (:db cofx)]
    (log/error "----- ERROR: get-all media failed: " event)
    {:db (-> (h/set-error db (str "Sorry. Media could not be retrieved."
                                  (when (= (-> event second :response :error) "Unauthorized")
                                    " Please login again.")))
             (assoc :user/media nil)
             )
      ;; !!! DON'T navigate back to [:media-overview] because this code is all called
      ;; by e.g. 'new study'
     }))

(rf/reg-event-fx
  ::get-all-failure
  get-media-failure)


(rf/reg-fx
  :media/load-all!
  (fn [_]
    (rf/dispatch [:media/all])))

(rf/reg-event-fx
 :media/all
 (fn [cofx _]
   (let [db (:db cofx)
         token (:login/token db)
         _ (log/info "------- :medial/all")]
     {:http-xhrio
      (lajax/as-transit {:method :get
                         :uri "/api/media" ;; TODO: set the correct call
                         :headers {:authorization (str "Token " token)}
                         :on-success [::get-all-success]
                         :on-failure [::get-all-failure]})})))
(rf/reg-event-db
 :media/preview
 (fn [db [_ {:keys [type] :as media}]]
   (let [el (.getElementById js/document (case (keyword type)
                                           :audio "audio-file"
                                           :video "video-file"
                                           "any-file"))]
     (when el (.pause el)))
   (assoc db :media/preview media
          :media/playing? false)))

(rf/reg-sub
 :media/preview
 (fn [db]
   (-> db :media/preview)))

(rf/reg-event-db
 :media/play!
 (fn [db [_ type]]
   (let [el (.getElementById js/document (case type
                                           :audio "audio-file"
                                           :video "video-file"))]
     (set! (.-onended el) (fn [e] (rf/dispatch [:media/pause! type])))
     (.load el)
     (.play el)
     (assoc db :media/playing? true))))

(rf/reg-event-db
 :media/pause!
 (fn [db [_ type]]
   (let [el (.getElementById js/document (case type
                                           :audio "audio-file"
                                           :video "video-file"))]
     (.pause el)
     (assoc db :media/playing? false))))

(rf/reg-sub
 :media/playing?
 (fn [db]
   (-> db :media/playing?)))

(rf/reg-sub
 :user/media
 (fn [db _]
   (:user/media db)))

(rf/reg-sub
 :user/images
 (fn [_]
   (rf/subscribe [:user/media]))
 (fn [media]
   (filter #(= :image (:type %)) media)))

(rf/reg-sub
 :user/audio
 (fn [_]
   (rf/subscribe [:user/media]))
 (fn [media]
   (filter #(= :audio (:type %)) media)))

(rf/reg-sub
 :user/videos
 (fn [_]
   (rf/subscribe [:user/media]))
 (fn [media]
   (filter #(= :video (:type %)) media)))
