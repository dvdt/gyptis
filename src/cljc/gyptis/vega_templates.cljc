(ns gyptis.vega-templates
  "Helper functions that produce and manipulate vega.js plot declarations."
  (:require [gyptis.util :refer [merge-spec] :as u]))

(def ^:dynamic top-level
  {:width   600
   :height  600
   :padding {:top 50, :left 100, :bottom 100, :right 50}
   :data    []
   :scales  []
   :axes    []
   :marks   []
   :legends []
   :signals []})

(defprotocol VegaSpec
  (->vg-spec [this opts] "Returns a clj hashmap of a vega plot specification."))

(defprotocol FacetGrid
  "Facetting plots is to make many subplots. Also known as small multiples or trellis plots."
  (facet-grid [this spec facet-x facet-y {:keys [facet_x facet_y]}]))

(defn- first-non-nil
  [data field]
  (->> data
       (map #(get % field))
       (drop-while nil?)
       first))

(defn guess-scale-type
  "Returns \"linear\" or \"ordinal\" or \"time\" or throws an exception."
  [data field]
  (if-let [meta-type (-> data meta (get field))]
    ;; first check the metadata on data for scale-type info
    meta-type
    ;; otherwise try to guess
    (when-let [d (first-non-nil data field)]
      (cond
        (u/date? d) "time"
        (number? d) "linear"
        (or (string? d) (nil? d)) "ordinal"
        :else (throw #?(:cljs
                        (js/Error. (str "Can't guess scale type for " d ", type= " (type d)))
                        :clj
                        (Exception. (str "Can't guess scale type for " d))))))))

(defn suppress-axis-labels
  [axis]
  (-> axis
      (assoc-in [:properties :labels :text :value] "")
      (assoc-in [:title] "")))

(defn vertical-x-labels
  "Returns a vega.js spec with x-axis labels oriented vertically."
  [spec]
  (-> spec
      (assoc-in [:axes 0 :properties :labels :angle :value] 270)
      (assoc-in [:axes 0 :properties :labels :align :value] "right")
      (assoc-in [:axes 0 :properties :labels :dx :value] -5)
      (assoc-in [:axes 0 :properties :labels :dy :value] -5)))

(defn ensure-facet-keys
  [data facet_x facet_y]
  (map (fn [datum]
         (->> datum
              (u/ensure-key facet_x "")
              (u/ensure-key facet_y ""))) data))

(defn- ->facet-mark
  [inner-spec data facet_x facet_y]
  {:type       "group"
   :from       {:data      "table"
                :transform [{:type "facet" :groupby [facet_x facet_y]}]}
   :properties {:enter {:x      {:scale "facet_x_scale" :field facet_x}
                        :y      {:scale "facet_y_scale" :field facet_y}
                        :height {:scale "facet_y_scale" :band true}
                        :width  {:scale "facet_x_scale" :band true}}}
   :marks      (:marks inner-spec)
   :scales     (:scales inner-spec)
   :axes       (:axes inner-spec)})

