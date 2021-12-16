(ns lf.auth.registration
  (:require
   [re-frame.core :as rf]
   [lf.auth.registration-events]))

(defn registration-page []
  [:section.section>div.container>div.content>section.hero.is-halfheight>div.hero-body
   [:div.container>div.column.is-5.is-offset-3>div.box>form
    [:h3.title.has-text-black "Registration"]
    [:div.field>label "Email address"]
    [:div.field>div.control>input.input.is-large
     {:placeholder "Your Email"
      :on-change #(rf/dispatch [:registration/email (-> % .-target .-value)])}]
    [:div.field>label "Password"]
    [:div.field>div.control>input.input.is-large
     {:type "password",
      :placeholder "Password"
      :on-change #(rf/dispatch [:registration/password (-> % .-target .-value)])}]
    [:div.field>label "Please re-type password"]
    [:div.field>div.control>input.input.is-large
     {:type "password",
      :placeholder "Please re-type password"
      :on-change #(rf/dispatch [:registration/confirmation-password (-> % .-target .-value)])}]
    [:label.checkbox.field
     [:input {:type "checkbox"
              :checked @(rf/subscribe [:registration/terms-accepted?])
              :on-change #(rf/dispatch [:registration/terms (-> % .-target .-checked)])}]
     " Accept "
     [:a {:href "#/" :on-click #(rf/dispatch [:registration/show-terms])} ;; TODO
      "terms and conditions"]]
    [:a.button.is-block.is-info.is-large.is-fullwidth
     {:href "#/registration" :on-click #(rf/dispatch [:registration/signup])}
     "Sign up"]]])


(defn registration-confirmed-page []
  [:section.section>div.container>div.content>section.hero.is-halfheight>div.hero-body
   [:div.container>div.column.is-5.is-offset-3>div.box>form
    [:h3.title.has-text-black "Thank you for your registration!"]
    [:div.field>label "You will receive an activation email at "
     [:span.title.is-6 @(rf/subscribe [:registration/email])]]]])
