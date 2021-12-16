(ns mtool-web.study.study-model)

(def mapping-audio "/participant/audio/mapping.m4a")
(def mapping-video "/participant/video/Instructions.mp4")
(def fixed-icon-image "/participant/images/Y.png")
(def icon-image "/participant/images/A.png")
(def icon-audio "/participant/audio/A.m4a")
(def bar-chart-audio "/participant/audio/Audio for bar chart page.m4a")
(def bar-chart-video "/participant/video/Bar chart tutorial video.mp4")
(def bar-chart-image "/participant/images/barchart_icon.png")

(def introduction-video "/participant/video/MM Practice task video.mp4")
(def introduction-audio "/participant/audio/Welcome audio.m4a")
(def practice-audio "/participant/audio/practice.m4a")
(def thank-you-audio "/participant/audio/thank_you.m4a")

(def default-mapping-icons (mapv
                            (fn [l]
                              {:name l
                               :image {:link (str "/participant/images/" l ".png")}
                               :audio {:link (str "/participant/audio/" l ".m4a")}
                               :use? true})
                            ["A" "B" "C" "D" "E" "F" "G" "H" "I" "J" "K" "L" "M" "N" "O" "Y"]))

(defn new-icon []
  {:name "A"
   :image {:link icon-image}
   :audio {:link icon-audio}
   :use? true})

(defn new-mapping []
  {:audio {:link mapping-audio}
   :video {:link mapping-video}
   :fixedIcon {:image {:link fixed-icon-image}
               :position :right}
   :icons default-mapping-icons})

(defn new-bar-chart []
  {:audio {:link  bar-chart-audio}
   :video {:link  bar-chart-video}
   :image {:link  bar-chart-image}
   :title nil
   :xTitle nil
   :xStart nil
   :xEnd nil
   :xStepSize nil
   :yTitle nil
   :yStart nil
   :yEnd nil})

(def new-study
  {:topic nil
   :description nil
   :survey nil
   :consentText nil
   :consentLink nil
   :mapping1 (new-mapping)
   :mapping2 (new-mapping)
   :barChart (new-bar-chart)
   :positiveArrows? false
   :negativeArrows? false
   :doubleHeadedArrows? false
   :introduction {:video {:link introduction-video}
                  :audio {:link introduction-audio}
                  :message nil}
   :practice {:audio {:link practice-audio}}
   :thankYou {:audio {:link thank-you-audio}
              :message nil}})


(defn introduction-video-path []
  [:study/current :introduction :video :link])

(defn introduction-audio-path []
  [:study/current :introduction :audio :link])

(defn second-icon-video-path []
  [:study/current :mapping2 :video :link])

(defn bar-chart-video-path []
  [:study/current :barChart :video :link])

(defn practice-audio-path []
  [:study/current :practice :audio :link])

(defn mapping-1-audio-path []
  [:study/current :mapping1 :audio :link])

(defn mapping-2-audio-path []
  [:study/current :mapping2 :audio :link])

(defn bar-chart-audio-path []
  [:study/current :barChart :audio :link])

(defn thanks-audio-path []
  [:study/current :thankYou :audio :link])

(defn thanks-msg-path []
  [:study/current :thankYou :message])

(defn welcome-msg-path []
  [:study/current :introduction :message])

(defn mapping-keyword [mapping-num]
  (keyword (str "mapping" mapping-num)))

(defn mapping-path [mapping-num]
  [:study/current (mapping-keyword mapping-num)])

(defn icons-path [mapping-num]
  (conj (mapping-path mapping-num) :icons))

(defn fixed-icon-path [mapping-num]
  (into (mapping-path mapping-num) [:fixedIcon :image :link]))

(defn fixed-icon-position-path [mapping-num]
  (into (mapping-path mapping-num) [:fixedIcon :position]))

(defn delete-icon [mapping-num icon-num db]
  (let [icons (get-in db (icons-path mapping-num))
        icons' (vec (concat (subvec icons 0 icon-num) (subvec icons (inc icon-num))))]
    (assoc-in db (icons-path mapping-num)  icons')))

(defn get-study [db id]
  (first (filter #(= id (:id %)) (:user/studies db))))

(defn icon-explanation-video-path []
  [:study/current :mapping1 :video :link])
