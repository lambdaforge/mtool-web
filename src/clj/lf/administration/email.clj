(ns lf.administration.email
  (:require [postal.core :refer [send-message]]
            [lf.middleware.auth :refer [create-token]]
            [mtool-web.config :refer [env]]
            [lf.administration.db :refer [get-user]]))

(defn send-email
  "Send mail to given address.
  Returns map with code, error and message
  See https://github.com/drewr/postal"
  [subject body recipient]
  (let [email {:from (:user (:mail-account env))
               :to recipient
               :subject subject
               :body body}]
    (send-message (:mail-account env) email)))

(defn send-password-reset-link
  "Sends a link to email address"
  [recipient]
  (let [user (get-user :user/email recipient)
        id (:id user)
        token (create-token user :one-time-token? true)
        link (str (:base-url env) "#/reset-password/" id "/" token)]
    (send-email "M-Tool Password Reset"
                (str "Your password reset link: " link)
                recipient)))

(defn send-account-verification-link
  "Sends a link to email address"
  [recipient]
  (let [user (get-user :user/email recipient)
        id (:id user)
        token (create-token user)
        link (str (:base-url env) "#/verify-account/" id  "/" token)]
    (send-email "M-Tool Account Verification"
                (str "Your verification link: " link)
                recipient)))
