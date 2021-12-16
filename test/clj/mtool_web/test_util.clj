(ns mtool-web.test-util
  (:require
   [clojure.java.io :as io]
   [lf.administration.db :as adb]
   [lf.middleware.formats :as formats]
   [clojure.edn :as edn]
   [clojure.string :as s]
   [mtool-web.config :as c :refer [env]]
   [mtool-web.db.media :as db]
   [mtool-web.handler :refer [app]]
   [muuntaja.core :as m]
   [ring.mock.request :refer [request header body]]
   [buddy.hashers :as hash])
  (:import [java.util Base64 Date]))

(defn get-study-link [study-id] (str (:base-url env) "/studies/" study-id))

(defn get-media-link [user-id filename] (str (:base-url env) "/" user-id "/" filename))

(defn transit-body [req params]
  (let [m (m/create (assoc m/default-options :return :bytes))]
    (-> req
        (header :content-type "application/transit+json")
        (body (m/encode m "application/transit+json" params)))))

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

(defn spawn-user [email password]
  (let [uid (adb/add-user {:email email :pwd (hash/derive password) :init-date (Date.)})]
    (adb/verify-user uid)
    (get-user email password)))

(defn insert-file [filepath user-id]
  (let [file (io/file (io/resource filepath))
        s-index (if-let [ind (s/last-index-of filepath "/")] ind -1)
        filename (subs filepath (inc s-index) (count filepath))
        path (str c/user-media-dir "/" user-id "/" filename)
        link (str (:base-url env) "/" user-id "/" filename)
        _ (io/make-parents path)
        _ (io/copy file (io/file path))
        mid (db/add-media user-id
                          {:filename filename
                           :note "note"
                           :type :image
                           :link link
                           :name "name"
                           :path path})]
    {:media-id mid
     :path path
     :link link}))

(defn clean-up-file [user-id filename]
  (let [path (str c/user-media-dir "/" user-id "/" filename)
        link (get-media-link user-id filename)]
    (when (.exists (io/file path))
      (io/delete-file path))
    (if-let [media-id (db/get-media-id link)]
      (db/delete-media media-id))))

(defn add-user-file [filename user-id]
  (clean-up-file user-id filename)
  (insert-file filename user-id))

(def test-study (edn/read-string (slurp (io/resource "test-study.edn"))))
