(ns mtool-web.participant
  (:require [mtool-web.config :as c :refer [env]]
            [mtool-web.db.study :as db]
            [clojure.string :as string]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as s]))

(def csv-separator ",")

(defn result-data->csv [{:keys [id mapping1Result mapping2Result barChartResult startedAt duration]}]
  (let [result-type->csv (fn [data-str]
                           (let [result (map #(string/split % #",") (string/split data-str #"\n"))
                                 meta-data (into {} (take 3 result))
                                 data (map (fn [[r1 r2 r3]]
                                             (let [mapping-type (read-string (get meta-data "Mapping Type"))]
                                               [id
                                                startedAt
                                                duration
                                                mapping-type
                                                (read-string (get meta-data "Start"))
                                                (read-string (get meta-data "Duration"))
                                                (if (= mapping-type "barChart")
                                                  ""
                                                  r1)
                                                (if (= mapping-type "barChart")
                                                  ""
                                                  r2)
                                                (if (= mapping-type "barChart")
                                                  ""
                                                  r3)
                                                (if (= mapping-type "barChart")
                                                  r1
                                                  "")
                                                (if (= mapping-type "barChart")
                                                  r2
                                                  "")]))
                                           (drop 4 result))]
                             data))]
    (vec (concat []
                 (when (and (nil? mapping1Result)
                            (nil? mapping2Result)
                            (nil? barChartResult))
                   [[id startedAt "" "" "" "" "" "" "" "" ""]])
                 (when mapping1Result
                   (result-type->csv mapping1Result))
                 (when mapping2Result
                   (result-type->csv mapping2Result))
                 (when barChartResult
                   (result-type->csv barChartResult))))))

(defn create-study-csv [study-id file-name]
  (let [{:keys [barChart]} (db/get-study study-id)
        header ["User_ID"	"Total_Start"	"Total_Duration" "Type"	"Start"	"Duration"	"From"	"To"	"Weight" (:xTitle barChart "X") (:yTitle barChart "Y")]
        result (vec (mapcat result-data->csv (db/get-session-data study-id)))]
    (with-open [writer (io/writer file-name)]
      (csv/write-csv writer (vec (concat [header] result))))))

(comment

  (def old-csv
    (->> "33b84820-3e6f-466b-9cec-a8d757691fee.csv"
         slurp))

  (let [header ["User_ID" "Total_Start" "Total_Duration" "Type" "Start" "Duration" "From" "To" "Weight" "Database Update"]
        result (->> (string/split old-csv #"\n\n")
                    (partition 2 2 nil)
                    (map (fn [[start end]] (str end "\n" start)))
                    (map (fn [s] (string/split s #"Connections")))
                    (map (fn [[overview connections]]
                           [(string/split overview #"\n")
                            (when-not (string/blank? connections)
                              (string/split connections #"\n"))]))
                    (map (fn [[overview connections]]
                           [(map #(string/split % #",") overview)
                            (map #(string/split % #",") (rest connections))]))
                    (map (fn [[overview connections]]
                           [overview
                            connections]))
                    (mapcat (fn [[[[_ db-update]
                                   _
                                   [_ user-id]
                                   [_ total-start]
                                   [_ total-duration]
                                   _
                                   [_ type]
                                   [_ start]
                                   [_ duration]] connections]]
                              (map (fn [[from to weight]]
                                     [user-id
                                      total-start
                                      total-duration
                                      (read-string type)
                                      (read-string start)
                                      (read-string duration)
                                      from
                                      to
                                      weight
                                      db-update
                                      ]) connections)))
                    (into [header]))]
    (with-open [writer (io/writer "33b84820-3e6f-466b-9cec-a8d757691fee-new.csv")]
      (csv/write-csv writer result)))

  )

(defn get-session-results-file [study-id user-id]
  (let [relative-path (str user-id "/results/mtool_" study-id ".csv")
        path (str c/user-media-dir "/" relative-path)
        link (str (:base-url env) "/" relative-path)]
    (io/make-parents path)
    (create-study-csv study-id path)
    link))

(defn adjust-settings [study]
  (let [icon-map (fn [mapping-type]
                   (let [icons (filter :use? (-> study mapping-type :icons))]
                     (zipmap (map :name icons)
                             (map (fn [{:keys [image audio]}] {:image (:link image) :audio (:link audio)})
                                  icons))))]
    {;; fixed settings
     :solutionImage         "/participant/images/solution.png"
     :practiceSolutionArray [["food" "smile" "3"]
                             ["work" "money" "2"]
                             ["money" "smile" "2"]
                             ["sleep" "smile" "1"]]

     :audioBlock            true
     :arrowWeights          [3 2 1 0 -1 -2 -3]
     :arrowColor            {:positive "black" :negative "black" :neutral "black"}
     :highlightColor        "lightskyblue"
     :customFactorNumber    0

     :uploadRoute           (str (:base-url env) "/api/session-data")
     :separator             csv-separator

     :buttonImages          {:bin      "/participant/buttons/bin.png"
                             :next     "/participant/buttons/next.png"
                             :previous "/participant/buttons/previous.png"
                             :question "/participant/buttons/question.png"}

     ;; Changeable settings
     :useConsent            (or (-> study :consentText some?)
                                (-> study :consentLink some?))

     :useSurvey             (-> study :survey some?)

     :usePositiveArrows     (-> study :positiveArrows?)
     :useNegativeArrows     (-> study :negativeArrows?)
     :useDoubleHeadedArrows (-> study :doubleHeadedArrows?)

     :useMapping1           (boolean (:mapping1 study))
     :useMapping2           (boolean (:mapping2 study))
     :useBarChart           (boolean (:barChart study))

     :barChart              {:title     (-> study :barChart :title)
                             :xTitle    (-> study :barChart :xTitle)
                             :xStart    (-> study :barChart :xStart)
                             :xEnd      (-> study :barChart :xEnd)
                             :xStepSize (-> study :barChart :xStepSize)
                             :yStepSize (-> study :barChart :yStepSize)
                             :yTitle    (-> study :barChart :yTitle)
                             :yStart    (-> study :barChart :yStart)
                             :yEnd      (-> study :barChart :yEnd)
                             :image     (-> study :barChart :image :link)
                             :audio     (-> study :barChart :audio :link)
                             :video     (-> study :barChart :video :link)}

     :mapping1              {:audio             (-> study :mapping1 :audio :link)
                             :video             (-> study :mapping1 :video :link)
                             :icons             (icon-map :mapping1)
                             :fixedIcon         (-> study :mapping1 :fixedIcon :image :link)
                             :fixedIconName     (-> study :mapping1 :fixedIcon :image :name)
                             :fixedIconPosition (-> study :mapping1 :fixedIcon :position)}

     :mapping2              {:audio             (-> study :mapping2 :audio :link)
                             :video             (-> study :mapping2 :video :link)
                             :icons             (icon-map :mapping2)
                             :fixedIcon         (-> study :mapping2 :fixedIcon :image :link)
                             :fixedIconName     (-> study :mapping2 :fixedIcon :image :name)
                             :fixedIconPosition (-> study :mapping2 :fixedIcon :position)}

     :practiceMapping       {:icons             {:work    {:name "work" :image "/participant/images/work.png" :audio "/participant/audio/work.m4a"}
                                                 :food    {:name "food" :image "/participant/images/food.png" :audio "/participant/audio/food.m4a"}
                                                 :money   {:name "money" :image "/participant/images/money.png" :audio "/participant/audio/money.m4a"}
                                                 :sleep   {:name "sleep" :image "/participant/images/sleep.png" :audio "/participant/audio/sleep.m4a"}
                                                 :friends {:name "friends" :image "/participant/images/friends.png" :audio "/participant/audio/friends.m4a"}}
                             :fixedIcon         "/participant/images/smile.png"
                             :fixedIconPosition :right
                             :audio             (-> study :practice :audio :link)}

     :introductionVideo     (-> study :introduction :video :link)
     :thankYouAudio         (-> study :thankYou :audio :link)
     :welcomeAudio          (-> study :introduction :audio :link)}))

