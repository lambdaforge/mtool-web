(ns mtool-web.media-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.java.io :as io]
   [mount.core :as mount]
   [mtool-web.config :refer [env]]
   [mtool-web.db.media :as db]
   [mtool-web.db.study :as sdb]
   [mtool-web.handler :refer [app ring-handler]]
   [mtool-web.test-util :as u]
   [muuntaja.core :as m]
   [ring.mock.request :refer [request header]]))

(use-fixtures
 :once
 (fn [f]
   (mount/start #'mtool-web.config/env
                #'mtool-web.handler/app-routes
                #'lf.administration.db/conn)
   (f)))

(deftest add-media-files
  (let [{user-token :token user-id :id} (u/get-user)]

    (testing "Successfully add file"
      (let [filename "file1.txt"
            _ (u/clean-up-file user-id filename)
            file (io/file (io/resource filename))
            response (ring-handler {:headers {:accept "application/edn"
                                              :authorization (str "Token " user-token)}
                                    :request-method :post
                                    :uri "/api/media"
                                    :multipart-params {:file {:filename filename
                                                              :content-type "text/plain"
                                                              :size (.length file)
                                                              :tempfile file}
                                                       "note" "some note"
                                                       "name" "some name"
                                                       "type" "image"}})
            body (m/decode-response-body response)]
        (is (= 200 (:status response)))
        (is (= "Media file has been stored successfully!" (:message body)))
        (is (not (nil? (:media-id body))))
        (is (not (nil? (:link body))))
        (is (db/media-exists? (:media-id body)))
        (is (.exists (io/file (db/get-media-location (:media-id body)))))
        (is (= 200
               (-> (request :get (:link body))
                   (header :accept "application/edn")
                   (header :authorization (str "Token " user-token))
                   ((app))
                   :status)))

        (testing "Try adding existing file"
          (let [response (ring-handler {:headers {:accept "application/edn"
                                                  :authorization (str "Token " user-token)}
                                        :request-method :post
                                        :uri "/api/media"
                                        :multipart-params {:file {:filename filename
                                                                  :content-type "text/plain"
                                                                  :size (.length file)
                                                                  :tempfile file}
                                                           "note" "some note"
                                                           "name" "some name"
                                                           "type" "image"}})
                body (m/decode-response-body response)]
            (is (= 400 (:status response)))
            (is (= "User media with given name and type has already been uploaded!" (:message body)))))
        (u/clean-up-file user-id filename)))

    (testing "Add file with null note"
             (let [filename "file1.txt"
                   _ (u/clean-up-file user-id filename)
                   file (io/file (io/resource filename))
                   response (ring-handler {:headers {:accept "application/edn"
                                                     :authorization (str "Token " user-token)}
                                           :request-method :post
                                           :uri "/api/media"
                                           :multipart-params {:file {:filename filename
                                                                     :content-type "text/plain"
                                                                     :size (.length file)
                                                                     :tempfile file}
                                                              "name" "some name"
                                                              "note" "null"
                                                              "type" "image"}})
                   {:keys [message media-id]} (m/decode-response-body response)]
               (is (= 200 (:status response)))
               (is (= "Media file has been stored successfully!" message))
               (is (= "" (:note (db/get-media media-id))))
               (u/clean-up-file user-id filename)))

    (testing "Try adding too large file"
       (let [filename "Introduction.mp4"
             file (io/file (io/resource (str "public/participant/video/" filename)))
             response (ring-handler {:headers {:accept "application/edn"
                                               :authorization (str "Token " user-token)}
                                     :request-method :post
                                     :uri "/api/media"
                                     :multipart-params {:file {:filename filename
                                                               :content-type "text/plain"
                                                               :size (.length file)
                                                               :tempfile file}
                                                        "name" "some name"
                                                        "note" "some note"
                                                        "type" "image"}})
             body (m/decode-response-body response)]
         (is (= 403 (:status response)))
         (is (= (str "Media file is too large. Allowed are " (:max-file-size env) " Mb.")
                (:message body)))
         (u/clean-up-file user-id filename)))))

(deftest list-files
  (let [{user-token :token user-id :id} (u/get-user)
        filename1 "file1.txt"
        filename2 "file2.txt"]

    (testing "Get empty list of media files"
      (doseq [filename (map :filename (db/get-user-media user-id))]
        (u/clean-up-file user-id filename))
      (let [response (-> (request :get "/api/media")
                         (header :accept "application/edn")
                         (header :authorization (str "Token " user-token))
                         ((app)))]
        (is (= 200 (:status response)))
        (is (= (m/decode-response-body response)
               {:message "User media has been fetched!"
                :media []}))))

    (testing "Get list of media files with 2 entries"
      (u/add-user-file filename1 user-id)
      (u/add-user-file filename2 user-id)
      (let [response (-> (request :get "/api/media")
                         (header :accept "application/edn")
                         (header :authorization (str "Token " user-token))
                         ((app)))
            body (m/decode-response-body response)]
        (is (= 200 (:status response)))
        (is (= "User media has been fetched!" (:message body) ))
        (is (= 2 (count (:media body))))
        (is (-> body :media first keys)
            #{:id :type :note :filename :link :addedAt})))))

(deftest media-deletion
  (let [{user-token :token user-id :id} (u/get-user)]

    (testing "Successful deletion"
      (let [media-id (:media-id (u/add-user-file "file1.txt" user-id))
            response (-> (request :delete (str "/api/media/" media-id))
                         (header :accept "application/edn")
                         (header :authorization (str "Token " user-token))
                         ((app)))
            body (m/decode-response-body response)]
        (is (= 200 (:status response)))
        (is (= "File has been deleted!" (:message body)))))

    (testing "File being used"
      (let [{:keys [media-id link]} (u/add-user-file "file1.txt" user-id)
            test-study (assoc-in u/test-study [:introduction :video :link] link)
            _ (sdb/add-study user-id test-study)
            response (-> (request :delete (str "/api/media/" media-id))
                         (header :accept "application/edn")
                         (header :authorization (str "Token " user-token))
                         ((app)))
            body (m/decode-response-body response)]
        (is (= 400 (:status response)))
        (is (= "Media file cannot be deleted because it is used in studies as: (:introduction/video)"
               (:message body)))) )))

(deftest user-media-deletion
  (let [{user-token :token user-id :id} (u/spawn-user "a.b@c.com" "gfegregregr")
        {media-id1 :media-id path1 :path} (u/add-user-file "file1.txt" user-id)
        {media-id2 :media-id path2 :path} (u/add-user-file "file2.txt" user-id)
        response (-> (request :delete (str "/api/accounts/" user-id))
                     (header :authorization (str "Token " user-token))
                     ((app)))]
    (is (= 200 (:status response)))
    (is (not (db/media-exists? media-id1)))
    (is (not (.exists (io/file path1))))
    (is (not (db/media-exists? media-id2)))
    (is (not (.exists (io/file path2))))))
