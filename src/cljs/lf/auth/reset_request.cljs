(ns lf.auth.reset-request
  (:require
   [re-frame.core :as rf]
   [lf.auth.reset-request-events]))

(defn reset-request-page []
  [:section.section>div.container>div.content>section.hero.is-halfheight>div.hero-body
   [:div.container>div.column.is-6.is-offset-3
    [:div.box>form
     [:h3.title.has-text-black "Password reset"]
     [:div.field>label "Email address"]
     [:div.field>div.control>input.input.is-large
      {:placeholder "Your Email"
       :on-change #(rf/dispatch [:reset-req/email (-> % .-target .-value)])}]
     [:div.field>label "Reset instructions will be sent to your email address."]
     [:a.button.is-block.is-info.is-large.is-fullwidth
      {:href "#/reset-request" :on-click #(rf/dispatch [:reset-req/reset-required])}
      "Request password reset"]]]])
