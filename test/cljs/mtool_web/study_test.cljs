(ns mtool-web.study-test
  (:require [cljs.test :refer-macros [are deftest is testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [mtool-web.study.mapping-events :as me]
            [mtool-web.study.study-events :as se]
            [mtool-web.study.study-model :as sm]
            [mtool-web.study.bar-chart-events :as sbe]
            [clojure.spec.alpha :as s]
            [re-frame.core :as rf]))

(deftest introduction
  (rf-test/run-test-sync
    (let [i (rf/subscribe [:video-audio/dropdown-value [:introduction :video :link]])]
      (rf/dispatch [:video-audio/dropdown-modified [:introduction :video :link] "a.jpg"])
      (is (= "a.jpg" @i)))))


(deftest mapping-presence
  (rf-test/run-test-sync
   (let [db {:study/current sm/new-study}
         mapping-num 2
         db' (me/toggle-mapping-presence db [nil  mapping-num])
         db'' (me/toggle-mapping-presence db' [nil  mapping-num])]
     ;; Assumes that we started with a study which contains a mapping!
     (is (= nil (me/mapping-instance db' mapping-num)))
     (is (= (sm/new-mapping) (me/mapping-instance db'' mapping-num))))))


(deftest bar-chart-presence
  (rf-test/run-test-sync
   (let [db {:study/current sm/new-study}
         db' (se/toggle-bar-chart-presence db )
         db'' (se/toggle-bar-chart-presence db')]
     ;; Assumes that we started with a study which contains a bar-chart!
     (is (= nil (se/bar-chart-instance db')))
     (is (= (sm/new-bar-chart) (se/bar-chart-instance db''))))))




(deftest setting-bar-chart-image
  (rf-test/run-test-sync
   (let [v (rf/subscribe [:bar-chart/icon-dropdown-value])]
     (rf/dispatch-sync [:bar-chart/icon-dropdown-modified "img.jpg"])
     (is (= "img.jpg" @v)))))



(deftest setting-bar-chart-field
  (rf-test/run-test-sync
   (let [v (rf/subscribe [:bar-chart/field :xTitle])]
     (rf/dispatch-sync [:bar-chart/field :xTitle "bar"])
     (is (= "bar" @v)))))


(deftest video-audio-section-dropdown
  (rf-test/run-test-sync
    (let [db-path [:introductionVideo :link]
          v (rf/subscribe [:video-audio/dropdown-value db-path])]
     (rf/dispatch-sync [:video-audio/dropdown-modified db-path "hello.mp4" ])
     (is (= "hello.mp4" @v)))))


(deftest activation-checkox
  (rf-test/run-test-sync
   (let [v (rf/subscribe [:study/activation-checkbox])]
     (rf/dispatch-sync [:study/activation-checkbox false])
     (is (= false @v)))))


(deftest study-creation
  (rf-test/run-test-sync
   (let [s (rf/subscribe [:study/current])]
      ;; listen the current study and to s/validate
     (rf/dispatch-sync [:study/new])
     (rf/dispatch-sync [:study/name "a study"])
     (rf/dispatch-sync [:study/desc "desc"])
     (rf/dispatch-sync [:mapping/icon-name-changed 1 0 "a"])
     (rf/dispatch-sync [:mapping/icon-name-changed 2 0 "b"])
     (rf/dispatch-sync [:bar-chart/field :yEnd 1])
     (rf/dispatch-sync [:bar-chart/field :xStepSize 1])
     (rf/dispatch-sync [:bar-chart/field :title "btitle"])
     (rf/dispatch-sync [:bar-chart/field :xTitle "title"])
     (rf/dispatch-sync [:bar-chart/field :yTitle "ytitle"])
     (rf/dispatch-sync [:bar-chart/field :yStart 1])
     (rf/dispatch-sync [:bar-chart/field :xEnd 1])
     (rf/dispatch-sync [:bar-chart/field :xStart 1])
     (rf/dispatch-sync [:study/thankyou-msg "thank you"])
     (rf/dispatch-sync [:study/welcome-msg "welcome"])
     (is (s/valid? :s/study-settings @s))
     (s/explain :s/study-settings @s))))
