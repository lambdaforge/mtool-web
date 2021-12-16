(ns mtool-web.db.study
  (:require
   [lf.administration.db :refer [conn]]
   [lf.helper :refer [remove-ns-from-keys add-ns-to-keys]]
   [mtool-web.config :refer [env]]
   [datahike.api :as d])
  (:import [java.util Date UUID]))


(def study-pull-spec
  [:study/id
   :study/topic
   :study/description
   :study/consentLink
   :study/consentText
   :study/survey
   {:study/mapping1 [{:mapping/audio [:media/link]}
                     {:mapping/video [:media/link]}
                     {:mapping/fixedIcon [{:fixedIcon/image [:media/link :media/name]}
                                          :fixedIcon/position]}
                     {:mapping/icons [:icon/name
                                      {:icon/image [:media/link]}
                                      {:icon/audio [:media/link]}
                                      :icon/use?]}]}
   {:study/mapping2 [{:mapping/audio [:media/link]}
                     {:mapping/video [:media/link]}
                     {:mapping/fixedIcon [{:fixedIcon/image [:media/link]}
                                          :fixedIcon/position]}
                     {:mapping/icons [:icon/name
                                      {:icon/image [:media/link]}
                                      {:icon/audio [:media/link]}
                                      :icon/use?]}]}
   {:study/barChart [{:barChart/audio [:media/link]}
                     {:barChart/video [:media/link]}
                     {:barChart/image [:media/link]}
                     :barChart/title
                     :barChart/xTitle
                     :barChart/xStart
                     :barChart/xEnd
                     :barChart/xStepSize
                     :barChart/yTitle
                     :barChart/yStart
                     :barChart/yEnd
                     :barChart/yStepSize]}
   :study/positiveArrows?
   :study/negativeArrows?
   :study/doubleHeadedArrows?
   {:study/introduction [{:introduction/video [:media/link]}
                         {:introduction/audio [:media/link]}
                         :introduction/message]}
   {:study/practice [{:practice/audio [:media/link]}]}
   {:study/thankYou [{:thankYou/audio [:media/link]}
                     :thankYou/message]}
   :study/active?
   :study/createdAt
   :study/link])

(defn add-study
  "Add a new study"
  ([user-id study]
   (add-study user-id study (.toString (UUID/randomUUID)) true))
  ([user-id study study-id active?]
   (let [study-entry (merge {:study/id study-id
                             :study/active? active?
                             :study/link (str (:base-url env) "/studies/" study-id)
                             :study/createdAt (Date.)}
                            (add-ns-to-keys "study" study))]
     (d/transact conn [{:user/id user-id
                        :user/studies [study-entry]}])
     study-id)))

(defn get-user-studies
  "Get all media for a researcher"
  [user-id]
  (let [studies (d/pull @conn [{:user/studies study-pull-spec}] [:user/id user-id])]
    (mapv remove-ns-from-keys (:user/studies studies))))

(defn get-study [study-id]
  (let [db-entry (d/pull @conn study-pull-spec [:study/id study-id])]
    (remove-ns-from-keys db-entry)))

(defn activate-study [study-id]
  (d/transact conn [[:db/add [:study/id study-id] :study/active? true]]))

(defn deactivate-study [study-id]
  (d/transact conn [[:db/add [:study/id study-id] :study/active? false]]))

(defn owns-study? [user-id study-id]
  (d/q '[:find ?e .
         :in $ ?rid ?sid
         :where [?e :study/id ?sid]
         [?r :user/id ?rid]]
       @conn user-id study-id))

(defn delete-study [study-id]
  (d/transact conn [[:db.purge/entity [:study/id study-id]]]))

(defn add-session-data [study-id {:keys [duration] :as session-data}]
  (let [session (->> (if duration
                        (assoc session-data :duration (float duration))
                        session-data)
                     (add-ns-to-keys "session"))]
    (d/transact conn [{:study/id study-id
                       :study/sessions [session]}])))

(defn get-session-data [study-id]
  (mapv #(remove-ns-from-keys (dissoc % :db/id))
        (:study/sessions (d/pull @conn '[{:study/sessions [*]}] [:study/id study-id]))))

(defn get-session-ids [study-id]
  (->> [:study/id study-id]
       (d/pull @conn [{:study/sessions [:db/id]}] )
       :study/sessions
       (map :db/id)))

(defn import-sessions [study-id session-ids]
  (d/transact conn [{:db/id [:study/id study-id]
                     :study/sessions session-ids}]))

(defn study-exists? [study-id]
  (d/entity @conn [:study/id study-id]))

(defn study-active? [study-id]
  (:study/active? (d/entity @conn [:study/id study-id])))
