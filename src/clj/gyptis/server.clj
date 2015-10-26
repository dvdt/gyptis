(ns gyptis.server
  (:require [gyptis.handler :refer [app]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (run-jetty app {:port port :join? false})))
