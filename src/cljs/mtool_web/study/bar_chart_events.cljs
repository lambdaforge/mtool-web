(ns mtool-web.study.bar-chart-events
  (:require [re-frame.core :as rf]))



(rf/reg-event-db
 :bar-chart/icon-dropdown-modified
 (fn [db [_ new-val]]
   (assoc-in db [:study/current :barChart :image :link] new-val)))

(rf/reg-sub
 :bar-chart/icon-dropdown-value
 (fn [db _]
   (get-in db [:study/current :barChart :image :link])))



(rf/reg-event-db
 :bar-chart/field
 (fn [db [_ id value]] ;; id is a field represented as a keyword
   (assoc-in db [:study/current :barChart id] value)))


(rf/reg-sub
 :bar-chart/field
 (fn [db [_ id]] ;; id is a field represented as a keyword
   (-> db :study/current :barChart id)))
