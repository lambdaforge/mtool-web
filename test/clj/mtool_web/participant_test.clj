(ns mtool-web.participant-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.java.io :as io]
   [mtool-web.db.study :as db]
   [mtool-web.db.media :as mdb]
   [mtool-web.handler :refer [app]]
   [mtool-web.test-util :as u]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [ring.mock.request :refer [request header json-body]])
  (:import [java.util UUID]))

(use-fixtures
 :once
 (fn [f]
   (mount/start #'mtool-web.config/env
                #'mtool-web.handler/app-routes
                #'lf.administration.db/conn)
   (f)))

(deftest request-participant-page
  (let [{user-id :id} (u/get-user)
        study-id (db/add-study user-id u/test-study (.toString (UUID/randomUUID)) true)]
    (testing "Non-existing study"
      (let [non-existing-study-id "1234-5678-9012"
            response ((app) (request :get (str "/studies/" non-existing-study-id)))]
        (is (= 404 (:status response)))
        (is (= "No study with given ID!" (:error response)))))

    (testing "Active study"
             (let [response ((app) (request :get (str "/studies/" study-id)))]
               (is (= 200 (:status response)))
               (is (.startsWith (str (:body response)) "<!DOCTYPE HTML"))))

    (testing "Inactive study"
      (db/deactivate-study study-id)
      (let [response ((app) (request :get (str "/studies/" study-id)))]
        (is (= 404 (:status response)))
        (is (= "Study has not been activated!" (:error response)))))))

(deftest upload-session-data
  (let [{user-id :id} (u/get-user)
        study-id (db/add-study user-id u/test-study)
        uuid (.toString (UUID/randomUUID))
        session {:barChartResult "a csv string"
                 :duration 105.0
                 :id uuid
                 :mapping1Result "a csv string"
                 :mapping2Result "a csv string"
                 :startedAt (read-string "#inst\"2021-02-26T22:24:15Z\"")}
        post-data (fn [key val] (-> (request :post "/api/session-data")
                                    (json-body {:study-id study-id
                                                :session {:id uuid
                                                          key val}})
                                    ((app))))]
    (testing "start date"
             (let [response (post-data :startedAt (:startedAt session))]
               (is (= 200 (:status response)))
               (is (= "Session data has been uploaded successfully!" (:message (m/decode-response-body response)))))             )
    (testing "mapping1 result"
             (let [response (post-data :mapping1Result (:mapping1Result session))]
               (is (= 200 (:status response)))
               (is (= "Session data has been uploaded successfully!" (:message (m/decode-response-body response))))))
    (testing "mapping2 result"
             (let [response (post-data :mapping2Result (:mapping2Result session))]
               (is (= 200 (:status response)))
               (is (= "Session data has been uploaded successfully!" (:message (m/decode-response-body response))))))
    (testing "bar chart result"
             (let [response (post-data :barChartResult (:barChartResult session))]
               (is (= 200 (:status response)))
               (is (= "Session data has been uploaded successfully!" (:message (m/decode-response-body response))))))
    (testing "duration"
             (let [response (post-data :duration (:duration session))]
               (is (= 200 (:status response)))
               (is (= "Session data has been uploaded successfully!" (:message (m/decode-response-body response))))))
    (testing "duration as integer"
             (let [response (post-data :duration (int (:duration session)))]
               (is (= 200 (:status response)))
               (is (= "Session data has been uploaded successfully!" (:message (m/decode-response-body response))))))
    (testing "stored complete result"
             (is (= session (first (db/get-session-data study-id)))))))

(deftest download-settings
  (testing "Uploaded files are still reachable"
           (let [{user-id :id} (u/get-user)
                 filename "Instructions.mp4"
                 {:keys [link media-id]} (u/insert-file (str "public/participant/video/" filename) user-id)
                 test-study (assoc-in u/test-study [:introduction :video :link] link)
                 study-id (db/add-study user-id test-study (.toString (java.util.UUID/randomUUID)) true)
                 response  (-> (request :get (str "/api/session-data?study-id=" study-id))
                              (header :accept "application/json")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (mdb/media-exists? media-id))
             (is (.exists (io/file (mdb/get-media-location media-id))))
             (is (= 200 (:status response)))
             (is (= link (:introductionVideo (:session-settings body))))
             (is (= 200 (:status ((app) (request :get link))))))))


(comment       ;; test app
 (user/start)

 (let [{user-id :id} (u/get-user)
       study-id (db/add-study user-id u/test-study)]
   (db/activate-study study-id)
   (println (str "link: " (u/get-study-link study-id)))
   ; (db/deactivate-study study-id)
   ))


