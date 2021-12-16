(ns mtool-web.study.mapping-events
  (:require [cljs.spec.alpha :as s]
            [mtool-web.study.study-model :as sm]
            [mtool-web.validation]
            [mtool-web.view-helper :as vh]
            [re-frame.core :as rf]))

;; mapping 1 and 2

(defn mapping-instance
  [db mapping-num]
  (-> db :study/current ((sm/mapping-keyword mapping-num))))

(defn toggle-mapping-presence
  [db [_ mapping-num]]
  (let [present? (mapping-instance db mapping-num)]
    (if present?
      (assoc db :study/current (dissoc (:study/current db) (sm/mapping-keyword mapping-num)))
      (assoc-in db (sm/mapping-path mapping-num) (sm/new-mapping)))))


(rf/reg-sub
 :mapping/instance
 (fn [db [_ mapping-num]]
   (mapping-instance db mapping-num)))


;; icons

(rf/reg-sub
 :mapping/icons
 (fn [db [_ mapping-num]]
   (get-in db (sm/icons-path mapping-num))))


;; *non-fixed* icon dropdowns
;;

(rf/reg-event-db
 :mapping/icon-dropdown-modified
 (fn [db [_ mapping-num icon-num media-type new-val]]
   (let [icons (get-in db (sm/icons-path mapping-num))
         icon (nth icons icon-num)
         icon' (if (nil? new-val)
                 (dissoc icon media-type)
                 (assoc-in icon [media-type :link] new-val))
         icon' (if (= :image media-type)
                 (assoc icon' :name (vh/link-to-name new-val))
                 icon')
         icons' (assoc icons icon-num icon')]
     (assoc-in db (sm/icons-path mapping-num) icons'))))

(rf/reg-sub
 :mapping/icon-dropdown-value
 (fn [db [_ mapping-num icon-num media-type]]
   (let [icons (get-in db (sm/icons-path mapping-num))
         icon (nth icons icon-num)]
     (-> icon media-type :link))))

(rf/reg-event-db
 :mapping/new-icon
 (fn [db [_ mapping-num]]
   (let [icons-path (sm/icons-path mapping-num)]
     (assoc-in db icons-path
               (conj (get-in db icons-path) (sm/new-icon))))))


;; icon checkbox

(defn icon-used?
  [mapping-num icon-num db]
  (let [icons (get-in db (conj (sm/mapping-path mapping-num) :icons))
        icon (nth icons icon-num)]
    (:use? icon)))

(rf/reg-event-db
 :mapping/icon-checkbox-checked
 (fn [db [_ mapping-num icon-num]]
   (let [used? (icon-used? mapping-num icon-num db)
         icons (get-in db (sm/icons-path mapping-num))
         icon (nth icons icon-num)
         icon' (assoc icon :use? (not used?))
         icons' (assoc icons icon-num icon')]
     (assoc-in db (sm/icons-path mapping-num) icons'))))

(rf/reg-sub
 :mapping/icon-checkbox
 (fn [db [_ mapping-num icon-num]]
   (icon-used? mapping-num icon-num db)))

;; icon name

(rf/reg-event-db
 :mapping/icon-name-changed
 (fn [db [_ mapping-num icon-num new-val]]
   (let [icons (get-in db (sm/icons-path mapping-num))
         icon (nth icons icon-num)
         icon' (assoc icon :name new-val)
         icons' (assoc icons icon-num icon')]
     (assoc-in db (sm/icons-path mapping-num) icons'))))

(rf/reg-sub
 :mapping/icon-name
 (fn [db [_ mapping-num icon-num]]
   (let [icons (get-in db (conj (sm/mapping-path mapping-num) :icons))
         icon (nth icons icon-num)]
     (:name icon))))


;; icon delete
(rf/reg-event-db
 :mapping/delete-icon
 (fn [db [_ mapping-num icon-num]]
   (sm/delete-icon mapping-num icon-num db)))


;; fixed-icon handling
;;

(rf/reg-event-db
 :mapping/fixed-icon-dropdown-modified
 (fn [db [_ mapping-num new-val]]
   (assoc-in db (sm/fixed-icon-path mapping-num) new-val)))


(rf/reg-sub
 :mapping/fixed-icon-dropdown-value
 (fn [db [_ mapping-num]]
   (get-in db (sm/fixed-icon-path mapping-num))))


;; fixed-icon-position handling
;;
(rf/reg-event-db
 :mapping/fixed-icon-position-dropdown-modified
 (fn [db [_ mapping-num new-val]]
   (assoc-in db (sm/fixed-icon-position-path mapping-num) new-val)))

(rf/reg-sub
 :mapping/fixed-icon-position-dropdown-value
 (fn [db [_ mapping-num]]
   (get-in db (sm/fixed-icon-position-path mapping-num))))
