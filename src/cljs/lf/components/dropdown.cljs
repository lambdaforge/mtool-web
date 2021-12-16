
(ns lf.components.dropdown
  (:require [taoensso.timbre :as log]
            [mtool-web.view-helper :as h]))



;; TODO: move to a lf namespace
(defn toggle-dropdown
  [id]
  (let [classList (.-classList (.getElementById js/document id))
        open? (contains? (set classList) "is-active")]
    (if open?
      (.remove classList "is-active")
      (.add classList "is-active"))))

(defn dropdown
  "A generic dropdown implementation.
  'id' is an id used for the dropdown css id.
  'value-sub-fn' is the subrscription function that returns what the dropdown selected item should be.
  'items' are the dropdown items."
  [id selected-value items]
  [:div.dropdown {:id id}
   [:div.dropdown-trigger>button.button
    {:aria-haspopup "true", :aria-controls "dropdown-menu"
     :on-click #(do
                  ;; Stops click event propagation so that the global click event handler
                  ;; set to close open dropdowns is not triggered.
                  (.stopPropagation %)
                  (if (empty? items)
                    (log/warn "---- empty dropdown")
                    (toggle-dropdown id)))}
    [:span selected-value]
    [:i.material-icons "keyboard_arrow_down"]]
   [:div.dropdown-menu
    {:id (str "dropdown-menu-" id) :role "menu"}
    items]])


(defn close-dropdowns
  "Closes all open dropdowns"
  []
  (doall
   (map
    #(.remove (.-classList %) "is-active")
    (.getElementsByClassName js/document "dropdown"))))
