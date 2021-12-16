(ns mtool-web.core
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            day8.re-frame.http-fx
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [lf.ajax.ajax :as ajax]
            [lf.auth.cancellation :as c]
            [lf.auth.login :as lo]
            [lf.auth.password-reset :as pr]
            [lf.auth.registration :as reg]
            [lf.auth.reset-request :as rr]
            [lf.auth.verify-account :as va]
            [lf.components.dropdown :as d]
            [markdown.core :refer [md->html]]
            mtool-web.events
            [mtool-web.media.overview :as mo]
            [mtool-web.media.upload :as u]
            [mtool-web.study.study :as sc]
            [mtool-web.study.study-overview :as so]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [reitit.core :as reitit]
            [reitit.frontend.easy :as rfe])
  (:import goog.History))

(defn nav-link
  ([uri title page]
   (nav-link uri title page nil))
  ([uri title page on-click-fn]
   [:a.navbar-item
    {:href     uri
     :class    (if (= page (-> @(rf/subscribe [:common/route]) :data :name))
                 "mtool-nav-active"
                 "mtool-nav-inactive")
     :on-click on-click-fn}
    title]))

(defn settings-dropdown []
  (d/dropdown "settings"
              "Account"
              (let [dropdown-item (fn [name url]
                                    [:a.dropdown-item {:href url} name])]
                [:div.dropdown-content
                 (dropdown-item "Cancel account" "#/cancellation")
                 [:hr.dropdown-divider]
                 [:a.dropdown-item {:href "#/"
                                    :on-click #(rf/dispatch [:logout])} "Log out"]])))

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-black>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "#/"
                                 :class (if (= :home (-> @(rf/subscribe [:common/route]) :data :name))
                                          "mtool-nav-active"
                                          "mtool-nav-inactive")}
                 [:img {:src "img/logo.svg" :width "40"}]]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click    #(swap! expanded? not)
                  :class       (when @expanded? :is-active)}
                 [:span] [:span] [:span]]]
               [:div#nav-menu.navbar-menu.is-black
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/media/overview" "Media"
                  :media-overview (fn [] (rf/dispatch [:media/all]))]
                 [nav-link "#/study/overview" "Studies"
                  :study-overview (fn [] (rf/dispatch [:study/all]))]]
                (let [settings-button (if @(rf/subscribe [:login/token])
                                        [settings-dropdown]
                                        [:div.buttons>button.button.is-light
                                         {:href "#/login" :on-click #(rf/dispatch [:common/navigate! :login])}
                                         "Log in"])]
                  [:div.navbar-end
                   [:a.navbar-item settings-button]])]]))

(defn home-page []
  [:section.section>div.container>div.content
   [:h3.title.is-4 "Welcome"]
   [:p "Welcome to the Mental Model Mapping Tool, or M-Tool. You can use this tool to capture perceptions of complex systems or phenomena (mental models). This tool is specially designed to allow you to compare mental models, and assess mental models of a diverse sample, by giving you the option to include audio instructions and visual components."]
   [:p "Please log-in to start using M-Tool. Next, you can upload the relevant media files under Media, and then create a study under Studies. For more information on how to use this tool, you can download the manual " [:a {:href "https://www.m-tool.org/gallery/M-Tool%20manual%20web%20based%20version.pdf" :target "_blank"} "here"] " or go to the M-Tool " [:a {:href "https://m-tool.org" :target "_blank"} "website."]]])

(defn error-panel []
  (when-let [error @(rf/subscribe [:common/error])]
    [:div.container>div.notification.is-danger error]))

(defn message-panel []
  (when-let [msg @(rf/subscribe [:common/message])]
    [:div.container>div.notification.is-success msg]))

(defn hero-panel []
  [:figure.image
   [:img {:src "img/banner.png"}]])

(defn page []
  (when-let [page @(rf/subscribe [:common/page])]
    (rf/dispatch [:common/init-view])
    [:div
     ;; Clicking anywhere on the page will close dropdowns
     {:on-click #(d/close-dropdowns)}
     [navbar]
     [hero-panel]
     [error-panel]
     [message-panel]
     (let [route @(rf/subscribe [:common/route])]
       (if (-> route :data :auth-required?)
         (if @(rf/subscribe [:login/token])
           [page]
           (do
             (rf/dispatch [:logout])
             (rf/dispatch [:common/navigate! :login])))
         [page]))]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(defn study-router []
  ["/study" {:auth-required? true}
   ["/overview" {:name :study-overview
                 :view #'so/study-overview-page}]
   ["/creation" {:name :study-creation
                 :view #'sc/study-page}]])

(defn media-router []
  ["/media" {:auth-required? true}
   ["/upload" {:name :media-upload
               :view #'u/upload-page}]
   ["/overview" {:name :media-overview
                 :view #'mo/overview-page}]])

(def router
  (reitit/router
    [["/login" {:name :login
           :view #'lo/login-page}]
     ["/" {:name :home
           :view #'home-page}]
     ["/verification-resent-confirmed" {:name :verification-resent-confirmed
                                        :view #'lo/resent-verification-confirmed-page}]
     ["/cancellation" {:name :cancellation
                       :view #'c/cancellation-page}]
     ["/registration" {:name :registration
                       :view #'reg/registration-page}]
     ["/registration-confirmed" {:name :registration-confirmed
                                 :view #'reg/registration-confirmed-page}]
     ["/reset-request" {:name :reset-request
                        :view #'rr/reset-request-page}]
     ["/reset-password/:id/:token"
      {:name        :reset-password
       :view        #'pr/password-reset-page
       :parameters  {:path {:id int? :token string?}}
       :controllers [{:parameters {:path [:id :token]}
                      :start      (fn [{:keys [path]}]
                                    (rf/dispatch [:pass-reset/user-id (:id path)])
                                    (rf/dispatch [:pass-reset/token (:token path)]))}]}]
     ["/verify-account/:id/:token"
      {:name        :verify-account
       :view        #'va/verify-account-page
       :parameters  {:path {:id int? :token string?}}
       :controllers [{:parameters {:path [:id :token]}
                      :start      (fn [{:keys [path]}]
                                    (rf/dispatch [:verify-account/verify (:id path) (:token path)]))}]}]
     ["/account-confirmed" {:name :account-confirmed
                            :view #'va/account-confirmed-page}]
     (media-router)
     (study-router)
     ]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))

(comment

  (init!)

  (rf/dispatch [:common/navigate! :media-overview])

  (js/alert "FOO")

  )
