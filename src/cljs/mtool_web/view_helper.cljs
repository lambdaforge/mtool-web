(ns mtool-web.view-helper
  (:require [mtool-web.media.media-model :as m]
            [mtool-web.study.study-model :as model]
            [re-frame.core :as rf]))

(defn field
  "Creates a form field with label 'name' and control 'component'"
  ([name component]
   (field name component "Required" false))
  ([name component help]
   (field name component help false))
  ([name component help warning]
   [:div.columns
    [:div.column.is-3>label.label name]
    [:div.column.is-9>div.control
     component
     [:p.help {:class (when warning "is-danger")} help]]]))

(defn link-to-name
  "Given a media link, returns the associated name."
  [link]
  (let [media @(rf/subscribe [:user/media])
        default-mapping-media (vec (mapcat (fn [{:keys [name image audio]}]
                                             [{:name name :link (:link image) :type :image}
                                              {:name name :link (:link audio) :type :audio}])
                                           model/default-mapping-icons))]
    (:name (first (filter #(= link (:link %)) (concat media default-mapping-media))))))

(defn req-field [name component help danger]
  [:div.columns
   [:div.column.is-3>label.label name]
   [:div.column.is-9>div.control
    component
    [:p.help {:class (when danger "is-danger")} help]]])

(defn req-text-field
  ([name db-id]
   (req-text-field name db-id "Required."))
  ([name db-id help]
   (let [v @(rf/subscribe [db-id])
         show-warn @(rf/subscribe [:common/show-warning])
         warn (and show-warn (empty? v))]
     (req-field
      name
      [:div.field>div.control>input.input
       {:type "text"
        :class (when warn "is-danger")
        :on-change #(rf/dispatch [db-id (-> % .-target .-value)])
        :value v}]
      help
      warn))))

(defn req-area-field
  ([name db-id]
   (req-area-field name db-id "Required."))
  ([name db-id help]
   (let [v @(rf/subscribe [db-id])
         show-warn @(rf/subscribe [:common/show-warning])
         warn (and show-warn (empty? v))]
     (req-field
      name
      [:textarea.textarea
       {:class (when warn "is-danger")
        :on-change #(rf/dispatch [db-id (-> % .-target .-value)])
        :value v}]
      help
      warn))))

(defn info-box [text]
  [:div.notification.is-info text])
