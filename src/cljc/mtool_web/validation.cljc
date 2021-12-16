(ns mtool-web.validation
  (:require [clojure.spec.alpha :as s]))


;; General
(s/def :s/id string?)
(s/def :s/date inst?)
(s/def :s/type keyword?)
(s/def :s/link string?)
(s/def :s/name string?)

;; Media
(s/def :s/media-type #{:image :video :audio})
(s/def :s/filename string?)
(s/def :s/note string?)
(s/def :s/addedAt :s/date)
(s/def :s/media-entry (s/keys :req-un [:s/id :s/type :s/note :s/filename :s/link :s/addedAt :s/name]))
(s/def :s/media-list (s/coll-of :s/media-entry))

;; Studies
(s/def :s/media (s/keys :req-un [:s/link]))
(s/def :s/topic string?)
(s/def :s/consentText string?)
(s/def :s/consentLink string?)
(s/def :s/survey string?)
(s/def :s/description string?)
(s/def :s/createdAt :s/date)
(s/def :s/image :s/media)
(s/def :s/audio :s/media)
(s/def :s/video :s/media)
(s/def :s/use? boolean?)
(s/def :s/icon (s/keys :req-un [:s/name :s/image :s/use?]
                       :opt-un [:s/audio]))
(s/def :s/icons (s/coll-of :s/icon))
(s/def :s/position #{:left :center :right})
(s/def :s/fixedIcon (s/keys :req-un [:s/image :s/position]))
(s/def :s/mapping (s/keys :req-un [:s/video :s/audio :s/icons :s/fixedIcon]))
(s/def :s/mapping1 (s/or :none nil? :mapping :s/mapping))
(s/def :s/mapping2 (s/or :none nil? :mapping :s/mapping))
(s/def :s/title string?)
(s/def :s/xTitle string?)
(s/def :s/xStart int?)
(s/def :s/xEnd int?)
(s/def :s/xStepSize int?)
(s/def :s/yStepSize int?)
(s/def :s/yTitle string?)
(s/def :s/yStart int?)
(s/def :s/yEnd int?)
(s/def :s/barChart (s/or :none nil? :barChart (s/keys :req-un [:s/image :s/audio :s/video :s/title
                                                               :s/xTitle :s/xStart :s/xEnd :s/xStepSize :s/yStepSize
                                                               :s/yTitle :s/yStart :s/yEnd])))
(s/def :s/positiveArrows? boolean?)
(s/def :s/negativeArrows? boolean?)
(s/def :s/doubleHeadedArrows? boolean?)
(s/def :s/message string?)
(s/def :s/introduction (s/keys :req-un [:s/video :s/audio :s/message]))
(s/def :s/practice (s/keys :req-un [:s/audio]))
(s/def :s/thankYou (s/keys :req-un [:s/audio :s/message]))
(s/def :s/study-settings (s/keys :req-un [:s/topic :s/positiveArrows? :s/negativeArrows? :s/doubleHeadedArrows? :s/introduction :s/practice :s/thankYou ]
                                 :opt-un [:s/mapping1 :s/description :s/mapping2 :s/barChart :s/createdAt :s/link :s/consentText :s/consentLink :s/survey]) )
(s/def :s/studies (s/coll-of :s/study-settings))

;; Session
(s/def :s/mapping1Result string?)
(s/def :s/mapping2Result string?)
(s/def :s/barChartResult string?)
(s/def :s/startedAt inst?)
(s/def :s/duration number?)
(s/def :s/session (s/keys :req-un [:s/id]
                          :opt-un [:s/mapping1Result :s/mapping2Result :s/barChartResult :s/startedAt :s/duration]))
(s/def :s/sessions (s/coll-of :s/session))
