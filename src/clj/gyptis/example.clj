(ns gyptis.example
  (:require
            [clojure.java.jdbc :as sql]
            [gyptis.vega-templates :refer [bar stacked-bar dodged-bar]]
            [gyptis.view :refer [plot! clear! new!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Import a simple dataset into the h2 database

(def db
  {:classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname "diamonds2;DB_CLOSE_DELAY=-1"})

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
  (sql/db-do-commands db create-diamonds-table-from-csv))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open a browser tab connected via websocket
(def ^:dynamic *ws* (view/new!))

(def data
  (jdbc/exec! ["SELECT "]))

(intern 'veg.example '*ws* 1)

(view/plot! *ws* (bar data))
(view/clear! *ws*)
(->> ["select carat as x, price as y, cut as fill from diamonds"]
     bar
     (assoc :title )
     (view/plot! *ws*))
