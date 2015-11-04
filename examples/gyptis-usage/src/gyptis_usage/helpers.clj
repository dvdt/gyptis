(ns gyptis-usage.helpers
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]))

(def db
  {:classname "org.h2.Driver"
   :subprotocol "h2:file"
   :subname "./gyptis-db"})

(defn- try-drop-table
  [db table-name]
  (try (sql/execute! db
                     [(str "DROP TABLE " (name table-name))])
       (catch Exception e)))

;; create an unemployment table from a csv data file
(defonce unemployment-table
  (do (try-drop-table db :unemployment)
      (sql/db-do-commands db
       (str (sql/create-table-ddl :unemployment
                                  [:fips_county "int"]
                                  [:unemploy_rate "real"])
            " AS SELECT * FROM CSVREAD('resources/unemployment.csv')")
       "CREATE INDEX fips_index ON unemployment (fips_county)")
      "unemployment"))

;; create table that maps numeric fips codes to human-friendly names.
(defonce fips-codes-table
  (do (try-drop-table db :fips_codes)
      (sql/db-do-commands
       db
       (str (sql/create-table-ddl :fips_codes
                                  [:state_abbrev "varchar(2)"]
                                  [:state_fips "int"]
                                  [:county_fips "int"]
                                  [:entity_fips "int"]
                                  [:ansi_code "int"]
                                  [:gu_name "varchar(255)"]
                                  [:entity_desc "varchar(255)"])
            " AS SELECT * FROM CSVREAD('resources/fips_codes_website.csv')"))

      "fips_codes"))

(defonce us-10m-geojson
  (as-> "resources/us-10m.geo.json" $
    (slurp $)
    (json/read-str $)
    (get $ "features")
    (map #(hash-map :id (% "id") :feature (json/write-str %)) $)))

(defonce fips-geojson-table
  (do (try-drop-table db :us_10m)
      (sql/db-do-commands db
                          (str (sql/create-table-ddl :us_10m
                                                     [:id "int"] ;fips
                                                     [:feature "varchar"]))
                          "CREATE INDEX id_index on us_10m (id)")
      (apply sql/insert! db :us_10m us-10m-geojson)
      "us_10m"))
