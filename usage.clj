(ns usage)

(use 'gyptis.core)

(use 'clj-time.core)

(def data [{:x "a", :y 0.1 :facet_y "a"}
           {:x "b", :y 1 :facet_y "a"}
           {:x "c", :y 2 :facet_y "a"}
           {:x "d", :y 3 :facet_y "c"}])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's make a bar chart

;; plot every hashmap in `data' as a point
(plot :bar-plot
      (point data))

(plot :bar-plot
      (point data))

;; plot every hashmap in `data' as a rectangle. Stacks bars with the same `x' value
(plot :bar-plot
      (bar data))

;; dodge bars with the same `x' value
(plot :bar-plot
       (dodged-bar data))

;; add labels
(plot :bar-plot
       (-> (dodged-bar data)
           (assoc-in [:labels :x] "hii")
           (assoc-in [:labels :y] "freq")))

;; add mouse-over interactions
(plot
 :bar-plot
 (-> (bar data)
     (assoc-in [:marks 0 :properties :hover]
               {:fill {:value "red"}})))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Works on time series.

(require 'clj-time.core :refer [date-time])

(def time-series-data
  [{:x (date-time 2015 1 1), :y 100 :fill ""}
   {:x (date-time 2015 1 1), :y 0 :fill "fb"}
   {:x (date-time 2015 1 2), :y 1}
   {:x (date-time 2015 1 3), :y 3}])

(plot! :
       (line time-data))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plays well with jdbc

(require 'clojure.java.jdbc :as sql)
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
