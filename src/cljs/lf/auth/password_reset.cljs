(ns lf.auth.password-reset
  (:require
   [re-frame.core :as rf]
   [lf.auth.password-reset-events]))

(defn password-reset-page []
  [:section.section>div.container>div.content>section.hero.is-halfheight>div.hero-body
   [:div.container>div.column.is-5.is-offset-3>div.box>form
    [:h3.title.has-text-black "Password reset"]
    ;; TODO: Pblm: email to show.
    ;;[:div.field>label "You may now reset the password"]
    [:div.field>label "New password"]
    [:div.field>div.control>input.input.is-large
     {:type "password",
      :placeholder "Password"
      :on-change #(rf/dispatch [:pass-reset/password (-> % .-target .-value)])}]
    [:div.field>label "Please re-type new password"]
    [:div.field>div.control>input.input.is-large
     {:type "password",
      :placeholder "Please re-type password"
      :on-change #(rf/dispatch [:pass-reset/confirmation-password (-> % .-target .-value)])}]
    [:a.button.is-block.is-info.is-large.is-fullwidth
     {:on-click #(rf/dispatch [:pass-reset/reset])}
     "Set new password"]]])
