(ns lf.middleware.auth
  (:require [mtool-web.config :as config]
            [lf.administration.db :as db]
            [buddy.auth :as buddy-auth]
            [buddy.auth.backends :as buddy-auth-backends]
            [buddy.auth.backends.httpbasic :as buddy-auth-backends-httpbasic]
            [buddy.auth.middleware :as buddy-auth-middleware]
            [buddy.hashers :as buddy-hashers]
            [buddy.sign.jwt :as jwt]))

(def one-time-tokens (atom #{}))

(defn create-token
  "Creates a signed jwt-token with user data as payload.
  `valid-seconds` sets the expiration span."
  [user & {:keys [valid-seconds one-time-token?] :or {valid-seconds 7200 ;; 2 hours
                                                      one-time-token? false}}]
  (let [payload (-> user
                    (select-keys [:id :email])
                    (assoc :exp (.plusSeconds
                                 (java.time.Instant/now) valid-seconds)))
        token (jwt/sign payload config/private-key {:alg :hs512})]
    (when one-time-token?
     (swap! one-time-tokens conj token))
    token))

(defn- ot-token-auth
  "Authentication function called from token-auth middleware for each
   request. The result of this function will be added to the request
   under key :identity."
  [request token]
  (let [{:keys [exp] :as msg} (jwt/unsign token config/private-key {:alg :hs512})
        now (.getEpochSecond (java.time.Instant/now))]
    (when (and (contains? @one-time-tokens token)
               (< now exp))
      (swap! one-time-tokens disj token)
      (dissoc msg :exp))))

(def ot-token-backend
  "Backend for verifying one-time JWT-tokens."
  (buddy-auth-backends/token {:authfn ot-token-auth}))

(def token-backend
  "Backend for verifying JWT-tokens."
  (buddy-auth-backends/jws {:secret config/private-key :options {:alg :hs512}}))

(defn- basic-auth
  "Authentication function called from basic-auth middleware for each
  request. The result of this function will be added to the request
  under key :identity.
  NOTE: Use HTTP Basic authentication always with HTTPS in real setups."
  [request {:keys [username password]}]
  (let [user (db/get-user :user/email username)]
    (when (and user (buddy-hashers/check password (:pwd user)))
      (-> user
          (select-keys [:id :email])
          (assoc :token (create-token user))))))

(defn create-basic-auth-backend
  "Creates basic-auth backend to be used by basic-auth-middleware."
  []
  (buddy-auth-backends-httpbasic/http-basic-backend
   {:authfn (partial basic-auth)}))

(defn create-basic-auth-middleware
  "Creates a middleware that authenticates requests using http-basic
  authentication."
  []
  (let [backend (create-basic-auth-backend)]
    (fn [handler]
      (buddy-auth-middleware/wrap-authentication handler backend))))

(defn token-auth-middleware
  "Middleware used on routes requiring token authentication."
  [handler]
  (buddy-auth-middleware/wrap-authentication handler token-backend))

(defn ot-token-auth-middleware
  "Middleware used on routes requiring one-time-token authentication."
  [handler]
  (buddy-auth-middleware/wrap-authentication handler ot-token-backend))

(defn auth-middleware
  "Middleware used in routes that require authentication. If request is
  not authenticated a 401 unauthorized response will be
  returned. Buddy checks if request key :identity is set to truthy
  value by any previous middleware."
  [handler]
  (fn [request]
    (if (buddy-auth/authenticated? request)
      (handler request)
      {:status 401 :body {:error "Unauthorized"}})))
