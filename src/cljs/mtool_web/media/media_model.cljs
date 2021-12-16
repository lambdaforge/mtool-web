(ns mtool-web.media.media-model
  (:require [re-frame.core :as rf]))

(defn media [media-type]
  (when-let [media (cond
                     (= :image media-type) (rf/subscribe [:user/images])
                     (= :audio media-type) (rf/subscribe [:user/audio])
                     (= :video media-type) (rf/subscribe [:user/videos]))]
    @media))
