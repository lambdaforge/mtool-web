(ns mtool-web.routes.media
  (:require
    [reitit.ring.middleware.multipart :as multipart]
    [lf.middleware.auth :as auth]
    [mtool-web.config :as c :refer [env]]
    [mtool-web.db.media :as db]
    [mtool-web.validation]
    [clojure.java.io :as io]))

(defn media-routes []
  ["/media"
   {:swagger {:tags ["Media"]}
    :middleware [auth/token-auth-middleware auth/auth-middleware]}
   [""
    {:post {:summary    "Upload a media file"
            :parameters {:multipart        {:file multipart/temp-file-part}
                         :multipart-params {:type :s/media-type :note string? :name string?}}
            :responses  {200 {:body {:message  string?
                                     :media-id :s/id
                                     :link     string?}}
                         400 {:body {:message string?}}
                         403 {:body {:message string?}}}
            :handler    (fn [{{{{:keys [filename tempfile size]} :file} :multipart} :parameters
                              m-params                                              :multipart-params
                              {user-id :id}                                         :identity}]
                          (let [path (str c/user-media-dir "/" user-id "/" filename)
                                name (get m-params "name")
                                type (keyword (get m-params "type"))]
                            (if (db/media-with-name-type-exists? user-id name type)
                              {:status 400
                               :body   {:message "User media with given name and type has already been uploaded!"}}
                              (if (.exists (io/file path))
                                {:status 400
                                 :body   {:message "User media with given name has already been uploaded!"}}
                                (if (> size (* (:max-file-size env) 1024 1024))
                                  {:status 403
                                   :body   {:message (str "Media file is too large. Allowed are " (:max-file-size env) " Mb.")}}
                                  (do (io/make-parents path)
                                      (io/copy tempfile (io/file path))
                                      (let [link (str (:base-url env) "/" user-id "/" filename)
                                            raw-note (get m-params "note")
                                            note (if (= raw-note "null") "" raw-note) ;; Should not be necessary but is probable bug of multipart params
                                            mid (db/add-media user-id
                                                              {:filename filename
                                                               :note     note
                                                               :type     type
                                                               :name     name
                                                               :link     link
                                                               :path     path})]
                                        {:status 200
                                         :body   {:message  "Media file has been stored successfully!"
                                                  :media-id mid
                                                  :link     link}})))))))}

     :get  {:summary   "Get a list of a researcher's media files"
            :responses {200 {:body {:message string?
                                    :media   :s/media-list}}}
            :handler   (fn [{{user-id :id} :identity}]
                         (let [media (vec (db/get-user-media user-id))]
                           {:status 200
                            :body   {:message "User media has been fetched!"
                                     :media   media}}))}}]

   ["/:id"
    {:delete {:summary "Delete a researcher's media file"
              :parameters {:path {:id :s/id}}
              :responses {200 {:body {:message string?}}
                          400 {:body {:message string?}}}
              :handler (fn [{{user-id :id} :identity
                             {{media-id :id} :path} :parameters}]
                         (if (db/owns-media? user-id media-id)
                           (if-let [usage (seq (db/media-usage media-id))]
                             {:status 400
                              :body {:message (str "Media file cannot be deleted because it is used in studies as: " usage)}}
                             (let [path (db/get-media-location media-id)]
                               (io/delete-file path)
                               (db/delete-media media-id)
                               {:status 200
                                :body {:message "File has been deleted!"}}))
                           {:status 400
                            :body {:message "User doesn't own a file with given ID!"}}))}}]])
