(ns mtool-web.db.media
  (:require
    [lf.administration.db :refer [conn]]
    [lf.helper :refer [remove-ns-from-keys add-ns-to-keys]]
    [datahike.api :as d])
  (:import [java.util Date UUID]))


(def media-pull-spec [:media/id :media/type :media/note :media/name :media/filename :media/link :media/addedAt])

(def media-name-type-q '[:find ?e .
                         :in $ ?r ?n ?t
                         :where
                         [?e :media/owner ?r]
                         [?e :media/name ?n]
                         [?e :media/type ?t]])

(defn media-with-name-type-exists?
  "Check if media exists for researcher"
  [researcher-id name type]
  (some? (d/q {:query media-name-type-q
               :args [@conn [:user/id researcher-id] name type]})))

(defn add-media
  "Add a new media file entry"
  [researcher-id media]
  (let [uuid (.toString (UUID/randomUUID))
        media-entry (merge {:media/id      uuid
                            :media/addedAt (Date.)
                            :media/owner   [:user/id researcher-id]}
                           (add-ns-to-keys "media" media))]
    (d/transact conn [media-entry])
    uuid))

(defn get-user-media
  "Get all media for a researcher"
  [user-id]
  (let [eids (->> (d/q '[:find ?e
                         :in $ ?researcher
                         :where
                         [?e :media/owner ?r]
                         [?r :user/id ?researcher]]
                       @conn user-id)
                  (map first))]
    (map remove-ns-from-keys (d/pull-many @conn media-pull-spec eids))))

(defn owns-media? [researcher-id media-id]
  (d/q '[:find ?e .
         :in $ ?rid ?mid
         :where
         [?e :media/id ?mid]
         [?e :media/owner ?r]
         [?r :user/id ?rid]]
       @conn researcher-id media-id))

(defn delete-media
  "Delete a media entry for a researcher"
  [media-id]
  (d/transact conn [[:db.purge/entity [:media/id media-id]]]))

(defn get-media [media-id]
  (remove-ns-from-keys (d/pull @conn media-pull-spec [:media/id media-id])))

(defn get-media-id [link]
  (:media/id (d/entity @conn [:media/link link])))

(defn get-media-filename [media-id]
  (:media/filename (d/entity @conn [:media/id media-id])))

(defn media-exists? [media-id]
  (:db/id (d/entity @conn [:media/id media-id])))

(defn get-media-location [media-id]
  (:media/path (d/entity @conn [:media/id media-id])))

(defn media-usage [media-id]
  (->> (d/q '[:find ?a
             :in $ ?mid
             :where
             [?m :media/id ?mid]
             [?e ?a ?m]]
           @conn media-id)
       (map first)))
