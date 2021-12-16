(ns mtool-web.routes.study
  (:require
   [lf.middleware.auth :as auth]
   [mtool-web.db.study :as db]
   [mtool-web.participant :refer [get-session-results-file]]
   [mtool-web.validation]))

(defn study-routes []
  ["/studies"
   {:swagger {:tags ["Studies"]}
    :middleware [auth/token-auth-middleware auth/auth-middleware]}
   [""
    {:post {:summary "Add a new study"
            :parameters {:body {:study :s/study-settings}}
            :responses {200 {:body {:message string?
                                    :study-id :s/id}}}
            :handler (fn [{{user-id :id} :identity
                           {{study-settings :study} :body} :parameters}]
                       (let [study-id (db/add-study user-id study-settings)]
                         {:status 200
                          :body {:message "Study has been added successfully!"
                                 :study-id study-id}}))}

     :get {:summary "Get all studies for a researcher"
           :responses {200 {:body {:message string?
                                   :studies :s/studies}}}
           :handler (fn [{{user-id :id} :identity}]
                      (let [studies (db/get-user-studies user-id)]
                        {:status 200
                         :body {:message "User studies have been fetched!"
                                :studies studies}}))}}]
   ["/:id"
    [""
     {:get {:summary "Get a particular study"
            :parameters {:path {:id :s/id}}
            :responses {200 {:body {:message string?
                                    :study :s/study-settings}}}
            :handler (fn [{{{study-id :id} :path} :parameters}]
                       (let [settings (db/get-study study-id)]
                         {:status 200
                          :body {:message "Study has been fetched!"
                                 :study settings}}))}

      :post {:summary "Overwrite study"
             :parameters {:path {:id :s/id}
                          :body {:study :s/study-settings}}
             :responses {200 {:body {:message string?
                                     :study-id :s/id}}
                         400 {:body {:message string?}}}
             :handler (fn [{{user-id :id} :identity
                            {{study-id :id} :path
                             {study-settings :study} :body} :parameters}]
                        (if (db/owns-study? user-id study-id)
                          (let [active? (db/study-active? study-id)
                                session-ids (db/get-session-ids study-id)]
                            (db/delete-study study-id)
                            (db/add-study user-id study-settings study-id active?)
                            (db/import-sessions study-id session-ids)
                            {:status 200
                             :body {:message "Study has been updated successfully!"
                                    :study-id study-id}})
                          {:status 400
                           :message "Researcher does not own a study with given ID!"}))}

      :delete {:summary "Delete a particular study"
               :parameters {:path {:id :s/id}}
               :responses {200 {:body {:message string?}}
                           404 {:body {:message string?}}}
               :handler (fn [{{user-id :id} :identity
                              {{study-id :id} :path} :parameters}]
                          (if (db/owns-study? user-id study-id)
                            (do (db/delete-study study-id)
                                {:status 200
                                 :body {:message "Study has been successfully deleted!"}})
                            {:status 404
                             :message "Researcher does not own a study with given ID"}))}}]

    ["/activate"
     {:put {:summary "Activate a study"
            :parameters {:path {:id :s/id}}
            :responses {200 {:body {:message string?}}
                        404 {:body {:message string?}}}
            :handler (fn [{{:keys [id]} :identity
                           {{study-id :id} :path} :parameters}]
                       (if (db/owns-study? id study-id)
                         (do (db/activate-study study-id)
                             {:status 200
                              :body {:message "Study has been successfully activated!"}})
                         {:status 404
                          :message "Researcher does not own a study with given ID"}))}}]

    ["/session-data"
     {:get {:summary "Download session data for a study"
            :parameters {:path {:id :s/id}}
            :responses {200 {:body {:message string?
                                    :link :s/link}}
                        404 {:body {:message string?}}}
            :handler (fn [{{:keys [id]} :identity
                           {{study-id :id} :path} :parameters}]
                       (if (db/owns-study? id study-id)
                         {:status 200
                          :body {:message "Session data file has been successfully created!"
                                 :link (get-session-results-file study-id id)}}
                         {:status 404
                          :message "Researcher does not own a study with given ID!"}))}}]]])
