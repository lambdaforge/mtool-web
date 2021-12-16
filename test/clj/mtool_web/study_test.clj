(ns mtool-web.study-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [mtool-web.db.study :as db]
   [mtool-web.handler :refer [app]]
   [mtool-web.test-util :as u :refer [transit-body]]
   [mount.core :as mount]
   [muuntaja.core :as m]
   [ring.mock.request :refer [request header]])
  (:import [java.util UUID]))

(use-fixtures
 :once
 (fn [f]
   (mount/start #'mtool-web.config/env
                #'mtool-web.handler/app-routes
                #'lf.administration.db/conn)
   (f)))

(deftest add-study
  (let [{user-token :token} (u/get-user)
        response (-> (request :post "/api/studies")
                     (header :authorization (str "Token " user-token))
                     (transit-body {:study u/test-study})
                     ((app)))
        body (m/decode-response-body response)]
    (is (= 200 (:status response)))
    (is (= "Study has been added successfully!" (:message body)))
    (is (not (nil? (:study-id body))))))

(deftest edit-study
  (let [{user-token :token user-id :id} (u/get-user)
        mapping2 (:mapping1 u/test-study)]

    (testing "Edit active study"
      (let [test-study (assoc u/test-study :active? true)
            study-id (db/add-study user-id test-study)
            changed-study (assoc test-study :mapping2 mapping2)
            response (-> (request :post (str "/api/studies/" study-id))
                         (header :authorization (str "Token " user-token))
                         (transit-body {:study changed-study})
                         ((app)))
            body (m/decode-response-body response)]
        (is (= 200 (:status response)))
        (is (= "Study has been updated successfully!" (:message body)))
        (is (= true (db/study-active? study-id)))
        (is (= mapping2 (:mapping2 (db/get-study study-id))))))

    (testing "Edit inactive study"
      (let [test-study (assoc u/test-study :active? false)
            study-id (db/add-study user-id test-study)
            changed-study (assoc test-study :mapping2 mapping2)
            response (-> (request :post (str "/api/studies/" study-id))
                         (header :authorization (str "Token " user-token))
                         (transit-body {:study changed-study})
                         ((app)))
            body (m/decode-response-body response)]
        (is (= 200 (:status response)))
        (is (= "Study has been updated successfully!" (:message body)))
        (is (= false (db/study-active? study-id)))
        (is (= mapping2 (:mapping2 (db/get-study study-id))))))))

(deftest list-studies
  (let [{user-token :token user-id :id} (u/get-user)]

    (testing "no entries"
      (doseq [study-id (map :id (db/get-user-studies user-id))]
        (db/delete-study study-id))
      (let [response (-> (request :get "/api/studies")
                         (header :authorization (str "Token " user-token))
                         ((app)))]
        (is (= 200 (:status response)))
        (is (= (m/decode-response-body response)
               {:message "User studies have been fetched!"
                :studies []}))))

    (testing "2 entries"
      (doseq [study-id (map :id (db/get-user-studies user-id))]
        (db/delete-study study-id))
      (db/add-study user-id u/test-study)
      (db/add-study user-id u/test-study)
      (let [response (-> (request :get "/api/studies")
                         (header :authorization (str "Token " user-token))
                         ((app)))
            body (m/decode-response-body response)]
        (is (= 200 (:status response)))
        (is (= "User studies have been fetched!" (:message body)))
        (is (= 2 (count (:studies body))))
        (is (-> body :studies first keys)
            #{:id :topic :description :positiveArrows? :negativeArrows? :doubleHeadedArrows?
              :introduction :practice :thankYou})))))

(deftest study-activation
  (let [{user-token :token user-id :id} (u/get-user)
        study-id (db/add-study user-id u/test-study)
        _ (db/deactivate-study study-id)
        response (-> (request :put (str "/api/studies/" study-id "/activate"))
                     (header :authorization (str "Token " user-token))
                     ((app)))
        body (m/decode-response-body response)]
    (is (= 200 (:status response)))
    (is (= "Study has been successfully activated!" (:message body)))
    (is (db/study-active? study-id))))

(deftest study-deletion
  (let [{user-token :token user-id :id} (u/spawn-user "x.y@z.com" "gfegregregr")
        study-id1 (db/add-study user-id u/test-study)
        study-id2 (db/add-study user-id u/test-study)
        response (-> (request :delete (str "/api/accounts/" user-id))
                     (header :authorization (str "Token " user-token))
                     ((app)))]
    (is (= 200 (:status response)))
    (is (not (db/study-exists? study-id1)))
    (is (not (db/study-exists? study-id2)))))

(deftest download-session-data
  (let [{user-token :token user-id :id} (u/get-user)
        study-id (db/add-study user-id u/test-study)
        uuid (.toString (UUID/randomUUID))
        session {:duration 105.0
                 :id uuid
                 :mapping1Result
                 "Mapping Type,\"mapping1\"\nStart,\"Wed Aug 11 2021 16:17:26 GMT+0200 (Central European Summer Time)\"\nDuration,\"15.6\"\nConnections\nY,C,3\nA,H,2\nY,A,2\nH,Y,1\n",
                 :mapping2Result
                 "Mapping Type,\"mapping2\"\nStart,\"Wed Aug 11 2021 16:17:45 GMT+0200 (Central European Summer Time)\"\nDuration,\"19.9\"\nConnections\nY,N,2\nY,E,1\nG,O,1\nY,N,3\nG,Y,2\n",
                 :startedAt (read-string "#inst\"2021-02-26T22:24:15Z\"")}]
    (db/add-session-data study-id session)
    (let [response (-> (request :get (str "/api/studies/" study-id "/session-data"))
                       (header :accept "application/edn")
                       (header :authorization (str "Token " user-token))
                       ((app)))
          body (m/decode-response-body response)]
      (is (= 200 (:status response)))
      (is (= "Session data file has been successfully created!" (:message body)))
      (is (= 200 (:status (-> (request :get (:link body))
                              (header :authorization (str "Token " user-token))
                              ((app)))))))))
