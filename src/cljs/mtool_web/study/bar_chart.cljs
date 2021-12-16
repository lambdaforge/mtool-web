(ns mtool-web.study.bar-chart
  (:require [lf.components.dropdown :as d]
            [mtool-web.view-helper :as vh]
            [mtool-web.study.bar-chart-events]
            [re-frame.core :as rf]
            [mtool-web.study.study-model :as sm]))

(defn icon [id]
  (vh/field
   "Icon"
   (d/dropdown id
               (let [v @(rf/subscribe [:bar-chart/icon-dropdown-value])
                     default-media sm/bar-chart-image]
                 (if (or (= nil v) (= default-media v))
                   "Default"
                   (vh/link-to-name v)))
               (concat [^{:key "default-bar-chart-icon"}
                         [:div.dropdown-content>a.dropdown-item
                          {:on-click #(rf/dispatch [:bar-chart/icon-dropdown-modified sm/bar-chart-image])}
                          "Default"]]
                        (doall
                         (map (fn [{:keys [filename name link]}]
                                ^{:key filename} [:div.dropdown-content>a.dropdown-item
                                                  {:on-click #(rf/dispatch [:bar-chart/icon-dropdown-modified link])}
                                                  name])
                              (mtool-web.media.media-model/media :image)))))
   nil))


(defn field
  "Id is a keyword representing the field."
  ([id label type]
   (field id label type {}))
  ([id label type opts]
   (let [value @(rf/subscribe [:bar-chart/field id])
         show-warn @(rf/subscribe [:common/show-warning])
         warn (and show-warn (empty? value))]
     (vh/req-field
      label
      [:div.field>div.control>input.input
       (merge
        {:type type
         :class (when warn "is-danger")
         :on-change #(let [v (-> % .-target .-value)]
                       (rf/dispatch [:bar-chart/field id (if (= "number" type)
                                                           (js/parseInt v)
                                                           v)]))
         :value value}
        opts)]
      "Required."
      warn))))


(defn bar-chart [id]
  (when @(rf/subscribe [:bar-chart/instance])
    [:div.tile.is-ancestor>div.tile.is-vertical
     [:div.tile.is-parent.is-vertical>article.tile.is-child.notification
      [:h3.title.is-5 "Bar Chart"]
      (vh/info-box "Please be aware that the size of the icon adapts to the number of increments. That is, many increments will result in very small icons in the bar chart.")
      (icon id)
      (field :title "Title" "text")
      (field :xTitle "X-Axis Title" "text")
      (field :xStart "X-Axis Start" "number")
      (field :xEnd "X-Axis End" "number")
      (field :xStepSize "X-Axis Increment" "number" {:min "0"})
      (field :yTitle "Y-Axis Title" "text")
      (field :yStart "Y-Axis Start" "number")
      (field :yEnd "Y-Axis End" "number")
      (field :yStepSize "Y-Axis Increment" "number" {:min "0"})
      ]]))
