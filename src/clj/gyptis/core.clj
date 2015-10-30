(ns gyptis.core
  (:require [gyptis.view :as view]
            [gyptis.vega-templates :as vega]
            [gyptis.util :as util]
            [gyptis.websocket :as ws]
            [ring.util.codec :refer [url-encode]]))

(def ^:dynamic *current-plot-key* :default)

(defn wrap-dims
  [spec]
  (util/merge-spec vega/top-level spec))

(defn facet-global
  "Facets the data by the *facet-x* fand *facet-y* keys. Scales are
  the same for each subplot."
  [{:keys [scales axes marks legends] [{data :values} & more] :data :as vega-spec}]
  (let [facet-keyed-data (vega/ensure-facet-keys data)]
    (prn facet-keyed-data)
    (wrap-dims (vega/facet (update-in vega-spec [:marks 0 :from :data] (constantly nil))
                           facet-keyed-data))))

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
     (view/plot! plot-key
                 (-> vega-spec facet-global))))
  ([vega-spec]
   (plot vega-spec *current-plot-key*)))

(defn point
  [data]
  (let [data (vega/ensure-facet-keys data)
        point-spec (vega/point data)
        facetted-spec (vega/facet (update-in point-spec [:marks 0 :from] (constantly nil))
                                  data)]
    (wrap-dims facetted-spec)))

(defn stacked-bar
  [data]
  (let [data (vega/ensure-facet-keys data)
        spec (-> data
                 vega/stacked-bar
                 #_(update-in [:data 1 :transform :groupby]
                            #(concat % [vega/*facet-x* vega/*facet-y*]))
                 (vega/facet data))]
    (wrap-dims spec)))

(defn bar
  [data]
  (stacked-bar data))

(defn dodged-bar
  [data]
  (let [data (vega/ensure-facet-keys data)
        spec (-> data
                 vega/dodged-bar
                 (vega/facet data))]
    (wrap-dims spec)))

(defn line
  [data]
  nil
  (comment (vega/facet vega/line data)))
