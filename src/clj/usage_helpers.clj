(ns usage-helpers
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]))

(def db
  {:classname "org.h2.Driver"
   :subprotocol "h2:file"
   :subname "./gyptis-db"})

;; create an unemployment table from a csv data file
(def unemployment-table
  (do (sql/db-do-commands
       db
       "DROP TABLE unemployment"

       (str (sql/create-table-ddl :unemployment
                                  [:fips_county "int"]
                                  [:unemploy_rate "real"])
            " AS SELECT * FROM CSVREAD('classpath:/unemployment.csv')")
       "CREATE INDEX fips_index ON unemployment (fips_county)")
      "unemployment"))

;; create table that maps numeric fips codes to human-friendly names.
(def fips-codes-table
  (do
      (sql/db-do-commands
       db
       "DROP TABLE fips_codes"
       (str (sql/create-table-ddl :fips_codes
                                  [:state_abbrev "varchar(2)"]
                                  [:state_fips "int"]
                                  [:county_fips "int"]
                                  [:entity_fips "int"]
                                  [:ansi_code "int"]
                                  [:gu_name "varchar(255)"]
                                  [:entity_desc "varchar(255)"])
            " AS SELECT * FROM CSVREAD('classpath:/fips_codes_website.csv')"))

      "fips_codes"))

(def us-10m-geojson
  (as-> "resources/us-10m.geo.json" $
    (slurp $)
    (json/read-str $)
    (get $ "features")
    (map #(hash-map :id (% "id") :feature (json/write-str %)) $)))

(take 1 us-10m-geojson)

(def fips-geojson-table
  (do (sql/db-do-commands
       db
       "DROP TABLE us_10m"
       (str (sql/create-table-ddl :us_10m
                                  [:id "int"] ;fips
                                  [:feature "varchar"]))
       "CREATE INDEX id_index on us_10m (id)")
      (apply sql/insert! db :us_10m us-10m-geojson)
      "fips_geojson"))


(as-> "resources/us-10m.geo.json" $
  (slurp $)
  (json/read-str $)
  (get $ "type")
  #_(get $ "features"))
