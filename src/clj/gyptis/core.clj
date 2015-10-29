(ns gyptis.core
  (:require [gyptis.view :as view]
            [gyptis.vega-templates :as vega]
            [gyptis.util :as util]
            [gyptis.websocket :as ws]
            [ring.util.codec :refer [url-encode]]))

(def ^:dynamic *current-plot-key* :default)

(defn plot
  "Plots the given vega-spec on the browser client connected by
  plot-key. If no client exists, a new one is created."
  ([plot-key vega-spec]
   (let [plot-key (-> plot-key
                      name
                      url-encode)]
     (when-not @view/web-server_
       (view/-main))
     (when-not (get-in @ws/connected-uids [:any plot-key])
       (view/new! :view-name plot-key)
       (alter-var-root #'*current-plot-key* (constantly plot-key)))
     (view/plot! plot-key vega-spec)))
  ([vega-spec]
   (plot vega-spec *current-plot-key*)))

(defn wrap-top-data
  [spec]
  (util/merge-spec vega/top-level spec))

(defn point
  [data]
  (let [data (vega/ensure-facet-keys data)
        point-spec (vega/point data)
        facetted-spec (vega/facet (update-in point-spec [:marks 0 :from] (constantly nil))
                                  data)]
    (wrap-top-data facetted-spec)))

(defn stacked-bar
  [data]
  (let [data (vega/ensure-facet-keys data)
        spec (-> data
                 vega/stacked-bar
                 (update-in [:data :transform 0 :groupby]
                            #(concat % [vega/*facet-x* vega/*facet-y*]))
                 (update-in [:marks 0 :from] (constantly nil))
                 (vega/facet data))]
    (wrap-top-data spec)))

(defn bar
  [data]
  (stacked-bar data))

(defn dodged-bar
  [data]
  (let [data (vega/ensure-facet-keys data)
        spec (-> data
                 vega/dodged-bar
                 #_(update-in [:marks 0 :from] (constantly nil))
                 (vega/facet data))]
    (wrap-top-data spec)))

(defn line
  [data]
  nil
  (comment (vega/facet vega/line data)))
