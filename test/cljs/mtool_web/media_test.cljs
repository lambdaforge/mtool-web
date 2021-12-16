(ns mtool-web.media-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [re-frame.core :as rf]
            [day8.re-frame.test :as rf-test]
            [mtool-web.media.overview-events :as oe]
            [mtool-web.media.upload-events :as ue]))

(deftest upload-file
  (rf-test/run-test-sync
   (let [db {}
         file "fake-file"
         res (ue/upload-file db [nil file])]
     (is (= file (:upload/file res))))))


(deftest upload-note
  (rf-test/run-test-sync
   (let [db {}
         note "a note"
         res (ue/upload-note db [nil note])]
     (is (= note (:upload/note res))))))



(deftest upload-type
  (rf-test/run-test-sync
   (let [db {}
         type "a type"
         res (ue/upload-type db [nil type])]
     (is (= type (:upload/type res))))))


(deftest load-user-media
  (rf-test/run-test-sync
   (let [db {}
         res (oe/get-media-failure db nil)]
     (is (= nil (:user/media res))))))



(deftest retrieve-media-by-type
  (rf-test/run-test-sync
    (let [v (rf/subscribe [:user/videos])
          a (rf/subscribe [:user/audio])
          i (rf/subscribe [:user/images])]
     (rf/dispatch-sync [:mtool-web.media.overview-events/get-all-success
                        {:media [{:type :video :filename "video"}
                                 {:type :audio :filename "audio"}
                                 {:type :image :filename "image"}]}])
     (is (= :video (:type (first @v))))
     (is (= :audio (:type (first @a))))
     (is (= :image (:type (first @i)))))))
