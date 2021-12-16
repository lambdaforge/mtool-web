(ns mtool-web.study.study
  (:require
   [mtool-web.study.study-events]
   [re-frame.core :as rf]
   [reagent.core :as rc]
   [mtool-web.view-helper :as vh]
   [lf.components.dropdown :as d]
   [mtool-web.study.mapping :as m]
   [mtool-web.study.bar-chart :as bc]
   [mtool-web.study.study-model :as sm]
   ["react-data-table-component" :refer (createTheme) :default DataTable]))

(defn buttons []
  [:section.section>div.level
   [:div.level-left>div.level-item]
   [:div.level-right>div.level-item>div.field.is-grouped
    [:p.control>button.button.is-danger
     {:on-click #(do
                   (rf/dispatch [:common/show-warning false])
                   (rf/dispatch [:common/navigate! :study-overview]))}
     "Cancel"]
    (when-let [mode (rf/subscribe [:study-screen/mode])]
      (if (= @mode :edit)
        [:p.control>div.button.is-primary
         {:on-click #(rf/dispatch [:study/update!])}
         "Update study"]
        [:p.control>div.button.is-primary
         {:on-click #(do
                       (rf/dispatch [:study/create!])
                       (.scrollTo js/window 0 0))} ;; Move to top of page
         "Create study"]))]])

(defn name-field
  []
  (vh/req-text-field "Name" :study/name))

(defn description []
  (vh/field "Description"
            [:textarea.textarea
             {:on-change #(rf/dispatch [:study/desc (-> % .-target .-value)])
              :value (when-let [desc (rf/subscribe [:study/desc])] @desc)}]
            "Optional."))

(defn consent-text []
  (vh/field "Consent Text"
            [:textarea.textarea
             {:on-change #(rf/dispatch [:study/consentText (-> % .-target .-value)])
              :value (when-let [consent-text (rf/subscribe [:study/consentText])] @consent-text)}]
            "Optional."))

(defn consent-link []
  (vh/field "Link to external consent page"
            [:div.field>div.control>input.input
             {:type "text"
              :on-change #(rf/dispatch [:study/consentLink (-> % .-target .-value)])
              :value (when-let [consent-link (rf/subscribe [:study/consentLink])] @consent-link)}]
            "Optional."))

(defn survey []
  (vh/field "Survey"
            [:div.field>div.control>input.input
             {:type "text"
              :placeholder "https://link-to-my-survey.org/?id={USER_ID}"
              :on-change #(rf/dispatch [:study/survey (-> % .-target .-value)])
              :value (when-let [survey (rf/subscribe [:study/survey])] @survey)}]
            "Optional. Add {USER_ID} to your survey link to include the participant ID in your study, e.g. https://link-to-my-survey.org/?id={USER_ID}"))

(defn welcome-message []
  (vh/req-area-field "Welcome message" :study/welcome-msg))

(defn thankyou-message []
  (vh/req-area-field "End of study message" :study/thankyou-msg))

(defn title []
  [:section.section>div.level
   [:div.level-left>div.level-item>h3.title.is-4
    (when-let [title (rf/subscribe [:study-screen/title])] @title)]])

