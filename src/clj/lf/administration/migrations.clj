(ns lf.administration.migrations
  (:require
   [datahike.api :as d]))

(def m2-barchart-y-step-size
  {:m2-barchart-y-step-size {:tx-data [{:db/doc         "Factor for one y step"
                                        :db/ident       :barChart/yStepSize
                                        :db/valueType   :db.type/long
                                        :db/cardinality :db.cardinality/one}]}})

(def m3-default-y-stepsize
  {:m3-default-y-stepsize {:tx-fn (fn [conn]
                                    (->> (d/q {:query '[:find [?e ...]
                                                        :where
                                                        [?e :barChart/title _]]
                                               :args [@conn]})
                                         (mapv #(hash-map :db/id % :barChart/yStepSize 1))))}})

(defn attribute-installed? [conn attr]
  (some? (d/entity @conn [:db/ident attr])))

(defn ensure-norm-attribute [conn]
  (if-not (attribute-installed? conn :tx/norm)
    (:db-after (d/transact conn {:tx-data [{:db/ident :tx/norm
                                           :db/valueType :db.type/keyword
                                           :db/cardinality :db.cardinality/one}]}))


    @conn))

(defn norm-installed? [db norm]
  (->> {:query '[:find (count ?t)
                 :in $ ?tn
                 :where
                 [_ :tx/norm ?tn ?t]]
        :args [db norm]}
       d/q
       first
       some?))

(def norm-list
  [m2-barchart-y-step-size
   m3-default-y-stepsize])

(defn ensure-norms [conn]
  (let [db (ensure-norm-attribute conn)]
    (println "Checking migrations ...")
    (doseq [norm-map norm-list]
      (doseq [[norm {:keys [tx-data tx-fn]
                     :or {tx-data []
                          tx-fn (fn [_] [])}}] norm-map]
        (println "Checking migration" norm)
        (when-not (norm-installed? db norm)
          (println "Run migration" norm)
          (d/transact conn {:tx-data (vec (concat [{:tx/norm norm}]
                                                  tx-data
                                                  (tx-fn conn)))}))))
    (println "Done")))

