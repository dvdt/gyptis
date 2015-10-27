(ns gyptis.example
  (:require
            [clojure.java.jdbc :as sql]
            [gyptis.vega-templates :refer [bar stacked-bar dodged-bar point] :as vega]
            [gyptis.util :refer [merge-spec ensure-key]]
            [gyptis.view :refer [plot! clear! new!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Import a simple dataset into the h2 database

(def db
  {:classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname "diamonds2;DB_CLOSE_DELAY=-1"})

(defn init-diamonds! []
  (let [create-diamonds-table-from-csv
        (str (sql/create-table-ddl :diamonds
                                   [:id "int primary key auto_increment"]
                                   [:carat "real"]
                                   [:cut "varchar(32)"]
                                   [:color "varchar(32)"]
                                   [:clarity "varchar(32)"]
                                   [:depth "real"]
                                   [:table "real"]
                                   [:price "real"]
                                   [:x "real"]
                                   [:y "real"]
                                   [:z "real"])
             " AS SELECT * FROM CSVREAD('classpath:/diamonds.csv')")]
    (sql/db-do-commands db create-diamonds-table-from-csv)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open a browser tab connected via websocket
#_(def ^:dynamic *ws* (view/new!))

#_(def data
  (jdbc/exec! ["SELECT "]))

(comment (view/plot! *ws* (bar data))
         (view/clear! *ws*)
         (->> ["select carat as x, price as y, cut as fill from diamonds"]
              bar
              (assoc :title )
              (view/plot! *ws*)))

(defn assoc-defaults
  "Ensures that undefined doesn't happen."
  [value]
  (->> value
       (ensure-key vega/*facet-x* "")
       (ensure-key vega/*facet-y* "")))

(def p (new! :view-name "afaf"))

(let [data
      (sql/query db
                 ["SELECT price as y, carat as x, cut as fill from diamonds limit 120;"])
      data (map assoc-defaults data)
      spec (vega/facet point data)
      spec (merge-spec
            spec
            {:width 1400
             :height 600
             :padding {:top 100, :left 100, :bottom 100, :right 100}}
            {:data ^:replace [{:name "table" :values data}]})]
  (plot! p spec))
