(ns mtool-web.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]))

(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))


(def secrets (load-config :merge [(source/from-system-props)
                                  (source/from-env)]))

(def user-db
  "loads from config first, then secrets.edn, then props and last env"
  (:user-db secrets))

(def private-key
  "loads from config first, then secrets.edn, then props and last env"
  (:private-key secrets))

(def user-media-dir "userfiles")

(def maxFileSize (* 100 1024 1024))                              ;; 100 Mb