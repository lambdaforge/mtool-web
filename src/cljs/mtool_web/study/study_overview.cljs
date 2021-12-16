(ns mtool-web.study.study-overview
  (:require
   [lf.helper :as h]
   [mtool-web.study.study-overview-events]
   [mtool-web.study.study-events]
   [mtool-web.study.study-edit-events]
   [re-frame.core :as rf]
   [reagent.core :as rc]
   ["react-data-table-component" :refer (createTheme) :default DataTable]))



(defn buttons []
  [:section.section>div.level
   [:div.level-item>div.field.is-grouped
    [:p.control>div.button.is-primary
     {:on-click #(do
                   (rf/dispatch [:study/new])
                   (rf/dispatch [:common/navigate! :study-creation]))}
     "Add new study"]]])



(defn handleEditClick [row]
  (let [id (get (js->clj row) "id")]
    (rf/dispatch [:study/edit id])))

(defn link-modal []
  (let [link @(rf/subscribe [:study/link])
        link-copied? @(rf/subscribe [:link/copied?])]
    [:div.modal {:id "link-modal"}
     [:div.modal-background]
     [:div.modal-card
      [:header.modal-card-head
       [:p.modal-card-title "Study link"]
       [:button.delete {:aria-label "close"
                        :on-click (fn [_]
                                    (rf/dispatch [:link/copied! nil])
                                    (h/toggle-component "link-modal"))}]]
      [:section.modal-card-body
       "This is the link you can share with your participants:"
       [:div>a {:href link :id "study-link" :target "_blank"} link]]
      [:footer.modal-card-foot.is-grouped.is-flex.is-flex-direction-row.is-justify-content-space-between
       [:button.button
        {:on-click (fn [_]
                     (rf/dispatch [:link/copied! nil])
                     (h/toggle-component "link-modal"))}
        "Close"]
       [:button.button
        {:class (if link-copied? "is-success" "is-info")
         :on-click #(rf/dispatch [:link/copied! link])}
        (if link-copied?
          "Link copied!"
          "Copy link")]]]]))

(defn delete-modal []
  (let [{:keys [id topic]} @(rf/subscribe [:study/deletion])]
    [:div.modal {:id "deletion-modal"}
     [:div.modal-background]
     [:div.modal-card
      [:header.modal-card-head.is-danger
       [:p.modal-card-title "Study deletion"]
       [:button.delete {:aria-label "close"
                        :on-click (fn [_]
                                    (h/toggle-component "deletion-modal"))}]]
      [:section.modal-card-body
       [:p "Study " [:strong topic] " will be deleted. All the data related to the study will be erased."]
       [:p [:strong.has-text-danger "Do you really want to proceed?"]]]
      [:footer.modal-card-foot.is-grouped.is-flex.is-flex-direction-row.is-justify-content-space-between
       [:button.button
        {:on-click (fn [_]
                     (h/toggle-component "deletion-modal"))}
        "Cancel"]
       [:button.button.is-danger
        {:on-click #(rf/dispatch [:study/delete! id])}
        "Delete study"]]]]))


(defn csv-link-modal []
  [:div.modal {:id "csv-link-modal"}
   [:div.modal-background]
   [:div.modal-card
    [:header.modal-card-head
     [:p.modal-card-title "Study download"]
     [:button.delete {:aria-label "close"
                      :on-click #(h/toggle-component "csv-link-modal")}]]
    [:section.modal-card-body
     [:a {:href @(rf/subscribe [:download/csv-link])}
      "Click on this link to download the study data"]]
    [:footer.modal-card-foot
     [:button.button
      {:on-click #(h/toggle-component "csv-link-modal")}
      "Close"]]]])

(defn- ->clj [row]
  (js->clj row :keywordize-keys true))

(defn handleActivateDeactivateClick [row]
  (let [id (:id (->clj row))]
    (if (:active? (->clj row))
      (rf/dispatch [:study/deactivate! id])
      (rf/dispatch [:study-overview/show-activation-modal id]))))


(defn handleLinkClick [row]
  (let [id (:id (->clj row))]
    (rf/dispatch [:study-overview/show-link-modal id])))

(defn handleDownloadClick [row]
  (let [id (:id (->clj row))]
    (rf/dispatch [:study/download! id])))

(defn handleDeleteClick [row]
  (let [id (:id (->clj row))]
    (rf/dispatch [:study-overview/show-deletion-modal id])))

(defn table []
  (letfn [(group-button [el on-click disabled?]
            (rc/create-element
             "p"
             #js{:className "control"}
             [(rc/create-element
               "button"
               #js{:className "button"
                   :disabled disabled?
                   :onClick on-click}
               [el])]))]
    [:div
     [:> DataTable
      {:columns [{:name "Name"
                  :selector :topic
                  :sortable true
                  :ignoreRowClick true}
                 {:name "Description"
                  :selector :description
                  :sortable true
                  :ignoreRowClick true}
                 {:name "Created at"
                  :selector :createdAt
                  :sortable true
                  :maxWidth "175px"
                  :ignoreRowClick true}
                 {:name "Actions"
                  :grow false
                  :cell (fn [row]
                          (let [active? (:active? (->clj row))]
                              [(rc/create-element
                                "div"
                                #js{:className "field has-addons"}
                                [(group-button (rc/create-element "span"
                                                                  #js{:className "material-icons"
                                                                      :data-tooltip "Edit study"}
                                                                  "create")
                                               (fn [e] (handleEditClick row))
                                               false)
                                 (group-button
                                  (rc/create-element "span"
                                                     #js{:className "material-icons"
                                                         :data-tooltip "Download study data"}
                                                     "cloud_download")
                                  (fn [e] (when active? (handleDownloadClick row)))
                                  (not active?))
                                 (group-button
                                  (rc/create-element "span"
                                                     #js{:className "material-icons"
                                                         :data-tooltip "Get study link"}
                                                     "link")
                                  (fn [e] (when active? (handleLinkClick row)))
                                  (not active?))
                                 (group-button
                                  (rc/create-element "span"
                                                     #js{:className "material-icons"
                                                         :data-tooltip "Delete study"}
                                                     "delete_forever")
                                  (fn [e] (when active? (handleDeleteClick row)))
                                  (not active?))])]))}]

       :theme "solarized"
       :data (map #(update-in % [:createdAt] (fn [i] (.toDateString i)))
                  (let [studies (rf/subscribe [:user/studies])]
                    (or @studies [])))}]]))


(defn title []
  [:h3.title.is-4 "Study Overview"])


(defn study-overview-page []
  [:section.section>div.container
   (title)
   (table)
   (buttons)
   (link-modal)
   (csv-link-modal)
   (delete-modal)])
