(ns datahike-server.config
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [environ.core :refer [env]]
            [datahike.config :refer [int-from-env bool-from-env]]
            [clojure.edn :as edn]))

(s/fdef load-config-file
  :args (s/cat :config-file string?)
  :ret map?)
(s/fdef load-config
  :args (s/cat :config-file #(or (string? %) (map? %)))
  :ret map?)
(s/def ::port int?)
(s/def ::join? boolean?)
(s/def ::loglevel #{:trace :debug :info :warn :error :fatal :report})
(s/def ::token keyword?)
(s/def ::dev-mode boolean?)

;; customization start
(s/def ::dbtype string?)
(s/def ::dbport int?)
(s/def ::jdbc-url (s/nilable string?))
(s/def ::host string?)
(s/def ::dbname string?)
(s/def ::user string?)
(s/def ::password string?)
(s/def ::max-body int?)
(s/def ::persistent-databases string?)
(s/def ::jdbc-options (s/nilable map?))

(s/def ::server-config-map (s/keys  :req-un [::port ::loglevel ::cache-size ::dbtype ::host ::dbname ::user ::password]
                                    :opt-un [::dev-mode ::token ::join? ::persistent-databases ::jdbc-url ::dbport ::jdbc-options ::max-body]))

(s/def ::server-config-url (s/keys  :req-un [::port ::loglevel ::jdbc-url ::cache-size]
                                    :opt-un [::user ::password ::dev-mode ::token ::join? ::persistent-databases ::jdbc-url ::dbport ::jdbc-options ::max-body]))

(s/def ::server-config (s/or :db-spec ::server-config-map :jdbc-url ::server-config-url))

;; customization end

(def config-file-path "resources/config.edn")

(defn load-config-file [config-file]
  (try
    (-> config-file slurp read-string)
    (catch java.io.FileNotFoundException e (log/info "No config file found at " config-file))
    (catch RuntimeException e (log/info "Could not validate edn in config file " config-file))))

(defn load-config
  "Loads and validates config for Datahike server. Accepts a map as config, or relative path of a config file as string."
  [config]
  (log/debug "Loading config")
  (let [arg-config (cond-> config
                     (string? config) load-config-file)
                       ;; customization start
        jdbc-url? (or (-> arg-config :server (contains? :jdbc-url))  
                      (:datahike-jdbc-url env))
        server-config (merge
                       {:port (int-from-env :datahike-jdbc-port (int-from-env :port 4000))
                        :loglevel (keyword (:datahike-jdbc-log-level env :warn))
                        :cache-size (int-from-env :datahike-jdbc-cache 100000)
                        :dev-mode (bool-from-env :datahike-jdbc-dev-mode true)}
                      (if jdbc-url?   
                       {:jdbc-url (:datahike-jdbc-url env)}
                       {:dbtype (:datahike-jdbc-dbtype env "postgresql")
                        :host (:datahike-jdbc-host env "localhost")
                        :dbport (int-from-env :datahike-jdbc-dbport 5432)
                        :dbname (:datahike-jdbc-dbname env "datahike")
                        :user (:datahike-jdbc-user env "datahike")
                        :password (:datahike-jdbc-password env "password")})
                       {:persistent-databases (:datahike-jdbc-persistent-databases env "")
                        :max-body (* (int-from-env :datahike-jdbc-max-body 16) 1024 1024)
                        :jdbc-options (-> env :datahike-jdbc-options (or "{}") edn/read-string)}
                       ;; customization end
                       (:server arg-config))
                      ;; customization start                                             
        token-config (if-let [token (keyword (:datahike-jdbc-token env))]
                       (merge
                        {:token token}
                      ;; customization end
                        server-config)
                       server-config)
        validated-server-config (if (s/valid? ::server-config token-config)
                                  token-config
                                  (throw (ex-info "Server configuration error:" (s/explain-data ::server-config token-config))))
        datahike-configs (:databases arg-config)]
    {:server validated-server-config
     :databases datahike-configs}))

(defstate config
  :start (load-config config-file-path)
  :stop {})
