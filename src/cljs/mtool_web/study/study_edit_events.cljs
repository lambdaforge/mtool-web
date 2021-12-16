(ns mtool-web.study.study-edit-events
  (:require [clojure.spec.alpha :as s]
            [lf.ajax.ajax :as lajax]
            [lf.helper :as h]
            [mtool-web.study.study-model :refer [get-study]]
            [re-frame.core :as rf]
            [mtool-web.study.study-validation :as v]
            [taoensso.timbre :as log]))

(rf/reg-event-fx
 :study/edit
 (fn [{:keys [db]} [_ id]]
   (let [study (get-study db id)]
     {:db (-> (assoc db :study/current study)
            (assoc :study-screen/title (:topic study))
            (assoc :study-screen/mode :edit))
      :common/navigate-fx! [:study-creation]})))



(rf/reg-event-fx
 ::update-success
 (fn [cofx event]
   (let [db (:db cofx)]
     (log/info "------ Successful study update: " event)
     (rf/dispatch [:study/all])
     {:db (h/set-message db "Study updated successfully.")
      :common/navigate-fx! [:study-overview]})))

(rf/reg-event-fx
 ::update-failure
 (fn [cofx event]
   (let [db (:db cofx)]
     (log/error "----- study update failed: " event)
     {:db (h/set-error db "Error: Could not update study.")})))


(rf/reg-event-fx
 :study/update!
 (fn [cofx _]
   (let [db (:db cofx)
         token (:login/token db)
         study (:study/current db)]
     (rf/dispatch [:common/init-view]) ;; clear error and status msg
     (.scrollTo js/window 0 0) ;; Move to top of page

     (if-let [msg (v/check-study study)]
       (rf/dispatch [:study/invalid-input msg])
       {:http-xhrio (lajax/as-transit
                     {:method :post
                      :uri (str "/api/studies/" (:id study))
                      :headers {:authorization (str "Token " token)}
                      :params {:study study}
                      :on-success [::update-success]
                      :on-failure [::update-failure]})}))))
