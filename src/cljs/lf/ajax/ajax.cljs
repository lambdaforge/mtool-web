(ns lf.ajax.ajax
  (:require
   [ajax.core :as ajax]
   [ajax.protocols :as ap]
   [taoensso.timbre :as log]
   [luminus-transit.time :as time]))


(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (-> request
        (update :headers #(merge {"x-csrf-token" js/csrfToken} %)))
    request))

;; injects transit serialization config into request options
(defn as-transit [opts]
  (merge {:raw             false
          :format          (ajax/transit-request-format {:json time/time-deserialization-handlers})
          :response-format (ajax/transit-response-format {:json time/time-serialization-handlers})}
         opts))

(defn unauthorized-response [response]
  (.log js/console (ap/-status response))
  response)



(defn load-interceptors! []
  (swap! ajax/default-interceptors
         concat
         [(ajax/to-interceptor {:name "default headers"
                                :request default-headers})
          (ajax/to-interceptor {:name "Unauthorized"
                                :response unauthorized-response})]))


(defn call
  "Makes an ajax call only if the auth token is present."
  [http-params {:keys [:login/token :login/date] :as db}]
  (let [now (js/Date.)]
    (if (and token date)
      (if (< (- now date) 3600000)
        {:http-xhrio (as-transit http-params)}
        {:db (-> db
                 (assoc :common/error (str "Please login again."))
                 (dissoc :login/token :login/date))
         :common/navigate-fx! [:login]})
      {:db (-> db
               (assoc :common/error (str "Please login again."))
               (dissoc :login/token :login/date))
       :common/navigate-fx! [:login]})))






