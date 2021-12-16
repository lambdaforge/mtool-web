(ns mtool-web.routes.home
  (:require
   [mtool-web.layout :as layout]
   [clojure.java.io :as io]
   [mtool-web.middleware :as middleware]
   [mtool-web.config :refer [env]]
   [mtool-web.db.study :refer [get-study study-active? study-exists?]]
   [ring.util.response]
   [ring.util.http-response :as response]))



(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/studies/:id"
    {:get {:parameters {:path {:id :s/id}}
           :handler (fn [{{study-id :id} :path-params :as request}]
                      (if (study-exists? study-id)
                        (let [{:keys [topic survey consentLink consentText] :as study} (get-study study-id)]
                          (if (study-active? study-id)
                            (layout/render request "participant.html"
                                           {:title topic
                                            :welcome (-> study :introduction :message)
                                            :thanks (-> study :thankYou :message)
                                            :study-id study-id
                                            :survey (or survey "")
                                            :consent-link consentLink
                                            :consent-text consentText
                                            :upload-route (str (:base-url env) "/api/session-data")})
                            {:status 404
                             :error "Study has not been activated!"}))
                        {:status 404
                         :error "No study with given ID!"}))}}]])
