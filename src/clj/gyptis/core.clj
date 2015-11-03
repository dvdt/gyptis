(ns gyptis.core
  (:require [gyptis.view.server :as server]
            [gyptis.view.websocket :as ws]
            [gyptis.vega-templates :as vt]
            [gyptis.util :as util]
            [ring.util.codec :refer [url-encode]]))

(def ^:dynamic *current-plot-key* :default)

(defn plot
  "Plots the given vega-spec on the browser client connected by
  the plot-key. If no client exists, a new browser tab is opened."
  ([plot-key vega-spec]
   (let [plot-key (-> plot-key
                      name
                      url-encode)]
     (when-not @server/web-server_
       (server/-main))
     (when-not (get-in @ws/connected-uids [:any plot-key])
       (server/new! :view-name plot-key)
       (alter-var-root #'*current-plot-key* (constantly plot-key)))
     (server/plot! plot-key vega-spec)))
  ([vega-spec]
   (plot *current-plot-key* vega-spec)))

(defn facet-global
  "Facets the data by the *facet-x* fand *facet-y* keys. Scales are
  the same for each subplot."
  [{:keys [scales axes marks legends] [{data :values} & more] :data :as vega-spec} & {:keys [facet-x facet-y]}]
  (binding [vt/*facet-x* (or facet-x vt/*facet-x*)
            vt/*facet-y* (or facet-y vt/*facet-y*)]
    (let [facet-keyed-data (vt/ensure-facet-keys data)]
      (-> vega-spec
          ;; Force the mark to inherit data from it's parent
          (assoc-in [:marks 0 :from :data] nil)
          (vt/facet facet-keyed-data)))))

(defn stacked-bar
  "Returns a vega.js bar plot that stacks bars with the same x position."
  [data & {:keys [x y fill] :as aes-mappings}]
  (binding [vt/*x* (or x vt/*x*)
            vt/*y* (or y vt/*y*)
            vt/*fill* (or fill vt/*fill*)]
    (->> data
         vt/->vg-data
         vt/stacked-bar
         (merge vt/top-level))))

(defn dodged-bar
  "Returns a vega.js bar plot that dodged bars with the same x position."
  [data & {:keys [x y fill facet-x facet-y] :as aes-mappings}]
  (binding [vt/*x* (or x vt/*x*)
            vt/*y* (or y vt/*y*)
            vt/*fill* (or fill vt/*fill*)]
    (->> data
         vt/->vg-data
         vt/dodged-bar
         (merge vt/top-level))))

(defn point
  "Returns a vega.js symbol plot."
  [data & {:keys [x y fill size facet-x facet-y] :as aes-mappings}]
  (binding [vt/*x* (or x vt/*x*)
            vt/*y* (or y vt/*y*)
            vt/*fill* (or fill vt/*fill*)
            vt/*size* (or size vt/*size*)]
    (->> data
         vt/->vg-data
         vt/point
         (merge vt/top-level))))

(defn line
  "Returns a vega.js line plot."
  [data & {:keys [x y stroke facet-x facet-y] :as aes-mappings}]
  (binding [vt/*x* (or x vt/*x*)
            vt/*y* (or y vt/*y*)
            vt/*stroke* (or stroke vt/*stroke*)]
    (->> data
         vt/->vg-data
         vt/line
         (merge vt/top-level))))

(defn choropleth
  "returns a vega.js map choropleth. "
  [data & {:keys [geopath fill facet-x facet-y] :as aes-mappings}]
  (binding [vt/*geopath* (or geopath vt/*geopath*)
            vt/*fill* (or fill vt/*fill*)]
    (->> data
         vt/->vg-data
         vt/choropleth
         (merge vt/top-level))))
