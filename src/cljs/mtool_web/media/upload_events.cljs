(ns mtool-web.media.upload-events
  (:require
    [re-frame.core :as rf]
    [lf.ajax.ajax :as lajax]
    [taoensso.timbre :as log]
    [lf.helper :as h]))


;;dispatchers

(defn upload-file
  [db [_ file]]
  (assoc db :upload/file file))

(rf/reg-event-db
  :upload/file
  upload-file)

(defn upload-note
  [db [_ note]]
  (assoc db :upload/note note))

(rf/reg-event-db
  :upload/note
  upload-note)


(defn upload-type
  [db [_ type]]
  (assoc db :upload/type type))

(rf/reg-event-db
  :upload/type
  upload-type)

(defn upload-name
  [db [_ name]]
  (assoc db :upload/name name))

(rf/reg-event-db
  :upload/name
  upload-name)

(rf/reg-fx
  ::media-all-fx!
  (fn [_]
    (rf/dispatch [:media/all])))

(defn after-upload!
  [db]
  (-> db
      (assoc :upload/file nil)
      (assoc :upload/type nil)
      (assoc :upload/note nil)
      (assoc :upload/name nil)))

(rf/reg-event-fx
  ::success
  (fn [cofx _]
    (let [db (:db cofx)]
      (log/info "----- Successfull upload")
      {:db             (-> db
                           (h/set-message (str "File successfully uploaded!"))
                           (assoc :upload/is-loading? false)
                           (assoc :common/show-warning false)
                           after-upload!)
       ::media-all-fx! nil})))

(rf/reg-event-fx
  ::failure
  (fn [cofx [_ {{:keys [message]} :response} :as event]]
    (let [db (:db cofx)]
      (log/error "----- ERROR: upload failed: " event)
      {:db (-> db
               (h/set-error (or message "Upload failed!"))
               (assoc :upload/is-loading? false)
               (assoc :common/show-warning true))})))

(rf/reg-event-fx
  :upload/upload!
  (fn [cofx _]
    (let [db (:db cofx)
          file (:upload/file db)
          a-type (:upload/type db)
          note (:upload/note db)
          name (:upload/name db)
          form-data (doto
                      (js/FormData.)
                      (.append "type" a-type)
                      (.append "note" note)
                      (.append "file" file)
                      (.append "name" name))
          token (:login/token db)]
      {:db (assoc db :upload/is-loading? true)
       :http-xhrio
           (lajax/as-transit {:method     :post
                              :uri        "/api/media"
                              :headers    {:authorization (str "Token " token)}
                              :body       form-data
                              :on-success [::success]
                              :on-failure [::failure]})})))

(rf/reg-sub
  :upload/file
  (fn [db _]
    (:upload/file db)))

(rf/reg-sub
  :upload/note
  (fn [db _]
    (:upload/note db)))

(rf/reg-sub
  :upload/name
  (fn [db _]
    (:upload/name db)))

(rf/reg-sub
  :upload/type
  (fn [db _]
    (:upload/type db)))

(rf/reg-sub
  :upload/is-loading?
  (fn [db _]
    (:upload/is-loading? db)))
