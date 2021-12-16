(ns mtool-web.media.upload
  (:require
   [mtool-web.media.upload-events]
   [mtool-web.view-helper :as vh]
   [re-frame.core :as rf]))


(defn title []
  [:h3.title.is-4 "New Media"])

(defn file-field []
  (vh/field
   "File"
   [:div.file.has-name.is-fullwidth
    [:label.file-label
     [:input.file-input {:type "file"
                         :name "media"
                         :accept ".mp3,.mp4,.png,.jpg,.jpeg,.png"
                         :on-change #(rf/dispatch [:upload/file (-> % .-target .-files (aget 0))])}]
     [:span.file-cta
      [:span.file-icon
       [:i.fas.fa-upload]]
      [:span.file-label "Choose a file…"]]
     [:span.file-name (when-let [file @(rf/subscribe [:upload/file])]
                        (-> file .-name))]]]
   "Required and the maximum file size allowed is 100 MB. Please make sure that the files can be used in the browser. We recommend .mp4 for videos, .mp3 for audio, and .png for images."))

(defn note-field []
  (vh/field "Note"
            [:textarea.textarea
             {:on-change #(rf/dispatch [:upload/note (-> % .-target .-value)])
              :value (when-let [note (rf/subscribe [:upload/note])]
                       @note)}]
            "Optional"))

(defn name-field []
  (vh/req-text-field "Name" :upload/name))

(defn- is-type-field-checked [type-field]
  (when-let [type (rf/subscribe [:upload/type])]
    (= @type type-field)))

(defn type-field []
  (vh/field "Type"
         [:div.control
          [:label.radio
           [:input {:type "radio", :name "type"
                    :on-click #(rf/dispatch [:upload/type "video"])
                    :checked (is-type-field-checked "video")}]
           " Video"]
          [:label.radio
           [:input {:type "radio", :name "type"
                    :on-click #(rf/dispatch [:upload/type "audio"])
                    :checked (is-type-field-checked "audio")}]
           " Audio"]
          [:label.radio
           [:input {:type "radio", :name "type"
                    :on-click #(rf/dispatch [:upload/type "image"])
                    :checked (is-type-field-checked "image")}]
           " Image"]
          ]))


(defn buttons []
  (let [is-loading? @(rf/subscribe [:upload/is-loading?])]
    [:section.section>div.level
     [:div.level-left>div.level-item]
     [:div.level-right>div.level-item>div.field.is-grouped
      [:p.control>button.button.is-danger
       {:on-click #(rf/dispatch [:common/navigate! :media-overview])}
       "Cancel"]
      [:p.control>button.button
       {:disabled (or (when-let [f (rf/subscribe [:upload/file])] (nil? @f))
                       (when-let [n (rf/subscribe [:upload/name])] (empty? @n))
                       (when-let [t (rf/subscribe [:upload/type])] (empty? @t)))
        :on-click #(do
                     (rf/dispatch [:common/init-view])
                     (rf/dispatch [:upload/upload!]))
        :class (str "is-primary " (when is-loading? "is-loading"))}
       "Upload media"]]]))

(defn upload-page []
  [:section.section>div.container
   (title)
   (file-field)
   (name-field)
   (note-field)
   (type-field)
   (buttons)])
