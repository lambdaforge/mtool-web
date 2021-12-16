(ns lf.helper
  (:require [clojure.string :refer [split]]))

(defn remove-ns-from-key [kw]
  (keyword (last (split (name kw) #"/"))))

(defn remove-ns-from-keys [hmap]
  (reduce-kv (fn [m k v]
               (let [new-key (remove-ns-from-key k)]
                 (cond
                   (map? v) (assoc m new-key (remove-ns-from-keys v))
                   (and (vector? v) (map? (first v))) (assoc m new-key (mapv remove-ns-from-keys v))
                   :else (assoc m new-key v))))
             {}
             hmap))


(defn get-next-namespace [current-kw]
  (cond
    (#{:mapping1 :mapping2} current-kw) "mapping"
    (#{:video :image :audio} current-kw) "media"
    (= :icons current-kw) "icon"
    :else (name current-kw)))

(defn add-ns-to-key [ns kw]
  (keyword (str ns "/" (name kw))))

(defn add-ns-to-keys [current-ns hmap]
  (reduce-kv (fn [m k v]
               (let [next-key (add-ns-to-key current-ns k)
                     next-ns (get-next-namespace k)
                     next-val (cond
                                (map? v) (add-ns-to-keys next-ns v)
                                (and (vector? v) (map? (first v))) (mapv (partial add-ns-to-keys next-ns) v)
                                :else v)]
                 (assoc m next-key next-val)))
             {}
             hmap)
  #_([ns hmap]
   (zipmap (map (partial add-ns-to-key ns)
                (keys hmap))
           (vals hmap))))
