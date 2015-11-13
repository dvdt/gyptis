(ns gyptis-usage.core
  (:require [gyptis.core :refer :all]
            [gyptis.vega-templates :refer [vertical-x-labels] :as vt]
            [gyptis.view :refer [plot!]]
            [clojure.data.json :as json]))

(def data [{:x "n=2", :y 1 :fill "n-1"}
           {:x "n=2", :y 0 :fill "n-2"}
           {:x "n=3", :y 1 :fill "n-1"}
           {:x "n=3", :y 1 :fill "n-2"}
           {:x "n=4", :y 2 :fill "n-1"}
           {:x "n=4", :y 1 :fill "n-2"}
           {:x "n=5", :y 3 :fill "n-1"}
           {:x "n=5", :y 2 :fill "n-2"}
           {:x "n=6", :y 5 :fill "n-1"}
           {:x "n=6", :y 3 :fill "n-2"}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a bar chart

;; plot every hashmap in `data' as a rectangle. Stacks bars with the same `x' value
(plot! (dodged-bar data))

(plot! (stacked-bar data)) ; data is the fibonacci sequence!

;; add labels
(plot! (-> data stacked-bar
           (assoc-in [:axes 0 :title] "n")
           (assoc-in [:axes 1 :title] "fibonacci(n)")
           (title "Fibonacci: F(2)=1, F(3)=2, F(4)=3, F(5)=5, F(6)=8")))

;; add mouse-over interactions
(plot! (-> *1 ; `plot!' returns the generated spec which the REPL then binds to *1
           (assoc-in [:marks 0 :properties :hover] {:fill {:value "red"}})
           (title "Fibonacci with hover interactions")))

;; Subplots are easy
(-> data
    stacked-bar
    (facet-global {:facet_x :fill})
    (title "Facetting by the 'fill'")
    plot!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Works on time series.

(def time-series-data
  [{:x (java.util.Date. "Jan 1, 2015"), :y 1 :stroke "a"}
   {:x (java.util.Date. "Jan 2, 2015"), :y 4 :stroke "a"}
   {:x (java.util.Date. "Jan 3, 2015"), :y 9 :stroke "a"}
   {:x (java.util.Date. "Jan 4, 2015"), :y 16 :stroke "a"}

   {:x (java.util.Date. "Jan 1, 2015"), :y 2 :stroke "b"}
   {:x (java.util.Date. "Jan 2, 2015"), :y 3 :stroke "b"}
   {:x (java.util.Date. "Jan 3, 2015"), :y 4 :stroke "b"}
   {:x (java.util.Date. "Jan 4, 2015"), :y 5 :stroke "b"}])

(-> time-series-data
    line
    plot!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plays well with jdbc
;; Unifying data across multiple data sources is easy with SQL :D

(require '[clojure.java.jdbc :as sql])
(require '[gyptis-usage.helpers :refer [db]])

;; These examples use an embedded H2 database.
;; Of course, it's simple to use other db engines.

(sql/query db ["SELECT * FROM unemployment limit 1"])
(sql/query db ["SELECT * FROM fips_codes limit 1"])

;; Top 20 most unemployed counties
(plot! (-> (sql/query db ["SELECT fips_county as x, unemploy_rate as y
                          FROM unemployment
                          ORDER BY unemploy_rate DESC LIMIT 20"])
           stacked-bar
           (title "Most unemployed counties")
           vertical-x-labels))

(def highest-unemployment-counties-by-name
  (sql/query db
             ["SELECT CONCAT_WS(', ', fips_codes.gu_name, fips_codes.state_abbrev) as x,
               unemploy_rate as y
               FROM fips_codes
               INNER JOIN unemployment ON (1000*fips_codes.state_fips + fips_codes.county_fips) = unemployment.fips_county
               WHERE fips_codes.entity_desc='County'
               ORDER BY y DESC LIMIT 25"]))

;; counties with highest unemployment rate
(plot! (-> highest-unemployment-counties-by-name
          stacked-bar
          vertical-x-labels
          (title "Most unemployed counties")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a map

(def county-unemployment-geo
  (sql/query db [(str "SELECT us_10m.feature as geopath, unemployment.unemploy_rate as fill "
                      "FROM unemployment "
                      "INNER JOIN us_10m ON us_10m.id=unemployment.fips_county ")]
             :row-fn #(update % :geopath json/read-str)))

(-> county-unemployment-geo
    (choropleth {:geopath-transform {:projection "albersUsa"
                                     :scale 800
                                     :translate [200 200]}})
    (title "US unemployment rates")
    plot!
    count)
