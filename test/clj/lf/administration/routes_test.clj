(ns lf.administration.routes-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [ring.mock.request :refer [request json-body header]]
   [mtool-web.handler :refer [app]]
   [lf.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mtool-web.config :refer [env]]
   [mount.core :as mount]
   [clojure.tools.logging :as log]
   [lf.administration.db :as db]
   [clojure.string :as s]
   [buddy.hashers :as hash])
  (:import [java.util Base64 Date]
           [javax.mail Folder Session Flags$Flag]))

(use-fixtures
 :once
 (fn [f]
   (mount/start #'mtool-web.config/env
                #'mtool-web.handler/app-routes
                #'lf.administration.db/conn)
   (f)))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(defn ->base64
  "Encodes a string as base64."
  [s]
  (.encodeToString (Base64/getEncoder) (.getBytes s)))

(defn login [email password]
  (-> (request :get "/api/login")
      (header :authorization (str "Basic " (->base64 (str email ":" password))))
      (header "accept" "application/edn")
      ((app))))

(def valid-creds
  {:email "abc@example.com"
   :password "123"
   :id "3afb82dd-034f-4e4a-b862-06719831a42b"})

(defn get-user
  ([] (get-user (:email valid-creds) (:password valid-creds)))
  ([email password]
   (-> (login email password) m/decode-response-body :user)))

(defn get-messages-about [subject]
  (let [props (doto (System/getProperties)
                (.setProperty "mail.store.protocol" "imaps"))
        session (Session/getDefaultInstance props nil)
        store (.getStore session "imaps")
        {:keys [user pass]} (:mail-account env)
        server (str "imap." (subs user (inc (s/last-index-of user "@"))))
        email (-> env :mail-account :user)
        _ (.connect store server email pass)
        inbox (doto ^Folder (.getFolder store "INBOX")
                (.open Folder/READ_WRITE))
        relevant (->> (.getMessages inbox)
                      (filter #(and (= (.getSubject %) subject)))
                      (mapv #(hash-map :msg % :content (.getContent %))))]
    (doall (map #(.setFlag (:msg %) Flags$Flag/DELETED true) relevant))
    (.close inbox false)
    (.close store)
    (mapv :content relevant)))

(defn get-token-from-mail [subroute mail-subject user-id]
  (loop [tries-left 10 token nil]
    (cond
      (not (nil? token)) token
      (= 0 tries-left) (do (log/warn (str "Email with subject '" mail-subject " for user with ID '" user-id "' doesn't exist!"))
                           nil)
      :else (do (Thread/sleep 1000)
                (let [token (->> (get-messages-about mail-subject)
                                 (map #(zipmap [:user :token]
                                               (rest (re-find (re-pattern (str subroute "/([^/]+)/(.*)")) %))))
                                 (filter #(= (:user %) user-id))
                                 last
                                 :token)]
                  (recur (dec tries-left) token))))))

(deftest basic
  (testing "main route"
           (let [response ((app) (request :get "/"))]
             (is (= 200 (:status response)))))

  (testing "not-found route"
           (let [response ((app) (request :get "/invalid"))]
             (is (= 404 (:status response))))))

;; Works fine with 'lein test' (but has issues under CIDER)
(deftest signup-test
  (let [response-fn (fn [params] ((app) (-> (request :post "/api/accounts")
                                            (json-body params))))]

    (testing "successful signup"                                  ;; Works also when email doesn't exist
             (let [email (:user (:mail-account env))
                   _ (when-let [old-user-id (db/get-user-id email)]
                       (db/delete-user old-user-id))
                   response (response-fn {:email email
                                          :password "12345678"})
                   body (m/decode-response-body response)
                   user-id (:user-id body)]
               (is (= 200 (:status response)))
               (is (= "Registration succeeded!" (:message body)))
               (is (= false (:verified (db/get-user :user/email email))))

               (testing "successful verification"
                 (let [mail-subject "M-Tool Account Verification"
                       token (get-token-from-mail "verify-account" mail-subject user-id)
                       response (-> (request :put (str "/api/accounts/" user-id "/verify"))
                                    (header :authorization (str "Token " token))
                                    (header :accept "application/edn")
                                    ((app)))
                       body (m/decode-response-body response)]
                   (is (not (nil? token)))
                   (is (= 200 (:status response)))
                   (is (= "The account has been verified!" (:message body)))
                   (is (= true (:verified (db/get-user :user/email email)))))
                 (db/delete-user user-id))))

    (testing "wrong email format"
             (let [response (response-fn {:email "jopline.ch"
                                          :password "12345678"})]
               (is (= 400 (:status response)))
               (is (= ["email"] (-> (m/decode-response-body response) :problems first :path)))))

    (testing "account already exists"
             (let [email "jop@line.ch"
                   password "12345678"
                   _ (db/add-user {:email email :pwd password :init-date (Date.)})
                   response (response-fn {:email email,
                                          :password password})]
               (is (= 400 (:status response)))
               (is (= {:message "Error with account: user already exists."}
                      (m/decode-response-body response)))))))

(deftest login-test
  (testing "Successful login"
           (let [{:keys [email password id]} valid-creds
                 response (-> (request :get "/api/login")
                              (header :authorization (str "Basic " (->base64 (str email ":" password))))
                              (header :accept "application/edn")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (= 200 (:status response)))
             (is (= "Login succeeded!" (:message body)))
             (is (= #{:id :email :token} (-> body :user keys set)))
             (is (= id (-> body :user :id)))))

  (testing "Login with wrong password"
           (let [{:keys [email password]} valid-creds
                 wrong-password (str password "gregre")
                 response (-> (request :get "/api/login")
                              (header :authorization (str "Basic " (->base64 (str email ":" wrong-password))))
                              (header :accept "application/edn")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (= 401 (:status response)))
             (is (= "Unauthorized" (:error body)))))

  (testing "Login with unknown user"
           (let [email "some-email@sth.com"
                 password "apassword"
                 response (-> (request :get "/api/login")
                              (header :authorization (str "Basic " (->base64 (str email ":" password))))
                              (header :accept "application/edn")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (= 401 (:status response)))
             (is (= "Unauthorized" (:error body)))))

  (testing "Login of unverified user"
           (let [email "efg@example.de"
                 password "456"
                 response (-> (request :get "/api/login")
                              (header :authorization (str "Basic " (->base64 (str email ":" password))))
                              (header :accept "application/edn")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (= 400 (:status response)))
             (is (= "Your account has not been verified! Please check your emails or click on resent verification." (:message body)))
             (is (= #{} (-> body :user keys set))))))

(deftest password-reset
  (testing "successful reset"
           (let [email (:user (:mail-account env))
                 password "12345678"
                 _ (do (when-let [old-user-id (db/get-user-id email)]
                         (db/delete-user old-user-id))
                       (db/add-user {:email email
                                     :pwd password
                                     :init-date (Date.)}))

                 response (-> (request :get (str "/api/reset-request?email=" email))
                              (header :accept "application/edn")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (= 200 (:status response)))
             (is (= "Password reset email was successfully sent!" (:message body)))

             (let [mail-subject "M-Tool Password Reset"
                   user-id (db/get-user-id email)
                   token (get-token-from-mail "reset-password" mail-subject user-id)
                   new-password "abcdefg"]

               (testing "successful password reset"
                 (let [response (-> (request :put (str "/api/accounts/" user-id "/reset-password"))
                                    (header :authorization (str "Token " token))
                                    (header :accept "application/edn")
                                    (json-body {:password new-password})
                                    ((app)))
                       body (m/decode-response-body response)]
                   (is (not (nil? token)))
                   (is (= 200 (:status response)))
                   (is (= "Password reset has been successful!" (:message body)))
                   (is (hash/check new-password (:pwd (db/get-user :user/email email))))))

               (testing "trying to use token a second time"
                        (let [second-password "uvwxyz"
                              response (-> (request :put (str "/api/accounts/" user-id "/reset-password"))
                                           (header :authorization (str "Token " token))
                                           (header :accept "application/edn")
                                           (json-body {:password second-password})
                                           ((app)))
                              body (m/decode-response-body response)]
                          (is (not (nil? token)))
                          (is (= 401 (:status response)))
                          (is (= "Unauthorized" (:error body)))
                          (is (hash/check new-password (:pwd (db/get-user :user/email email))))))
               (db/delete-user user-id))))
  (testing "unsuccessful reset request"
           (let [unregistered-address "vferere@grt.jkl"
                 response (-> (request :get (str "/api/reset-request?email=" unregistered-address))
                              (header :accept "application/edn")
                              ((app)))
                 body (m/decode-response-body response)]
             (is (= 400 (:status response)))
             (is (= "Password reset failed!" (:message body))))))

;; Fails when test is run a second time on same repl server
(deftest account-deletion
  (testing "successful deletion"
   (let [email "hij@example.com"
         {:keys [email pwd createdAt]} (db/get-user :user/email email)
         {:keys [id token]} (get-user email "789")
         response (-> (request :delete (str "/api/accounts/" id))
                      (header :accept "application/edn")
                      (header :authorization (str "Token " token))
                      ((app)))]
     (is (= 200 (:status response)))
     (is (= {:message "The account has been deleted!"}
            (m/decode-response-body response)))
     (is (not (db/user-exists? id)))

     (db/add-user {:email "hij@example.com"
                   :pwd pwd
                   :init-date createdAt})))

  (testing "path user ID and token user id do not match"
           (let [user1 (get-user "123@example.com" "abc")
                 user2 (get-user "abc@example.com" "123")
                 response (-> (request :delete (str "/api/accounts/" (:id user1)))
                              (header :accept "application/edn")
                              (header :authorization (str "Token " (:token user2)))
                              ((app)))]
             (is (= 400 (:status response)))
             (is (= {:message "Account could not be deleted!"}
                    (m/decode-response-body response)))))

  (testing "path user ID does not exist"
           (let [user (get-user "abc@example.com""123")
                 non-existing-user-id "grewlojikokdoewj"
                 response (-> (request :delete (str "/api/accounts/" non-existing-user-id))
                              (header :accept "application/edn")
                              (header :authorization (str "Token " (:token user)))
                              ((app)))]
             (is (= 400 (:status response)))
             (is (= {:message "Account could not be deleted!"}
                    (m/decode-response-body response))))))
