(ns usage-helpers
  (:require [clojure.java.jdbc :as sql]
            [clj-time.core :refer [date-time]]))

(def db
  {:classname "org.h2.Driver"
   :subprotocol "h2:mem"
   :subname "gyptis_usage;DB_CLOSE_DELAY=-1"})

(defn random-walk
  []
  (let [random (java.util.Random.)]
    (reductions + (repeatedly #(.nextGaussian random)))))

(def stock-data
  (concat
   (map (fn [day price]
          {:x (date-time 2015 1 day) :y price :fill "company A"})
        (range 1 31) (random-walk))
   (map (fn [day price]
          {:x (date-time 2015 1 day) :y price :fill "company F"})
        (range 1 31) (random-walk))
   (map (fn [day price]
          {:x (date-time 2015 1 day) :y price :fill "company G"})
        (range 1 31) (random-walk))))

(defn init-db! []
  (let [unemploy-table
        (str (sql/create-table-ddl :unemployment
                                   [:id "int"] ; fips county code
                                   [:rate "real"] ; unemployment rate
                                   )
             " AS SELECT * FROM CSVREAD('~/Downloads/trip_data_1.csv')")
        us-geo-table (str (sql/create-table-ddl :us_10m
                                                [:id "int"] ; fips county code
                                                [:geojson "clob"])
             " AS SELECT * FROM CSVREAD('~/Downloads/trip_data_1.csv')")]
    (sql/db-do-commands db create-tabe-from-csv)))
