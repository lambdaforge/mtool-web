(ns mtool-web.study.mapping
  (:require [lf.components.dropdown :as d]
            [mtool-web.media.media-model :refer [media]]
            [mtool-web.study.mapping-events :as me]
            mtool-web.study.study-events
            [mtool-web.view-helper :as vh]
            [re-frame.core :as rf]
            [mtool-web.study.study-model :as sm]))

(defn buttons
  [mapping-num]
  [:section.section>div.level
   [:div.level-item>div.field.is-grouped
    [:p.control>div.button.is-small.is-primary
     {:on-click #(rf/dispatch [:mapping/new-icon mapping-num])}
     "Add icon"]]])


(defn title [name]
  [:h3.title.is-5 name])


(defn checkbox
  [mapping-num icon-num]
  [:label.checkbox.field>input
   {:type "checkbox"
    :checked (when-let [checked? (rf/subscribe [:mapping/icon-checkbox mapping-num icon-num])] @checked?)
    :on-change #(rf/dispatch [:mapping/icon-checkbox-checked mapping-num icon-num (-> % .-target .-checked)])}])

(defn icon-dropdown
  [mapping-num icon-num default-media media-type]
  (d/dropdown
   (str mapping-num "-" icon-num "-" media-type)
   (let [v @(rf/subscribe [:mapping/icon-dropdown-value mapping-num icon-num media-type])]
     (if (= nil v)
       "None"
       (vh/link-to-name v)))
   (concat
    (if (= :audio media-type)
      [^{:key "NO_AUDIO"}
       [:div.dropdown-content>a.dropdown-item
        {:on-click #(rf/dispatch [:mapping/icon-dropdown-modified
                                  mapping-num icon-num media-type nil])}
        "None"]]
      [])
    (doall (map (fn [{:keys [name filename link]}]
                  ^{:key filename}
                  [:div.dropdown-content>a.dropdown-item
                   {:on-click #(rf/dispatch [:mapping/icon-dropdown-modified
                                             mapping-num icon-num media-type link])}
                   name])
                (media media-type))))))

(defn icon-row
  [mapping-num icon-num]
  ^{:key icon-num} [:div.columns
                    [:div.column.is-1 (checkbox mapping-num icon-num)]
                    [:div.column.is-1]
                    [:div.column.is-3 (icon-dropdown mapping-num icon-num sm/icon-image :image)]
                    [:div.column.is-1]
                    [:div.column.is-3 (icon-dropdown mapping-num icon-num sm/icon-audio :audio)]
                    [:div.column.is-1 [:a
                                       {:on-click #(rf/dispatch [:mapping/delete-icon mapping-num icon-num])}
                                       [:i.material-icons "highlight_off"]]]])


(defn icon-table-title []
  [:div.columns
   [:div.column.is-1>label.label "Use?"]
   [:div.column.is-1>label.label]
   [:div.column.is-3>label.label "Image file"]
   [:div.column.is-1>label.label]
   [:div.column.is-3>label.label "Audio explanation (optional)"]])


(defn icons-table [mapping-num]
  [:div
   (icon-table-title)
   (when-let [icons (rf/subscribe [:mapping/icons mapping-num])]
     (doall
      (map-indexed (fn [icon-num _] ^{:key icon-num} (icon-row mapping-num icon-num)) @icons)))])

(defn fixed-icon-dropdown
  [mapping-num]
  (d/dropdown (str "fixed-icon-" mapping-num)
              (let [v @(rf/subscribe [:mapping/fixed-icon-dropdown-value mapping-num])]
                (if (empty? v)
                  "None"
                  (if (= v sm/fixed-icon-image)
                    "Y"
                    (vh/link-to-name v))))
              (concat [^{:key "No-fixed-icon"} [:div.dropdown-content>a.dropdown-item
                                                {:on-click #(do
                                                              (rf/dispatch [:mapping/fixed-icon-dropdown-modified mapping-num ""]))}
                                                "None"]
                       ^{:key "default-fixed-icon"} [:div.dropdown-content>a.dropdown-item
                                                     {:on-click #(do
                                                                   (rf/dispatch [:mapping/fixed-icon-dropdown-modified mapping-num sm/fixed-icon-image]))}
                                                     "Y"]]
                      (doall
                       (->> (media :image)
                            (map (fn [{:keys [name filename link]}]
                                   ^{:key filename} [:div.dropdown-content>a.dropdown-item
                                                     {:on-click #(rf/dispatch [:mapping/fixed-icon-dropdown-modified
                                                                               mapping-num link])}
                                                     name])))))))

(defn fixed-icon-field
  [mapping-num]
  (vh/field "Fixed icon?"
            [:div.field>div.control
             (fixed-icon-dropdown mapping-num)]
            false))


(defn fixed-icon-position-dropdown
  [mapping-num]
  (d/dropdown (str "fixe-icon-pos-" mapping-num)
              @(rf/subscribe [:mapping/fixed-icon-position-dropdown-value mapping-num])
              (doall
               (map (fn [position]
                      ^{:key position} [:div.dropdown-content>a.dropdown-item
                                        {:on-click #(rf/dispatch [:mapping/fixed-icon-position-dropdown-modified
                                                                  mapping-num position])}
                                        position])
                    [:left :center :right]))))



(defn fixed-icon-position-field
  [mapping-num]
  (vh/field "Fixed icon position?"
            [:div.field>div.control
             (fixed-icon-position-dropdown mapping-num)]
            false))

(defn mapping
  "name is the title of this mappins section.
  mapping-num is the number identifying this mapping."
  [name mapping-num]
  [:div
   (title name)
   (fixed-icon-field mapping-num)
   (fixed-icon-position-field mapping-num)
   (icons-table mapping-num)
   (buttons mapping-num)])
