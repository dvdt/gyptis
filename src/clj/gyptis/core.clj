(ns gyptis.core
  (:require [gyptis.vega-templates :as vt]
            [gyptis.util :as util]))

(defn facet-global
  "Facets the data by the *facet-x* fand *facet-y* keys. Scales are
  the same for each subplot."
  [{:keys [scales axes marks legends] [{data :values} & more] :data :as vega-spec}]
  (let [facet-keyed-data (vt/ensure-facet-keys data)]
    (-> vega-spec
        ;; Force the mark to inherit data from it's parent
        (assoc-in [:marks 0 :from :data] nil)
        (vt/facet facet-keyed-data))))

(defn stacked-bar
  "Returns a vega.js bar plot that stacks bars with the same x position."
  [data]
  (->> data
       vt/->vg-data
       vt/stacked-bar
       (merge vt/top-level)))

(defn dodged-bar
  "Returns a vega.js bar plot that dodged bars with the same x position."
  [data]
  (->> data
       vt/->vg-data
       vt/dodged-bar
       (merge vt/top-level)))

(defn point
  "Returns a vega.js symbol plot."
  [data]
  (->> data
       vt/->vg-data
       vt/point
       (merge vt/top-level)))

(defn line
  "Returns a vega.js line plot."
  [data]
  (->> data
       vt/->vg-data
       vt/line
       (merge vt/top-level)))

(defn choropleth
  "returns a vega.js map choropleth. "
  [data & {:keys [type projection center translate scale
                  precision clipAngle]
           :or {type "geopath" projection "albersUsa"}}]
  (let [geotransform {:type type :projection projection :center center
                      :translate translate :scale scale :precision precision
                      :clipAngle clipAngle}]
    (as-> data $
      (vt/->vg-data $)
      (vt/choropleth $ geotransform)
      (merge vt/top-level $))))