(defn- add-facet-axes
  "Returns a vector of facetted group marks with axes legends in the
  bottom row and first column"
  [facet-mark [datum & more :as data] facet_x facet_y]
  (let [first-facet-x (->> facet_x (get datum) u/->json)
        last-facet-y (->> data
                          (map #(get % facet_y))
                          u/last-unique-elem
                          u/->json)
        unlabelled-facet-pred (str "datum['" (name facet_x) "'] !==" first-facet-x
                                   " && datum['" (name facet_y) "'] !==" last-facet-y)
        x-label-facet-pred (str "datum['" (name facet_x) "'] !==" first-facet-x
                                " && datum['" (name facet_y) "'] ===" last-facet-y)
        y-label-facet-pred (str "datum['" (name facet_x) "'] ===" first-facet-x
                                " && datum['" (name facet_y) "'] !==" last-facet-y)
        xy-label-facet-pred (str "datum['" (name facet_x) "'] ===" first-facet-x
                                 " && datum['" (name facet_y) "'] ===" last-facet-y)

        unlabelled-facet-mark
        (-> facet-mark
            (update-in [:axes] #(mapv suppress-axis-labels %))
            (update-in [:from :transform] conj {:type "filter"
                                                :test unlabelled-facet-pred}))
        x-labelled-facet-mark
        (-> facet-mark
            (assoc-in [:from :transform 1] {:type "filter"
                                            :test x-label-facet-pred})
            (update-in [:axes] (partial mapv
                                        (fn [{:keys [type scale properties] :as axis}]
                                          (if (= type "y") (suppress-axis-labels axis) axis)))))
        y-labelled-facet-mark
        (-> facet-mark
            (assoc-in [:from :transform 1] {:type "filter"
                                            :test y-label-facet-pred})
            (update-in [:axes] (partial mapv
                                        (fn [{:keys [type scale properties] :as axis}]
                                          (if (= type "x") (suppress-axis-labels axis) axis)))))
        xy-labelled-facet-mark
        (-> facet-mark
            (assoc-in [:from :transform 1] {:type "filter"
                                            :test xy-label-facet-pred}))]
    [unlabelled-facet-mark x-labelled-facet-mark y-labelled-facet-mark xy-labelled-facet-mark]))

(defn facet
  "Takes a vega spec and produces a vega spec with a facetted layout
  based on the facet_x and facet_y fields."
  [inner-spec [datum & more :as data] facet_x facet_y]
  {:pre [(contains? datum facet_x) (contains? datum facet_y)]}
  (let [facetted-marks (add-facet-axes (->facet-mark inner-spec data facet_x facet_y)
                                       data facet_x facet_y)]
    (merge (select-keys inner-spec [:width :height :padding])
           {:data    (into [{:name "table" :values data}] (rest (:data inner-spec)))
            :legends (:legends inner-spec)
            :scales  (vec (concat [{:name    "facet_x_scale"
                                    :type    "ordinal"
                                    :padding 0.15
                                    :range   "width"
                                    :domain  {:data "table" :field facet_x}}
                                   {:name    "facet_y_scale"
                                    :type    "ordinal"
                                    :padding 0.15
                                    :range   "height"
                                    :domain  {:data "table" :field facet_y}}]
                                  (:scales inner-spec)))
            :axes    [{:type       "x" :scale "facet_x_scale" :orient "top" :tickSize 0
                       :properties {:axis {:strokeWidth {:value 0}}}}
                      {:type       "y" :scale "facet_y_scale" :orient "right" :tickSize 0
                       :properties {:axis {:strokeWidth {:value 0}}}}]
            :marks   facetted-marks})))

(defn assoc-hover
  [vg hover-spec]
  (assoc-in vg [:marks 0 :properties :hover] hover-spec))

(defn ->vg-data*
  [data field]
  (case (guess-scale-type data field)
    "time" (let [millis-time (map #(update % field u/->epoch-millis) data)]
             (vary-meta millis-time assoc field "time"))
    "linear" (vary-meta data assoc field "linear")
    "ordinal" (vary-meta data assoc field "ordinal")))

(defn ->vg-data
  "Converts objects like Date into data that vega.js understands."
  ([data]
   (->vg-data data (-> data first keys)))
  ([data fields]
   (reduce (fn [acc, field]
             (->vg-data* acc field))
           data fields)))

(defn bar
  [data x y]
  {:pre [(contains? (first data) x) (contains? (first data) y)]}
  {:data   [{:name "table" :values data}]
   :scales [{:name   "x",
             :type   "ordinal",
             :range  "width",
             :domain (u/dedup (map #(get % x) data))}
            {:name   "y",
             :type   "linear",
             :range  "height",
             :domain {:data "table", :field y},
             :nice   true
             :round  true}],
   :axes   [{:type "x", :scale "x"} {:type "y", :scale "y"}],
   :marks
           [{:type "rect",
             :properties
                   {:update
                    {:x     {:scale "x", :field x},
                     :width {:scale "x", :band true, :offset -1},
                     :y     {:scale "y", :field y},
                     :y2    {:scale "y", :value 0}
                     :fill  {:value "steelblue"}}}}]})

(defrecord Bar
  [data]
  VegaSpec
  (->vg-spec
    [this {:keys [x y]}]
    (bar data x y))
  FacetGrid
  (facet-grid
    [this spec facet-x facet-y _]
    (facet spec data facet-x facet-y)))

(defn stacked-bar
  "Stacks bars on the `fill' field"
  [[datum & more :as data] x y fill]
  (let [mark {:type       "rect"
              :from       {:data      "table"
                           :transform [{:type    "stack"
                                        :groupby [x]
                                        :field   y}]}
              :properties {:update {:x     {:scale "x" :field x}
                                    :width {:scale "x" :band true :offset -1}
                                    :y     {:scale "y" :field "layout_start"}
                                    :y2    {:scale "y" :field "layout_end"}
                                    :fill  {:scale "fill" :field fill}
                                    :stroke {:value "white"}
                                    :strokeWidth {:value 1}}}}
        sum_y_data {:name      "stats"
                    :source    "table"
                    :transform [{:type      "aggregate"
                                 :groupby   [x]
                                 :summarize [{:field y :ops ["sum"]}]}]}]
    (merge-spec (bar data x y)
                {:data    [sum_y_data]
                 :scales
                          ^:replace [{:name   "x",
                                      :type   "ordinal",
                                      :domain (u/dedup (map #(get % x) data)),
                                      :range  "width"}
                                     {:name   "y",
                                      :type   "linear",
                                      :domain {:data "stats", :field (str "sum_" (name y))},
                                      :range  "height",
                                      :round  true,
                                      :nice   true}
                                     {:name   "fill",
                                      :type   "ordinal",
                                      :domain {:data "table", :field fill},
                                      :range  "category20"}],
                 :axes    ^:replace [{:type "x", :scale "x"}
                                     {:type "y", :scale "y"}],
                 :legends (if (contains? datum fill) [{:fill "fill"}] [])
                 :marks   ^:replace [mark]})))

(defrecord StackedBar
  [data]
  VegaSpec
  (->vg-spec [this {:keys [x y fill] :or {x :x y :y fill :fill}}]
    (stacked-bar data x y fill))
  FacetGrid
  (facet-grid [this spec facet-x facet-y _]
    (let [facetted-spec
          (-> spec
              (update-in [:data 1 :transform 0 :groupby]
                         into [facet-x facet-y])
              (update-in [:marks 0 :from] dissoc :data))]
      (facet facetted-spec (ensure-facet-keys data facet-x facet-y) facet-x facet-y))))

(defn- dodged-bars-mark
  "Dodges bars on the fill. Assumes that a x, y and fill scales have been previously defined."
  [x y fill]
  {:type "group",
   :from
         {:data      "table"
          :transform [{:type "facet", :groupby [x]}]},
   :scales
         [{:name   "x-dodge",
           :type   "ordinal",
           :range  "width",
           :domain {:data "table" :field fill}}],
   :properties
         {:update
          {:x     {:scale "x", :field x},
           :width {:scale "x", :band true}
           :stroke {:value "white"}
           :strokeWidth {:value 1}}
          },
   :marks
         [{:name "dodged-bars",
           :type "rect",
           :properties
                 {:update
                  {:x     {:scale "x-dodge", :field fill},
                   :width {:scale "x-dodge", :band true},
                   :y     {:scale "y", :field y},
                   :y2    {:scale "y", :value 0},
                   :fill  {:scale "fill", :field fill}}}}]})

(defn dodged-bar
  "Returns a vega spec for plotting a dodged bar plot. Requires x and
 y aesthetics. Optional aesthetics: fill"
  [[datum & more :as data] x y fill]
  (let [spec (merge-spec (bar data x y)
                         {:scales ^:replace [{:name    "x",
                                              :type    "ordinal",
                                              :padding 0.2,
                                              :range   "width",
                                              :domain  (u/dedup (map #(get % x) data))}
                                             {:name   "y",
                                              :type   "linear",
                                              :range  "height",
                                              :domain {:data "table", :field y},
                                              :nice   true
                                              :round  true}]})
        fill-spec {:scales  [{:name   "fill",
                              :type   "ordinal",
                              :domain {:data "table", :field fill},
                              :range  "category20"}]
                   :marks   ^:replace [(dodged-bars-mark x y fill)]
                   :legends (if (contains? datum fill) [{:fill "fill"}] [])}
        spec (merge-spec spec fill-spec)]
    spec))

(defrecord DodgedBar
  [data]
  VegaSpec
  (->vg-spec [this {:keys [x y fill] :or {x :x y :y fill :fill}}]
    (dodged-bar data x y fill))
  FacetGrid
  (facet-grid [this spec facet-x facet-y _]
    (let [facetted-spec (update-in spec [:marks 0 :from] dissoc :data)]
      (facet facetted-spec (ensure-facet-keys data facet-x facet-y) facet-x facet-y))))

(defn point
  "Returns a vega spec for drawing points. Each datum is required to
  have the x and y keys, and optionally fill and size."
  [[datum & more :as data] x y fill size]
  {:pre [(contains? datum x) (contains? datum y)]}
  (let [x-type (guess-scale-type data x)
        x-scale (merge-spec {:name "x" :domain {:data "table" :field x} :range "width"}
                            (case x-type
                              "ordinal" {:type "ordinal" :points true :padding 1}
                              "linear" {:type "linear" :nice true :round true}
                              "time" {:type "time"}))
        y-type (guess-scale-type data y)
        y-scale (merge-spec {:name "y" :domain {:data "table" :field y} :range "height"}
                            (case y-type
                              "ordinal" {:type "ordinal" :points true :padding 1}
                              "linear" {:type "linear" :nice true}
                              "time" {:type "time"}))
        default-point {:scales [x-scale y-scale]
                       :axes   [{:type "x", :scale "x"} {:type "y", :scale "y"}]
                       :data   [{:name "table" :values data}]
                       :mark-tmp
                               {:type "symbol",
                                :from {:data "table"},
                                :properties
                                      {:update
                                              {:x           {:scale "x", :field x},
                                               :y           {:scale "y", :field y},
                                               :fill        {:value "steelblue"}
                                               :size        {:value 10},
                                               :fillOpacity {:value 1}},
                                       :hover {:fillOpacity {:value 1}}}}}
        fill-specs {:scales   [{:name   "fill",
                                :type   "ordinal",
                                :domain {:data "table", :field fill},
                                :range  "category20"}]
                    :mark-tmp {:properties {:update
                                            {:fill ^:replace
                                                   {:scale "fill", :field fill}}}}
                    :legends  (if (contains? datum fill) [{:fill "fill"}] [])}
        size-specs {:scales   [(merge-spec {:name   "size"
                                            :domain {:data "table", :field size}
                                            :range  [16 100]}
                                           (case (guess-scale-type data size)
                                             "linear" {:type "linear" :nice true :round true}
                                             "ordinal" {:type "ordinal" :points true}
                                             "time" {:type "time" :points true}
                                             nil {:type  "ordinal" :points true
                                                  :range ^:replace [25 25]}))]
                    :legends  (if (contains? datum size) [{:size "size"}] [])
                    :mark-tmp {:properties {:update
                                            {:size ^:replace
                                                   {:scale "size", :field size}}}}}
        spec (merge-spec default-point fill-specs size-specs)]
    (-> spec (assoc :marks [(:mark-tmp spec)]) (dissoc :mark-tmp))))

(defrecord Point
  [data]
  VegaSpec
  (->vg-spec [this {:keys [x y fill size]}]
    (point data x y fill size))
  FacetGrid
  (facet-grid [this spec facet-x facet-y _]
    (let [facetted-spec (update-in spec [:marks 0 :from] dissoc :data)]
      (facet facetted-spec (ensure-facet-keys data facet-x facet-y) facet-x facet-y))))

(defn line
  "line chart"
  [[datum & more :as data] x y stroke]
  (let [x-type (guess-scale-type data x)
        x-scale (merge-spec {:name "x" :domain {:data "table" :field x} :range "width"}
                            (case x-type
                              "ordinal" {:type "ordinal" :points true :padding 1}
                              "linear" {:type "linear" :nice true :round true}
                              "time" {:type "time"}))
        y-type (guess-scale-type data y)
        y-scale (merge-spec {:name "y" :domain {:data "table" :field y} :range "height"}
                            (case y-type
                              "ordinal" {:type "ordinal" :points true :padding 1}
                              "linear" {:type "linear" :nice true}
                              "time" {:type "time"}))
        stroke-scale {:name "stroke" :domain {:data "table" :field stroke}
                      :type "ordinal" :range "category20"}
        vg-spec {:scales  [x-scale y-scale stroke-scale]
                 :legends (if (contains? datum stroke) [{:fill "stroke"}] [])
                 :axes    [{:type "x", :scale "x"} {:type "y", :scale "y"}]
                 :data    [{:name "table" :values data}]}
        line-mark (-> {:type "line"}
                      (assoc-in [:properties :update]
                                {:x           {:scale "x" :field x}
                                 :y           {:scale "y" :field y}
                                 :stroke      {:scale "stroke" :field stroke}
                                 :strokeWidth {:value 2}}))
        group-mark {:type  "group"
                    :from  {:data      "table"
                            :transform [{:type    "facet"
                                         :groupby [stroke]}]}
                    :marks [line-mark]}]
    (-> vg-spec
        (assoc :marks [group-mark]))))

(defrecord Line
  [data]
  VegaSpec
  (->vg-spec [this {:keys [x y stroke] :or {x :x y :y stroke :stroke}}]
    (line data x y stroke))
  FacetGrid
  (facet-grid [this spec facet-x facet-y _]
    (let [facetted-spec
          (-> spec
              (update-in [:marks 0 :from] dissoc :data))]
      (facet facetted-spec (ensure-facet-keys data facet-x facet-y) facet-x facet-y))))

(defn choropleth
  [[datum & more :as data] geopath-transform fill]
  (let [transforms [geopath-transform]
        data [{:name "table" :values data}]
        scales [{:name   "fill" :type "quantize"
                 :domain {:data "table" :field fill}
                 :range  ["#fff7ec", "#fee8c8", "#fdd49e", "#fdbb84", "#fc8d59",
                          "#ef6548", "#d7301f", "#b30000", "#7f0000"]}]
        marks [{:type "path"
                ;; geotransform in mark so that facetting works?
                :from {:data      "table"
                       :transform transforms}
                :properties
                      {:update {:path {:field "layout_path"}
                                :fill {:scale "fill" :field fill}}
                       :hover  {:fill {:value "steelblue"}}}}]]
    {:data    data
     :scales  scales
     :marks   marks
     :legends [{:fill "fill"}]}))


(defrecord Choropleth
  [data]
  VegaSpec
  (->vg-spec [this {:keys [geopath-transform fill]}]
    (choropleth data geopath-transform fill))
  FacetGrid
  (facet-grid [this spec facet-x facet-y _]
    (let [facetted-spec
          (-> spec
              (update-in [:marks 0 :from] dissoc :data))]
      (facet facetted-spec (ensure-facet-keys data facet-x facet-y) facet-x facet-y))))
