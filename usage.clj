(ns usage
  (:require [gyptis.core :refer :all]
            [gyptis.vega-templates :as vega]))

(def data [{:x "a", :y 5 :facet_y "a" :fill 1}
           {:x "a", :y 5 :facet_y "a" :fill 2}
           {:x "b", :y 10 :facet_y "a" :fill 1}
           {:x "c", :y 2 :facet_y "a" :fill 2}
           {:x "d", :y 3 :facet_y "c" :fill 2}])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a bar chart

(plot :bar-plot
      (vega/dodged-bar data))

;; plot every hashmap in `data' as a rectangle. Stacks bars with the same `x' value
(plot :bar-plot
      (vega/stacked-bar data))

;; add labels
(plot :bar-plot
      (-> data vega/point
          (assoc-in [:axes 0 :title] "letter")
          (assoc-in [:axes 1 :title] "rate")))

;; add mouse-over interactions
(plot
 :bar-plot
 (-> (vega/bar data)
     (assoc-in [:marks 0 :properties :hover]
               {:fill {:value "red"}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Works on time series.

(def time-series-data
  [{:x (java.util.Date. "Jan 1, 2015"), :y 4 :stroke "a"}
   {:x (java.util.Date. "Jan 2, 2015"), :y 0 :stroke "a"}
   {:x (java.util.Date. "Jan 3, 2015"), :y 1 :stroke "a"}
   {:x (java.util.Date. "Jan 4, 2015"), :y 3 :stroke "a"}
   {:x (java.util.Date. "Jan 1, 2015"), :y 3 :stroke "b"}
   {:x (java.util.Date. "Jan 2, 2015"), :y 10 :stroke "b"}
   {:x (java.util.Date. "Jan 3, 2015"), :y 9 :stroke "b"}
   {:x (java.util.Date. "Jan 4, 2015"), :y 5 :stroke "b"}])

(plot :time-series
      (-> time-series-data vega/->vg-data vega/line
          (assoc :width 400)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plays well with jdbc

(require '([clojure.java.jdbc :as sql]))
(require 'gyptis.usage-helpers :as h)

(def unemployment-rate-by-county-id
  (sql/query h/db [(str "SELECT id as x,"
                        "rate as y "
                        "FROM unemployment"
                        "ORDER BY y desc"
                        "LIMIT 20")]))

(plot! :bar-plot (stacked-bar unemployment-rate-by-county-id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a map

(def county-unemployment-geo
  (sql/query h/db [(str "SELECT us_10m.state as layout_path,"
                        "rate as fill"
                        "FROM unemployment"
                        "WHERE us_10m.state=state")]))

(plot! "unemploy-map"
       (choropleth unemployment-rate-by-county))
