(ns lf.auth.verify-account
  (:require
   [re-frame.core :as rf]
   [lf.auth.verify-account-events]))


(defn verify-account-page []
  [:section.section>div.container>div.content>section.hero.is-halflheight>div.hero-body
   [:div.container>div.column.is-6.is-offset-3
    ;; A deliberately empty screen
    ]])



(defn account-confirmed-page []
  [:section.section>div.container>div.content>section.hero.is-halfheight>div.hero-body
   [:div.container>div.column.is-6.is-offset-3>div.box>form
    [:div.field>label.title.is-5 "Your account is now verified."]
    [:div.field>label.title.is-5 "Your can login "
     [:a {:href "#/"} "here."]]]])
