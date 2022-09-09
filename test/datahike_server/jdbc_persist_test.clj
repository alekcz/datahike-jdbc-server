(ns ^:integration datahike-server.jdbc-persist-test
  (:require [clojure.test :refer [deftest testing is]]
            [datahike-server.database :as db]
            [datahike-server.config :as config]
            [datahike-server.jdbc :as j]
            [datahike-server.test-utils :refer [api-request]]
            [mount.core :as mount]))

(defn custom-config []
  (->  (config/load-config config/config-file-path) 
    (assoc-in [:server :persistent-databases] "persistence-aura,brilliance-aura")
    (assoc-in [:server :dev-mode] true)))

(deftest persistence-test
  (mount/start-with-states {#'datahike-server.config/config
                            {:start custom-config
                             :stop (fn [] {})}})
  (db/cleanup-databases)                             
  (is (not (some #{"persistence-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name)))))
  (is (not (some #{"brilliance-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name)))))
  (let [schema [{:db/ident :user/name  :db/valueType :db.type/string :db/cardinality :db.cardinality/one}]
        _res1 (api-request :post "/create-database" 
              {:name "persistence-aura" 
               :schema-flexibility "read" 
               :keep-history? false
               :initial-tx schema})
        _res2 (api-request :post "/create-database" 
              {:name "brilliance-aura" 
               :schema-flexibility "read" 
               :keep-history? true
               :initial-tx schema})
        _res3 (api-request :post "/create-database" 
              {:name "arcane-aura" 
               :schema-flexibility "read" 
               :keep-history? true
               :initial-tx schema})
        dbs (->> (api-request :get "/databases" nil {}) :databases (map :name))]
    (is (some #{"persistence-aura"} dbs))
    (is (some #{"brilliance-aura"} dbs))
    (is (some #{"arcane-aura"} dbs))
    (println "\n\n\n" 
      (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "persistence-aura"}}) "\n"
      (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "brilliance-aura"}}) "\n"
      (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "arcane-aura"}}) "\n\n\n")
    (is (= 3 (count (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "persistence-aura"}}))))
    (is (= 4 (count (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "brilliance-aura"}}))))
    (is (= 4 (count (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "arcane-aura"}}))))
    (mount/start-with-states {#'datahike-server.config/config
                                {:start custom-config
                                :stop (fn [] {})}
                              #'datahike-server.database/conns
                                {:start #(db/init-connections (j/connect (custom-config)))
                                 :stop (fn [] {})}})
    (is (some #{"persistence-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name))))
    (is (some #{"brilliance-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name))))
    (is (not (some #{"arcane-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name)))))
    (is (= 3 (count (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "persistence-aura"}}))))
    (is (= 4 (count (api-request :post "/datoms" {:index :eavt} {:headers {:db-name "brilliance-aura"}}))))
    (mount/start-with-states {#'datahike-server.config/config
                                {:start #(->  (config/load-config config/config-file-path) 
                                              (assoc-in [:server :dev-mode] true))
                                :stop (fn [] {})}
                              #'datahike-server.database/conns
                                {:start #(db/init-connections (j/connect (config/load-config config/config-file-path)))
                                 :stop (fn [] {})}})
    (is (not (some #{"persistence-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name)))))
    (is (not (some #{"brilliance-aura"} (->> (api-request :get "/databases" nil {}) :databases (map :name))))))
  (mount/stop))