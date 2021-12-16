(ns mtool-web.handler
  (:require
    [mtool-web.middleware :as middleware]
    [mtool-web.config :as c]
    [clojure.java.io :as io]
    [mtool-web.layout :refer [error-page]]
    [mtool-web.routes.home :refer [home-routes]]
    [mtool-web.routes.media :refer [media-routes]]
    [mtool-web.routes.study :refer [study-routes]]
    [mtool-web.routes.participant :refer [participant-routes]]
    [lf.administration.routes :refer [api-base-route administration-routes]]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring :as ring]
    [reitit.dev.pretty :as pretty]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [mtool-web.env :refer [defaults]]
    [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))


(def route-opts
  {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
   ;;:validate spec/validate ;; enable spec validation for route data
   ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
   :exception pretty/exception})

(def ring-handler (ring/ring-handler
                    (ring/router
                     [(home-routes)                         ;; TODO: why functions?
                      (into (api-base-route "MTool API")
                            [(administration-routes)
                             (media-routes)
                             (study-routes)
                             (participant-routes)])]
                      route-opts)
                    (ring/routes
                      (swagger-ui/create-swagger-ui-handler
                        {:path   "/swagger-ui"
                         :url    "/api/swagger.json"
                         :config {:validator-url nil}})
                      (ring/create-resource-handler
                        {:path "/"})
                      (wrap-content-type
                        (wrap-webjars (constantly nil)))
                      (ring/create-default-handler
                        {:not-found
                         (constantly (error-page {:status 404, :title "404 - Page not found"}))
                         :method-not-allowed
                         (constantly (error-page {:status 405, :title "405 - Not allowed"}))
                         :not-acceptable
                         (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))


(mount/defstate app-routes
  :start ring-handler)

(defn app []
  (when-not (.exists (io/file c/user-media-dir))
   (.mkdir (io/file c/user-media-dir)))
  (middleware/wrap-base #'app-routes))
