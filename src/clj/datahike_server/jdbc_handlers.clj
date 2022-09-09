(ns datahike-server.jdbc-handlers
  (:require [datahike-server.database :refer [conns]]
            [mount.core :as mount]
            [datahike-server.config :as config]            
            [datahike-server.jdbc :as j]
            [datahike-server.middleware :as middleware]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            ;; [org.httpkit.client :as http]
            [clojure.string :as str]))

(s/def ::name string?)
(s/def ::keep-history? boolean?)
(s/def ::delete? boolean?)
(s/def ::schema-flexibility (s/or :keyword keyword? :string string?))
(s/def ::initial-tx (s/coll-of map?))

(s/def ::database (s/keys :req-un [::name] 
                          :opt-un [::keep-history? ::schema-flexibility ::initial-tx]))

(s/def ::removal (s/keys  :req-un [::name] 
                          :opt-un [::delete?]))

(defn success
  ([] {:status 200})
  ([data] {:status 200 :body data}))

(defn create-database [{{:keys [body]} :parameters}]
  (if (contains? conns (:name body))
    (throw (ex-info
            (str "A database with name '" (:name body) "' already exists. Database names on the transactor should be unique.")
            {:event :connection/initialization
             :error :database.name/duplicate}))
    (let [[created? connection] (j/add-database body config/config)]
      (when connection
        (-> 
        (mount/swap {#'datahike-server.database/conns  (assoc conns (:name body) connection)})
        (mount/start)))
      (success {:name (:name body) :connected (some? connection) :created created?}))))
  
(defn remove-database [{{:keys [body]} :parameters}]
  (let [status (j/delete-database body config/config)]
    (-> 
      (mount/swap {#'datahike-server.database/conns (dissoc conns (:name body))})
      (mount/start))
    (success {:name (:name body) :disconnected true :deleted (some? status)})))

;; (defn build-url [host path query-string]
;;   (let [url (.toString (java.net.URL. (java.net.URL. host) path))]
;;     (if (not-empty query-string)
;;       (str url "?" query-string)
;;       url)))

;; (defn host-from-url [url]
;;   (when url
;;     (str/replace url #"http://" "")))

;; (defn proxy-request [request]
;;   (let [host "http://localhost:8080"
;;         stripped-headers (dissoc (:headers request) "content-length")
;;         replaced-host-headers (assoc stripped-headers "host" (host-from-url host))
;;         res (if host
;;               (select-keys  @(http/request {:url (build-url host (:uri request) (:query-string request))
;;                                             :method           (:request-method request)
;;                                             :body             (:body request)
;;                                             :headers          replaced-host-headers})
;;                             [:status :headers :body])
;;               {:status 200 :body "haha"})]
;;     res))

(def routes
  [["/ping"
    {:get {:no-doc  true
           :handler (fn [_request] {:status 200 :body {:message "pew pew"}})}}]

   ["/hello"
    {:get {:handler (fn [_request] {:status 200 :body {:message "pew pew"}})}}]

   ["/create-database"
    {:swagger {:tags ["API"]}
     :post    {:operationId "CreateDatabase"
               :summary "Connect to a datahike database create it if it does not exist"
               :parameters {:body   (st/spec {:spec ::database
                                              :name "creation configuration"})}
               :middleware [middleware/token-auth middleware/auth]
               :handler    create-database}}]
  ;;  ["/metrics"
  ;;   {:swagger {:tags ["API"]}
  ;;    :get    {:operationId "METRICS"
  ;;              :summary "Reverse proxy to metrics"
  ;;              :handler  proxy-request}}]

   ["/delete-database"
    {:swagger {:tags ["API"]}
     :post    {:operationId "DeleteDatabase"
               :summary "Remove connection to an existing database optionally delete it entirely"
               :parameters {:body   (st/spec {:spec ::removal
                                              :name "removal target"})}
               :middleware [middleware/token-auth middleware/auth]
               :handler    remove-database}}]])