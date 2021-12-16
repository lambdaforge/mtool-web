(ns mtool-web.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [mtool-web.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[mtool-web started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[mtool-web has shut down successfully]=-"))
   :middleware wrap-dev})
