(ns lf.helper)

(defn set-error
  [db msg]
  (assoc db :common/error msg))

(defn set-message
  [db msg]
  (assoc db :common/message msg))


(defn toggle-component
  "Toggle a bulma component by setting/removing its 'is-active' class.
  'id' is the component css id."
  [id]
  (let [classList (.-classList (.getElementById js/document id))
        open? (contains? (set classList) "is-active")]
    (if open?
      (.remove classList "is-active")
      (.add classList "is-active"))))
