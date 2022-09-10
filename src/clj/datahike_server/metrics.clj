
(ns datahike-server.metrics
  (:require  
   [iapetos.collector.jvm :as jvm]
   [iapetos.collector.ring :as iapetos-ring]
   [iapetos.core :as prometheus]))

(defonce registry
  (-> (prometheus/collector-registry)
      (prometheus/register
       (prometheus/histogram :app/dh-transact-ms)
       (prometheus/histogram :app/dh-query-ms)
       (prometheus/histogram :app/dh-pull-ms)
       (prometheus/histogram :app/dh-pull-many-ms)
       (prometheus/counter   :app/dh-transact-total)
       (prometheus/counter   :app/dh-query-total)
       (prometheus/counter   :app/dh-pull-total)
       (prometheus/counter   :app/dh-pull-many-total))
      (jvm/initialize)
      (iapetos-ring/initialize)))