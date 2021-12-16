(ns mtool-web.media.overview
  (:require
   [mtool-web.media.overview-events]
   [re-frame.core :as rf]
   [reagent.core :as rc]
   ["react-data-table-component" :refer (createTheme) :default DataTable]))

(defn buttons []
  [:section.section>div.level
   [:div.level-item>div.field.is-grouped
    [:p.control>div.button.is-primary
     {:on-click #(rf/dispatch [:common/navigate! :media-upload])}
     "Upload media"]]])

(defn handleDeleteClick [row]
  (let [id (get (js->clj row) "id")]
    (rf/dispatch [:media/delete! id])))

(defn handlePlayClick [row]
  (let [media (js->clj row :keywordize-keys true)]
    (rf/dispatch [:media/preview media])))

(defn table []
  [:div
   [:> DataTable {:columns [{:name "File name"
                             :selector :filename
                             :sortable true
                             :ignoreRowClick true}
                            {:name "Name"
                             :selector :name
                             :sortable true
                             :ignoreRowClick true}
                            {:name "Type"
                             :selector :type
                             :sortable true
                             :ignoreRowClick true}
                            {:name "Note"
                             :selector :note
                             :width "300px"
                             :ignoreRowClick true}
                            {:name "Added at"
                             :selector :addedAt
                             :width "150px"
                             :ignoreRowClick true}
                            {:name ""
                             :width "100px"
                             :cell (fn [row]
                                     (rc/create-element "span"
                                                        #js{:className "material-icons delete-button"
                                                            :onClick (fn [e] (handlePlayClick row))}
                                                        "remove_red_eye"))}
                            {:name ""
                             :width "100px"
                             :cell (fn [row]
                                     (rc/create-element "span"
                                                        #js{:className "material-icons delete-button"
                                                            :onClick (fn [e] (handleDeleteClick row))}
                                                        "highlight_off"))}]
                  :theme "solarized"
                  :data (map #(update-in % [:addedAt] (fn [i] (.toDateString i)))
                             (let [media (rf/subscribe [:user/media])]
                               (or @media [])))}]])

(defn title []
  [:h3.title.is-4 "Media Overview"])


(defn preview []
  (let [{:keys [link type name filename] :as media} @(rf/subscribe [:media/preview])
        playing? @(rf/subscribe [:media/playing?])]
    [:section.section
     [:h3.title.is-4 "Media Preview"]
     (if (some? media)
       (let [component  (case (keyword type)
                          :image [:div
                                  [:p (str "Showing " ) [:strong filename]]
                                  [:img {:src link :alt name :height "100" :style {:max-height "100px" :width "auto" :height "auto"}}]]
                          :audio [:div
                                  [:p (str "File: ") [:strong filename]]
                                  [:audio {:controls false :id "audio-file"} [:source {:src link :type "audio/mp4"}]]
                                  [:button.button.is-primary
                                   {:onClick (fn [_] (if playing?
                                                       (rf/dispatch [:media/pause! :audio])
                                                       (rf/dispatch [:media/play! :audio])))}
                                   (if playing?
                                     "Pause"
                                     "Play")]]
                          :video [:div
                                  [:div 
                                   [:p (str "File ") [:strong filename]]
                                   [:video {:controls false
                                            :width "320"
                                            :height "240"
                                            :onended (fn [e] (js/alert "END"))
                                            :id "video-file"}
                                    [:source {:src link
                                              :type "video/mp4"}]]]
                                  [:button.button.is-primary
                                   {:onClick (fn [_] (if playing?
                                                       (rf/dispatch [:media/pause! :video])
                                                       (rf/dispatch [:media/play! :video])))}
                                   (if playing?
                                     "Pause"
                                     "Play")]]
                          [:p "Media type not supported."])]
         component)
       [:p "You can preview your media by clicking the eye button in the overview."])]))

(defn overview-page []
  [:div.container
   (preview)
   [:section.section
    (title)
    (table)]
   (buttons)])

(comment

  

  )
