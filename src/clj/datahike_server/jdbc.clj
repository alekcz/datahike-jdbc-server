(ns datahike-server.jdbc
  (:require [datahike-server.config :as config]
            [taoensso.timbre :as log]
            [datahike.api :as d]
            [clojure.string :as str])
  (:import [java.util UUID]))

(def memdb {:store {:backend :mem
                    :id "default"}
            :schema-flexibility :read
            :keep-history? false
            :name "default"
            :attribute-refs? false,
            :cache-size 300,
            :index :datahike.index/hitchhiker-tree,
            :index-config {:index-b-factor 17, :index-data-node-size 300, :index-log-size 283}})

(defn prepare-config [cfg]
  (let [res (if (-> cfg :store :jdbcUrl str/blank?)
              cfg
              (assoc cfg :store (dissoc (:store cfg) :host :dbname :port :user :password)))]
    (println res)
    res))

(defn prepare-databases [{:keys [server databases] :as configuration}]
  (assoc configuration
    :databases
    (vec
      (for [cfg databases]
        (prepare-config
          (if (-> cfg :store :backend (not= :jdbc))
              cfg 
              (-> (assoc cfg :cache-size (-> server :cache-size))
                  (assoc :store  (merge
                                  {:backend :jdbc 
                                   :dbtype (-> server :dbtype)
                                   :jdbcUrl (-> server :jdbcUrl)
                                   :host (-> server :host)
                                   :port (-> server :dbport)
                                   :user (-> server :user)
                                   :password (-> server :password)
                                   :dbname (-> server :dbname)
                                   :table (-> cfg :name)}
                                  (-> server :jdbc-options))))))))))

(defn connect [config]
  (let [persistent (-> config :server :persistent-databases)
        initial-db-names (when-not (str/blank? persistent) (str/split persistent #","))
        initial-dbs (for [n initial-db-names] 
                      (prepare-config
                        {:store  (merge
                                    {:backend :jdbc 
                                     :dbtype (-> config :server :dbtype)
                                     :jdbcUrl (-> config :server :jdbcUrl)
                                     :host (-> config :server :host)
                                     :port (-> config :server :dbport)
                                     :user (-> config :server :user)
                                     :password (-> config :server :password)
                                     :dbname (-> config :server :dbname)
                                     :table n}
                                    (-> config :server :jdbc-options))
                         :name n
                         :cache-size (-> config :server :cache-size)}))
        valid-dbs (filter d/database-exists? initial-dbs)
        final-dbs (-> config :databases (concat valid-dbs))
        final-config (assoc config :databases final-dbs)]
    (prepare-databases final-config)))

(defn add-database [{:keys [name keep-history? schema-flexibility initial-tx]} config]
  (let [cfg' { :store (merge
                      {:backend :jdbc 
                       :dbtype (-> config :server :dbtype)
                       :jdbcUrl (-> config :server :jdbcUrl)
                       :host (-> config :server :host)
                       :port (-> config :server :dbport)
                       :user (-> config :server :user)
                       :password (-> config :server :password)
                       :dbname (-> config :server :dbname)
                       :table name}
                      (-> config :server :jdbc-options))
              :name name
              :keep-history? keep-history?
              :schema-flexibility (keyword schema-flexibility)
              :cache-size (-> config :server :cache-size)
              :initial-tx initial-tx}
        cfg (prepare-config cfg')
        exists? (d/database-exists? cfg)]
    (when-not exists?
      (log/infof "Creating database...")
      (d/create-database cfg)
      (log/infof "Done"))
    [(not exists?) (d/connect cfg)]))

(defn delete-database [{:keys [name delete?]} config]
  (let [cfg' { :store  (merge
                        {:backend :jdbc 
                         :dbtype (-> config :server :dbtype)
                         :jdbcUrl (-> config :server :jdbcUrl)
                         :host (-> config :server :host)
                         :port (-> config :server :dbport)
                         :user (-> config :server :user)
                         :password (-> config :server :password)
                         :dbname (-> config :server :dbname)
                         :table name}
                        (-> config :server :jdbc-options))
              :name name}
        cfg (prepare-config cfg')]
    (log/infof (str "Deleting database " name "..."))
    (when delete?
      (when (d/database-exists? cfg) 
        (d/delete-database cfg)))))