(ns lf.auth.cancellation
  (:require
   [re-frame.core :as rf]
   [lf.auth.cancellation-events]))

(defn cancellation-page []
  [:section.section>div.container>div.content
   [:div.container>div.column.is-5.is-offset-3
    [:div.box>form
     [:h3.title.has-text-black "Account cancellation"]
     [:div.field>label "Your account will be cancelled."]
     ;; Restore if we can show the email
     ;; [:div.level>div.level-item.title.is-5 @(rf/subscribe [:login/email])]
     ;; [:div.field>label "will be cancelled."]
     [:div.field>label.has-text-danger.title.is-5 "All your data will be deleted and can not be restored!"]
     [:div.field.is-grouped.is-flex.is-flex-direction-row.is-justify-content-space-between
      [:a.button.is-info
       {:href "#/"} "Abort"]
      [:a.button.is-danger
       {:href "#/" :on-click #(rf/dispatch [:account/cancel])}
       "Cancel account"]]]]])
