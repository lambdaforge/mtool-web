(ns lf.auth.login
  (:require
   [re-frame.core :as rf]
   [lf.auth.login-events]))

(defn login-page []
  [:section.section>div.container>div.content>section.hero.is-halftheight>div.hero-body
   [:div.container>div.column.is-5.is-offset-3
    [:div.box>form {:autocomplete "on"}
     [:h3.title.has-text-black "Login"]
     [:div.field>label "Email address"]
     [:div.field>div.control>input.input.is-large
      {:placeholder "Your Email"
       :autocomplete "email"
       :on-change #(rf/dispatch [:login/email (-> % .-target .-value)])}]
     [:div.field>label "Password"]
     [:div.field>div.control>input.input.is-large
      {:type "password",
       :autocomplete "current-password"
       :placeholder "Your Password"
       :on-change #(rf/dispatch [:login/password (-> % .-target .-value)])}]
     [:div.field
      [:a {:href "#/reset-request"} "Forgot password?"]]
     (if (= :not-verified @(rf/subscribe [:common/errorStatus]))
       [:a.button.is-block.is-warning.is-large.is-fullwidth
        {:href "#/" :on-click #(rf/dispatch [:resent-verification])}
        "Resent verification"]
       [:a.button.is-block.is-info.is-large.is-fullwidth
        {:on-click #(rf/dispatch [:login])}
        "Log in"])
     [:hr]
     [:div.has-text-centered "Not registered yet? Sign up "
      [:a {:href "#/registration"}
       "here"]]]]])

(defn resent-verification-confirmed-page []
  [:section.section>div.container>div.content>section.hero.is-fullheight>div.hero-body
   [:div.container>div.column.is-5.is-offset-3>div.box>form
    [:h3.title.has-text-black "Resend activation"]
    [:div.field>label "You will receive an activation email at "
     [:span.title.is-6 @(rf/subscribe [:login/email])]
     ""]]])
