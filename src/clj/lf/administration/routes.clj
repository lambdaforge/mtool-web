(ns lf.administration.routes
  (:require
   [mtool-web.db.media :refer [delete-media get-user-media get-media-location]]
   [lf.administration.email :as mail]
   [lf.administration.db :as db]
   [lf.middleware.auth :as auth]
   [lf.middleware.formats :as formats]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [clojure.spec.alpha :as s]
   [taoensso.timbre :as log]
   [buddy.hashers :as hash]
   [clojure.java.io :as io])
  (:import [java.util Date]))

(s/def ::token string?)
(s/def ::authorization string?)
(s/def ::auth-header (s/keys :req-un [::authorization]))
(s/def ::id string?)                                        ;; uuid string
(s/def ::email #(re-matches #".+@.+\..+" %))
(s/def ::password string?)
(s/def ::jwt-content (s/keys :req-un [::id ::email]))

(defn api-base-route [api-name]
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware       ;; decodes body? used in muuntaja/format-request-m
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart, for files
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title api-name
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]])

(defn administration-routes []
  [["" {:swagger {:tags ["Administration"]}}
    ["/login"
     {:get {:middleware [(auth/create-basic-auth-middleware) auth/auth-middleware]
            :summary    "Login with base64 encoded username:password via Authorization header"
            :parameters {:header ::auth-header}
            :responses  {200 {:body {:message string?
                                     :user    ::jwt-content}}
                         400 {:body {:message string?}}}
            :handler    (fn [{{id :id :as user} :identity}]
                          (if (db/user-verified? id)
                            {:status 200
                             :body   {:message "Login succeeded!"
                                      :user    user}}
                            {:status 400
                             :body   {:message "Your account has not been verified! Please check your emails or click on resent verification."}}))}}]

    ["/resent-verification"
     {:get {:summary "Resend verification request"
             :middleware [(auth/create-basic-auth-middleware) auth/auth-middleware]
             :parameters {:header ::auth-header}
             :responses {200 {:body {:message string?}}
                         400 {:body {:message string?}}}
             :handler (fn [{{id :id} :identity}]
                        (let [{:keys [error message]} (mail/send-account-verification-link (db/get-user-email id))]
                          (if (= error :SUCCESS)
                            {:status 200
                             :body {:message "Verification resent succeeded! Please check your email."}}
                            (do (log/error (str "Error sending mail: " message))
                                {:status 400
                                 :body {:message "Sending mail failed!"}}))))}}]

    ["/reset-request"
     {:get {:summary "Request password reset email"
            :parameters {:query {:email ::email}}
            :responses {200 {:body {:message string?}}
                        400 {:body {:message string?}}}
            :handler (fn [{{{:keys [email]} :query} :parameters}]
                       (if (db/user-email-exists? email)
                         (let [{:keys [error message]} (mail/send-password-reset-link email)]
                           (if (= error :SUCCESS)
                             {:status 200
                              :body {:message "Password reset email was successfully sent!"}}
                             (do (log/error (str "Error sending mail: " message))
                                 {:status 400
                                  :body {:message "Password reset failed!"}})))
                         (do (log/error (str "Requested Email address " email " could not be found in database!"))
                             {:status 400
                              :body {:message "Password reset failed!"}})))}}]]

   ["/accounts" {:swagger {:tags ["Administration"]}}
     [""
      {:post {:summary "Register a new user"
              :parameters {:body {:email ::email :password ::password}}
              :responses {200 {:body {:message string?
                                      :user-id :s/id}}
                          400 {:body {:message string?}}}
              :handler (fn [{{{:keys [email password]} :body} :parameters}]
                         (if-not (db/user-email-exists? email)
                           (let [user-id (db/add-user {:email email :pwd (hash/derive password) :init-date (Date.)})
                                 {:keys [error message]} (mail/send-account-verification-link email)]
                             (if (= error :SUCCESS)
                               {:status 200
                                :body {:message "Registration succeeded!"
                                       :user-id user-id}}
                               (do (log/error (str "Error sending mail: " message))
                                   {:status 400
                                    :body {:message "Sending mail failed!"}})))
                           {:status 400
                            :body {:message "Error with account: user already exists."}}))}}]
     ["/:id"
      [""
       {:delete {:middleware [auth/token-auth-middleware auth/auth-middleware]
                 :summary "Delete current user and their files"
                 :parameters {:path {:id ::id}}
                 :responses {200 {:body {:message string?}}
                             400 {:body {:message string?}}}
                 :handler (fn [{{user-id :id} :identity
                                {{:keys [id]} :path} :parameters}]
                            (if (= user-id id)
                              (do (doseq [media (get-user-media user-id)
                                          :let [path (get-media-location (:id media))]]
                                    (delete-media (:id media))
                                    (io/delete-file (io/file path)))
                                  (db/delete-user user-id)
                                  {:status 200
                                   :body {:message "The account has been deleted!"}})
                              (if (db/user-exists? id)
                                (do (log/error "Account deletion failed: existing account number '" id "' doesn't match logged in user with ID'" user-id "' !")
                                    {:status 400
                                     :body {:message "Account could not be deleted!"}})
                                (do (log/error "Account deletion failed: account number '" id "' doesn't exist!")
                                    {:status 400
                                     :body {:message "Account could not be deleted!"}}))))}}]

      ["/reset-password"
       {:put {:summary "Reset password"
              :middleware [auth/ot-token-auth-middleware auth/auth-middleware]
              :parameters {:body {:password ::password}
                           :path {:id ::id}}
              :responses {200 {:body {:message string?}}
                          400 {:body {:message string?}}}
              :handler (fn [{{user-id :id} :identity
                             {{:keys [id]} :path
                              {:keys [password]} :body} :parameters}]
                         (if (= user-id id)
                           (do (db/reset-user-pwd id (hash/derive password))
                               {:status 200
                                :body {:message "Password reset has been successful!"}})
                           (if (db/user-exists? id)
                             (do (log/error "Password reset failed: existing account number '" id "' doesn't match logged in user with ID'" user-id "' !")
                                 {:status 400
                                  :body {:message "Password reset failed!"}})
                             (do (log/error "Password reset failed: account number '" id "' doesn't exist!")
                                 {:status 400
                                  :body {:message "Password reset failed!"}}))))}}]
      ["/verify"
       {:put {:summary "Verify a user account"
              :middleware [auth/token-auth-middleware auth/auth-middleware]
              :parameters {:path {:id ::id}}
              :responses {200 {:body {:message string?}}
                          400 {:body {:message string?}}}
              :handler (fn [{{user-id :id} :identity
                             {{:keys [id]} :path} :parameters}]
                         (if (= user-id id)
                           (do (db/verify-user id)
                               {:status 200
                                :body {:message "The account has been verified!"}})
                           (if (db/user-exists? id)
                             (do (log/error "Account verification failed: existing account number '" id "' doesn't match logged in user with ID'" user-id "' !")
                                 {:status 400
                                  :body {:message "Account could not be verified!"}})
                             (do (log/error "Account verification failed: account number '" id "' doesn't exist!")
                                 {:status 400
                                  :body {:message "Account could not be verified!"}}))))}}]]]])
