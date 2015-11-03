(ns usage
  (:require [gyptis.core :refer :all]
            [gyptis.vega-templates :refer [vertical-x-labels] :as vt]
            [clojure.data.json :as json]))

(def bar-data [{:x "a", :y  1 :fill 1}
               {:x "a", :y  1 :fill 2}
               {:x "b", :y  2 :fill 1}
               {:x "b", :y  3 :fill 2}
               {:x "c", :y  5 :fill 1}
               {:x "c", :y  8 :fill 2}
               {:x "d", :y 13 :fill 1}
               {:x "d", :y 21 :fill 2}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a bar chart

(plot (dodged-bar bar-data))

;; plot every hashmap in `data' as a rectangle. Stacks bars with the same `x' value
(plot (stacked-bar bar-data))


;; add labels
(plot (-> bar-data stacked-bar
          (assoc-in [:axes 0 :title] "letter")
          (assoc-in [:axes 1 :title] "rate")))

;; add mouse-over interactions
(plot (-> bar-data stacked-bar
          (assoc-in [:marks 0 :properties :hover] {:fill {:value "red"}})))

;; Subplots are easy
(binding [vt/*facet-x* :fill]
  (-> bar-data
      stacked-bar
      facet-global
      plot))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Works on time series.

(def time-series-data
  [{:x (java.util.Date. "Jan 1, 2015"), :y 1 :stroke "a"}
   {:x (java.util.Date. "Jan 2, 2015"), :y 2 :stroke "a"}
   {:x (java.util.Date. "Jan 3, 2015"), :y 4 :stroke "a"}
   {:x (java.util.Date. "Jan 4, 2015"), :y 9 :stroke "a"}

   {:x (java.util.Date. "Jan 1, 2015"), :y 2 :stroke "b"}
   {:x (java.util.Date. "Jan 2, 2015"), :y 3 :stroke "b"}
   {:x (java.util.Date. "Jan 3, 2015"), :y 4 :stroke "b"}
   {:x (java.util.Date. "Jan 4, 2015"), :y 5 :stroke "b"}])

(-> time-series-data
    line
    plot)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plays well with jdbc
;; Unifying data across multiple data sources is easy with SQL :D

(require '[clojure.java.jdbc :as sql])
(require '[usage-helpers :refer [db]])

;; These examples use an embedded H2 database.
;; Of course, it's simple to use other db engines.

(sql/query db ["SELECT * FROM unemployment limit 1"])

(plot (-> (sql/query db ["SELECT fips_county as x, unemploy_rate as y
                          FROM unemployment
                          ORDER BY unemploy_rate DESC LIMIT 20"])
          stacked-bar
          vertical-x-labels))

(def highest-unemployment-counties
  (sql/query db
             ["SELECT CONCAT_WS(', ', fips_codes.gu_name, fips_codes.state_abbrev) as x,
               unemploy_rate as y
               FROM fips_codes
               INNER JOIN unemployment ON (1000*fips_codes.state_fips + fips_codes.county_fips) = unemployment.fips_county
               WHERE fips_codes.entity_desc='County'
               ORDER BY y DESC LIMIT 25"]))

;; counties with highest unemployment rate
(plot (-> highest-unemployment-counties
          stacked-bar
          vertical-x-labels))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a map

(def county-unemployment-geo
  (sql/query db [(str "SELECT feature as geopath, unemploy_rate as fill "
                      "FROM unemployment "
                      "INNER JOIN us_10m ON us_10m.id=unemployment.fips_county ")]
             :row-fn #(update % :geopath json/read-str)))

(-> county-unemployment-geo
    (choropleth :projection "albersUsa")
    plot)
