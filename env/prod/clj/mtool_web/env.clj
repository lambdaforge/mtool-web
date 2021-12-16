(ns mtool-web.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[mtool-web started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[mtool-web has shut down successfully]=-"))
   :middleware identity})
