
(ns datahike-server.metrics
  (:require  
   [iapetos.collector.jvm :as jvm]
   [iapetos.collector.ring :as iapetos-ring]
   [iapetos.core :as prometheus]))

(defonce registry
  (-> (prometheus/collector-registry)
      (prometheus/register
       (prometheus/histogram :datahike-server/transact-ms)
       (prometheus/histogram :datahike-server/query-ms)
       (prometheus/histogram :datahike-server/pull-ms)
       (prometheus/histogram :datahike-server/pull-many-ms)
       (prometheus/counter   :datahike-server/transact-total)
       (prometheus/counter   :datahike-server/query-total)
       (prometheus/counter   :datahike-server/pull-total)
       (prometheus/counter   :datahike-server/pull-many-total))
      (jvm/initialize)
      (iapetos-ring/initialize)))