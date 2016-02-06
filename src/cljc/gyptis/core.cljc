(ns gyptis.core
  "Core Gyptis functions for generating declarative vega.js plot specifications"
  (:require [gyptis.vega-templates :as vt]
            [gyptis.util :as util]))

(defn facet-grid
  "Plots data with differing facet_x and facet_y values in separate
  subplots. Scales are the same for each subplot."
  [{:keys [scales axes marks legends] [{data :values} & more] :data :as vega-spec}
   & [{:keys [facet_x facet_y] :or {facet_x :facet_x
                                    facet_y :facet_y}}]]
  (if-let [gyp-spec (get (meta vega-spec) :gyptis)]
    (vt/facet-grid gyp-spec vega-spec facet_x facet_y nil)
    (throw #?(:clj  (Exception. "Facetting not implemented for this spec")
              :cljs (js/Error. "Facetting not implemented for this spec")))))

(defn stacked-bar
  "Given a coll of hashmaps or vectors, returns a vega bar plot that
  stacks bars with the same x position. For a `stacked-bar`, each
  hashmap (or vector) in data should have `x` and `y` keys as set in
  the `opts` map. `fill` is optional. The `x` values are treated as
  categorical and `y` values are treated as quantitative.

  The following example code returns a (clojure) vega.js specification
  for a bar chart with letters on the x-axis and numbers on the
  y-axis:

  (stacked-bar [{:my-x \"a\" :my-y 1}, {:my-x \"b\" :my-y 2}]
               {:x :my-x :y :my-y})"

  [[datum & more :as data] & [{:keys [x y fill]
                               :or   {x :x y :y fill :fill}
                               :as   opts}]]
  (let [vg-data (vt/->vg-data data)
        gy-spec (vt/->StackedBar vg-data)
        vg-spec (vt/->vg-spec gy-spec (merge opts {:x x :y y :fill fill}))]
    (with-meta (merge vt/top-level vg-spec) {:gyptis gy-spec})))

(defn dodged-bar
  "Like `stacked-bar`, except returns a vega bar plot that dodges bars
  with the same y position."
  [[datum & more :as data] & [{:keys [x y fill]
                               :or   {x :x y :y fill :fill}
                               :as   opts}]]
  (let [vg-data (vt/->vg-data data)
        gy-spec (vt/->DodgedBar vg-data)
        vg-spec (vt/->vg-spec gy-spec (merge opts {:x x :y y :fill fill}))]
    (with-meta (merge vt/top-level vg-spec) {:gyptis gy-spec})))

(defn point
  "Returns a vega.js symbol plot.  Each datum should have `x` and `y`
  keys, and optionally, `fill` and `size`. These keys are set in the
  opts hashmap."
  [[datum & more :as data] & [{:keys [x y fill size]
                               :or   {x :x y :y fill :fill size :size}
                               :as   opts}]]
  (let [vg-data (vt/->vg-data data)
        gy-spec (vt/->Point vg-data)
        vg-spec (vt/->vg-spec gy-spec (merge opts {:x x :y y :fill fill :size size}))]
    (with-meta (merge vt/top-level vg-spec) {:gyptis gy-spec})))

(defn line
  "Returns a vega.js symbol plot.  Each datum should have the `x`
  and `y` keys, and optionally, `stroke`."
  [[datum & more :as data] & [{:keys [x y stroke]
                               :or   {x :x y :y stroke :stroke}
                               :as   opts}]]
  (let [vg-data (vt/->vg-data data)
        gy-spec (vt/->Line vg-data)
        vg-spec (vt/->vg-spec gy-spec (merge opts {:x x :y y :stroke stroke}))]
    (with-meta (merge vt/top-level vg-spec) {:gyptis gy-spec})))

(defn choropleth
  "Returns a vega.js map choropleth. Each datum should have a
  `geopath` key that maps to a GeoJson object val and a `fill` val
  that can be represented by a quantitative scale. The
  `:geopath-transform` option is passed directly to vega."
  [[datum & more :as data] & [{fill                             :fill,
                               geopath                          :geopath,
                               {:keys [type projection center translate
                                       scale precision clipAngle]
                                :or   {type       "geopath"
                                       projection "albersUsa"}} :geopath-transform,
                               :or                              {fill :fill geopath :geopath},
                               :as                              opts}]]
  (let [geotransform {:type      type :projection projection :center center
                      :translate translate :scale scale :precision precision
                      :clipAngle clipAngle}
        raise-geopath (fn [datum] (-> datum
                                      (dissoc geopath)
                                      (merge (get datum geopath))))
        vg-data (vt/->vg-data data [:fill])
        vg-data (map raise-geopath vg-data)
        gy-spec (vt/->Choropleth vg-data)
        vg-spec (vt/->vg-spec gy-spec (merge opts {:fill              fill
                                                   :geopath-transform geotransform}))]
    (with-meta (merge vt/top-level vg-spec) {:gyptis gy-spec})))

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
                   {:type       "text"
                    :name       "title-text"
                    :from       {:data "title-text"}
                    :properties {:update
                                 {:text       {:value title-text}
                                  :fill       {:value "#000"}
                                  :x          {:value (* -1 left-padding)}
                                  :y          {:value (* -1 top-padding)}
                                  :align      {:value "left"}
                                  :baseline   {:value "top"}
                                  :fontWeight {:value "bold"}
                                  :fontSize   {:value "16"}}}}))))
