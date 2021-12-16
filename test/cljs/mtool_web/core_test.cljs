(ns mtool-web.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [re-frame.core :as rf]
            [day8.re-frame.test :as rf-test]
            [lf.auth.login-events :as le]
            [mtool-web.events]
            [lf.auth.registration-events :as re]))

(deftest logout
  (rf-test/run-test-sync
    (let [db {:login/token 12222}
          res (le/logout db nil)]
      (is (not (:login/token res))))))



(deftest registration
  (rf-test/run-test-sync
    (let [db {:registration/email "jo@x"
              :registration/password "12345678"}
          res (re/signup {:db db} nil)]
      (testing "password do not match"
        (is (= re/pass-no-match (:common/error (:db res)))))
      (testing "terms not accepted"
        (let [db (assoc db :registration/confirmation-password "12345678")
              res (re/signup {:db db} nil)]
          (is (= re/terms-not-accepted (:common/error (:db res)))))))))


;; Does not work
#_(deftest registration-confirmed
  (rf-test/run-test-sync
    (testing "shows registration-confirmed screen"
      (rf/dispatch-sync [:lf.auth.registration-events/successful-signup])
      (is (= @(rf/subscribe [:common/page]) lf.auth.registration/registration-confirmed-page)))))
