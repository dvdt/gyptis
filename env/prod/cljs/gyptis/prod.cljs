(ns gyptis.prod
  (:require [gyptis.client :as client]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))
(client/init!)
