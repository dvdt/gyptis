(ns gyptis.core
  "Core Gyptis functions for generating declarative vega.js plot specifications"
  (:require [gyptis.vega-templates :as vt]
            [gyptis.util :as util]))

(defn facet-global
  "Plots data with differing (`gyptis.vega-templates/*facet-x*`,
  `gyptis.vega-templates/*facet-y*`) values in separate
  subplots. Scales are the same for each subplot (hence 'global')."
  [{:keys [scales axes marks legends] [{data :values} & more] :data :as vega-spec}]
  (let [facet-keyed-data (vt/ensure-facet-keys data)]
    (-> vega-spec
        ;; Force the mark to inherit data from it's parent
        (assoc-in [:marks 0 :from :data] nil)
        (vt/facet facet-keyed-data))))

(defn stacked-bar
  "Given a coll of hashmaps or vectors, returns a vega bar plot that
  stacks bars with the same x position. For a `stacked-bar`, each
  hashmap (or vector) in data should have `*x*` and `*y*` keys from
  the gyptis.vega-templates namespace.  `*fill*` is optional. The
  `*x*` values are treated as categorical and `*y*` values are treated
  as quantitative.

  The following example code returns a (clojure) vega.js specification
  for a bar chart with letters on the x-axis and numbers on the
  y-axis:

  (binding [gyptis.vega-templates/*x* :my-x
            gyptis.vega-templates/*y* :my-y]
    (stacked-bar [{:my-x \"a\" :my-y 1}
                  {:my-x \"b\" :my-y 2}
                  {:my-x \"c\" :my-y 3}])) "
  [[datum & more :as data]]
  (->> data
       vt/->vg-data
       vt/stacked-bar
       (merge vt/top-level)))

(defn dodged-bar
  "Like `stacked-bar`, except returns a vega bar plot that dodges bars
  with the same y position."
  [[datum & more :as data]]
  (->> data
       vt/->vg-data
       vt/dodged-bar
       (merge vt/top-level)))

(defn point
  "Returns a vega.js symbol plot.  Each datum should have `*x*`
  and `*y*` keys, and optionally, `*fill*` and `*size*`."
  [[datum & more :as data]]
  (->> data
       vt/->vg-data
       vt/point
       (merge vt/top-level)))

(defn line
  "Returns a vega.js symbol plot.  Each datum should have the `*x*`
  and `*y*` keys, and optionally, `*stroke*`."
  [[datum & more :as data]]
  (->> data
       vt/->vg-data
       vt/line
       (merge vt/top-level)))

(defn choropleth
  "Returns a vega.js map choropleth. Each datum should have a
  `*geojson*` key that maps to a GeoJson object val; and a `*fill*`."
  [[datum & more :as data] & {:keys [type projection center translate scale
                  precision clipAngle]
           :or {type "geopath" projection "albersUsa"}}]
  (let [geotransform {:type type :projection projection :center center
                      :translate translate :scale scale :precision precision
                      :clipAngle clipAngle}]
    (as-> data $
      (vt/->vg-data $)
      (vt/choropleth $ geotransform)
      (merge vt/top-level $))))

(defn title
  "Adds data and a text mark to the given spec to display the title-text."
  [spec title-text]
  (let [not-title (partial filter #(not= "title-text" (:name %)))
        title-free-spec (-> spec
                            (update :data (comp vec not-title))
                            (update :marks (comp vec not-title)))
        left-padding (or (get-in spec [:padding :left])
                         (get-in vt/top-level [:padding :left]))
        top-padding (or (get-in spec [:padding :top])
                        (get-in vt/top-level [:padding :top]))]
    (-> title-free-spec
        (update-in [:data] conj {:name "title-text" :values [{:a "a"}]})
        (update-in [:marks] conj
                   {:type "text"
                    :name "title-text"
                    :from {:data "title-text"}
                    :properties {:update
                                 {:text {:value title-text}
                                  :fill {:value "#000"}
                                  :x {:value (* -1 left-padding)}
                                  :y {:value (* -1 top-padding)}
                                  :align {:value "left"}
                                  :baseline {:value "top"}
                                  :fontWeight {:value "bold"}
                                  :fontSize {:value "16"}}}}))))
