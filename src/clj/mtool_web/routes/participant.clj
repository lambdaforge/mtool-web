(ns mtool-web.routes.participant
  (:require
   [mtool-web.db.study :as db]
   [mtool-web.participant :refer [adjust-settings]]
   [mtool-web.validation]))

(defn participant-routes []
  ["/session-data"
   {:swagger {:tags ["Participant"]}}
   [""
    {:get {:summary "Get study settings adjusted to participant app"
           :parameters {:query {:study-id :s/id}}
           :responses {200 {:body {:message string?
                                   :session-settings map?}}}
           :handler (fn [{{{study-id :study-id} :query} :parameters}]
                      (let [settings (-> (db/get-study study-id) adjust-settings)]
                        {:status 200
                         :body {:message "Session setting have been fetched successfully!"
                                :session-settings settings}}))}
     :post {:summary "Upload results of a participant's session"
            :parameters {:body {:study-id :s/id
                                :session :s/session}}
            :responses {200 {:body {:message string?}}
                        404 {:body {:message string?}}}
            :handler (fn [{{{:keys [session study-id]} :body} :parameters}]
                       (if (db/study-exists? study-id)
                         (do (db/add-session-data study-id session)
                             {:status 200
                              :body {:message "Session data has been uploaded successfully!"}})
                         {:status 404
                          :body {:message "Study doesn't exist!"}}))}}]])
