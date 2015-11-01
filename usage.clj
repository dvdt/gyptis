(ns usage
  (:require [gyptis.core :refer :all]
            [gyptis.view :refer [plot!]]
            [gyptis.vega-templates :as vt]
            [gyptis.validate :refer [valid?]]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]))

(def bar-data [{:x "a", :y 5 :facet_y "a" :fill 1}
               {:x "a", :y 5 :facet_y "a" :fill 2}
               {:x "b", :y 9 :facet_y "a" :fill 1}
               {:x "c", :y 2 :facet_y "a" :fill 2}
               {:x "d", :y 3 :facet_y "c" :fill 2}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a bar chart

(plot (vt/dodged-bar bar-data))

;; plot every hashmap in `data' as a rectangle. Stacks bars with the same `x' value
(plot (vt/stacked-bar bar-data))

;; add labels
(plot (-> bar-data vt/stacked-bar
          (assoc-in [:axes 0 :title] "letter")
          (assoc-in [:axes 1 :title] "rate")))

;; add mouse-over interactions
(plot (-> bar-data vt/stacked-bar
          (assoc-in [:marks 0 :properties :hover]
                    {:fill {:value "red"}})))

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

(plot (-> time-series-data vt/->vg-data vt/line
          (assoc :width 600)))

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
                          ORDER BY unemploy_rate DESC LIMIT 10"])
          vt/dodged-bar))

(def highest-unemployment-counties
  (sql/query db
             ["SELECT CONCAT_WS(', ', fips_codes.gu_name, fips_codes.state_abbrev) as x,
               unemploy_rate as y
               FROM fips_codes
               INNER JOIN unemployment ON (1000*fips_codes.state_fips + fips_codes.county_fips) = unemployment.fips_county
               WHERE fips_codes.entity_desc='County'
               ORDER BY y DESC LIMIT 20"]))

;; counties with highest unemployment rate
(plot (-> highest-unemployment-counties
          vt/stacked-bar
          vertical-x-labels))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a map

(def county-unemployment-geo
  (sql/query db [(str "SELECT feature as geopath, unemploy_rate as fill from unemployment
INNER JOIN us_10m ON us_10m.id=unemployment.fips_county")]
             :row-fn #(update % :geopath json/read-str)))

(plot (vt/choropleth county-unemployment-geo))