(defn checkbox
  "db-id is a keyword used in app db"
  [desc db-id]
  [:div
   [:label.checkbox.field
    [:input {:type "checkbox"
             :checked (when-let [checked? (rf/subscribe [db-id])] @checked?)
             :on-change #(rf/dispatch [db-id (-> % .-target .-checked)])}]
    (str " " desc)]])

(defn screens []
  (vh/field "Screens"
            (list ^{:key "c1"} [checkbox "Mapping screen 1" :study/mapping-1-checkbox]
                  ^{:key "c2"} [checkbox "Mapping screen 2" :study/mapping-2-checkbox]
                  ^{:key "c3"} [checkbox "Bar chart screen" :study/bar-chart-checkbox])
            "At least one required"))

(defn arrows []
  (let [mapping1 @(rf/subscribe [:mapping/instance 1])
        mapping2 @(rf/subscribe [:mapping/instance 2])]
    (when (or mapping1 mapping2)
      (vh/field "Arrows"
                (list ^{:key "c4"} [checkbox "neutral" :study/neutral-checkbox]
                      ^{:key "c5"} [checkbox "positive and negative" :study/signed-checkbox]
                      ^{:key "c6"} [checkbox "double-headed" :study/double-headed-checkbox])
                "At least one required"))))

(defn mappings []
  [:h3.title.is-5 "Icons"]
  [:div.tile.is-ancestor
   [:div.tile.is-vertical
    (when @(rf/subscribe [:mapping/instance 1])
      [:div.tile.is-parent.is-vertical>article.tile.is-child.notification
       (m/mapping "Mapping Screen 1" 1)])
    (when @(rf/subscribe [:mapping/instance 2])
      [:div.tile.is-parent.is-vertical>article.tile.is-child.notification
       (m/mapping "Mapping Screen 2" 2)])]])

(defn video-audio-field [id db-path default-media media-type]
  (vh/field
   id
   (d/dropdown id
               (let [v @(rf/subscribe [:video-audio/dropdown-value db-path])]
                 (if (or (= nil v) (= default-media v))
                   "Default"
                   (vh/link-to-name v)))
               (concat [[:div.dropdown-content>a.dropdown-item
                         {:on-click #(rf/dispatch [:video-audio/dropdown-modified db-path default-media])}
                         "Default"]]
                       (doall
                        (map (fn [{:keys [name filename link]}]
                               ^{:key filename} [:div.dropdown-content>a.dropdown-item
                                                 {:on-click #(rf/dispatch [:video-audio/dropdown-modified db-path link])}
                                                 name])
                             (mtool-web.media.media-model/media media-type)))))
   nil))

(defn video-audio []
  (let [mapping1 @(rf/subscribe [:mapping/instance 1])
        mapping2 @(rf/subscribe [:mapping/instance 2])
        bar-chart @(rf/subscribe [:bar-chart/instance])]
    (list
     ^{:key "title"}[:h3.title.is-5 "Videos And Audio"]
     (doall
      (for [[label db-path default-media type]
            (filter some? [^{:key "welcome-audio"} ["Welcome audio" (sm/introduction-audio-path)
                            sm/introduction-audio :audio]
                           (when (or mapping1 mapping2)
                             ^{:key "intro"} ["Introduction video mental model practice task" (sm/introduction-video-path)
                              sm/introduction-video :video])
                           (when (or mapping1 mapping2)
                             ^{:key "pratice-audio"} ["Practice audio" (sm/practice-audio-path) "/participant/audio/practice.m4a" :audio])
                           (when mapping1
                             ^{:key "exp-video"} ["Icon explanation video" (sm/icon-explanation-video-path)
                              sm/mapping-video  :video])
                           (when mapping2
                             ^{:key "exp-2-video"} ["Second icon explanation video" (sm/second-icon-video-path)
                              sm/mapping-video :video])
                           (when mapping1
                             ^{:key "m1-audio"} ["Mapping screen 1 audio" (sm/mapping-1-audio-path)
                              sm/mapping-audio :audio])
                           (when mapping2
                             ^{:key "m2-audio"} ["Mapping screen 2 audio" (sm/mapping-2-audio-path)
                              sm/mapping-audio :audio])
                           (when bar-chart
                             ^{:key "brchrt-video"} ["Bar chart tutorial video" (sm/bar-chart-video-path)
                              sm/bar-chart-video :video])
                           (when bar-chart
                             ^{:key "brchrt-audio"} ["Bar chart screen audio" (sm/bar-chart-audio-path)
                              sm/bar-chart-audio :audio])
                           ^{:key "thx-audio"} ["Thank you audio" (sm/thanks-audio-path)
                            sm/thank-you-audio :audio]])]
        (video-audio-field label db-path default-media type))))))

(defn study-page []
  [:section.section>div.container
   (title)
   (vh/info-box "You can create new studies here. Note: Please make sure you have uploaded the media that you want to use before you create a study.")
   [:section.section
    (name-field)
    (description)
    (consent-text)
    (consent-link)
    (welcome-message)
    (thankyou-message)
    (survey)
    (screens)
    (arrows)]
   [:section.section
    (video-audio)]
   (mappings)
   (bc/bar-chart  "bar-chart")
   (buttons)])
