(ns lf.administration.db
  (:require
   [datahike.api :as d]
   [mount.core :refer [defstate]]
   [clojure.edn :as edn]
   [mtool-web.config :refer [env]]
   [lf.helper :refer [remove-ns-from-keys]]
   [clojure.java.io :as io]
   [lf.administration.migrations :as m])
  (:import [java.util UUID]))


(defstate conn
          :start (let [config (:datahike env)]
                   (if (d/database-exists? config)
                     (let [conn (d/connect config)
                           study-sessions-schema (-> @conn :schema :study/sessions)]
                       (when (:db/isComponent study-sessions-schema)
                         (datahike.api/transact conn [[:db/retract [:db/ident :study/sessions] :db/isComponent true]]))
                       (m/ensure-norms conn)
                       conn)
                     (let [_ (d/create-database config)
                           conn (d/connect config)]
                       (d/transact conn (edn/read-string (slurp (io/resource "db-schema.edn"))))
                       (when-let [data-file (io/resource "db-initial-data.edn")]
                         (d/transact conn (edn/read-string (slurp data-file))))
                       (m/ensure-norms conn)
                       conn)))
          :stop (d/release conn))

(defn show-schema
  "Show currently installed schema"
  []
  (.-schema @conn))


;; User account administration
(defn get-user-eid
  "Get the entity ID of a user by attribute-value pair"
  [unique-attr val]
  (d/q '[:find ?e . :in $ ?a ?v :where [?e ?a ?v]]
       @conn unique-attr val))

(defn get-user
  "Get user map
  - by id with (get-user :user/id id
  - by email with (get-user :user/email email)"
  [unique-attr val]
  (when-let [eid (get-user-eid unique-attr val)]
    (-> (d/pull @conn [:user/id :user/email :user/pwd :user/verified :user/createdAt] eid)
        remove-ns-from-keys)))

(defn set-user-attribute
  "Set user attribute by id"
  [id attr new-val]
  (d/transact conn [[:db/add [:user/id id] attr new-val]]))

(defn get-user-id
  "Get user ID"
  [email]
  (:id (get-user :user/email email)))

(defn get-user-email
  "Get user email address"
  [id]
  (:email (get-user :user/id id)))

(defn add-user
  "Add a new, unverified user account.
   Return the user's ID"
  [{:keys [email pwd init-date]}]
  (let [uuid (.toString (UUID/randomUUID))]
    (d/transact conn [{:user/id uuid
                       :user/email email
                       :user/pwd pwd
                       :user/verified false
                       :user/createdAt init-date}])
    uuid))

(defn verify-user
  "Set verification flag of user to true."
  [id]
  (set-user-attribute id :user/verified true))

(defn reset-user-pwd
  "Resets password of user."
  [id new-pwd]
  (set-user-attribute id :user/pwd new-pwd))

(defn user-email-exists?
  "Check if user with email address exists"
  [email]
  (get-user :user/email email))

(defn user-exists?
  "Check if user exists"
  [id]
  (get-user :user/id id))

(defn user-verified?
  "Check if user account has been verified"
  [id]
  (:verified (get-user :user/id id)))

(defn delete-user
  "Delete user account"
  [id]
  (d/transact conn [[:db.purge/entity [:user/id id]]]))
