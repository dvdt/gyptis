(ns gyptis.vega-templates
  "Helper functions that return vega.js plot declarations"
  (:require [gyptis.util :refer [merge-spec] :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
(def ^:dynamic *table* "table")
(def ^:dynamic *x* :x)
(def ^:dynamic *y* :y)
(def ^:dynamic *fill* :fill)
(def ^:dynamic *size* :size)
(def ^:dynamic *facet-x* :facet_x)
(def ^:dynamic *facet-y* :facet_y)

(defn guess-scale-type
  "Returns linear or ordinal"
  [data field]
  (when-let [d (first (drop-while nil? (map #(get % field) data)))]
    (cond
      (number? d) "linear"
      (or (string? d) (nil? d)) "ordinal"
      :else (throw #?(:cljs
                      (js/Error (str "Can't guess scale type for " d ", type= " (type d)))
                      :clj
                      (Exception. (str "Can't guess scale type for " d)))))))

(defn suppress-axis-labels
  [axis]
  (assoc-in axis [:properties :labels :text :value] ""))

(defn ensure-facet-keys
  [data]
  (map (fn [datum]
         (->> datum
          (u/ensure-key *facet-x* "")
          (u/ensure-key *facet-y* ""))) data))

(defn ->facet-mark
  [inner-spec data]
  {:type "group"
   :from {:data *table*
          :transform  [{:type "facet" :groupby [*facet-x* *facet-y*]}]}
   :properties {:enter {:x {:scale "facet_x_scale" :field *facet-x*}
                        :y {:scale "facet_y_scale" :field *facet-y*}
                        :height {:scale "facet_y_scale" :band true}
                        :width {:scale "facet_x_scale" :band true}}}
   :marks (:marks inner-spec)
   :scales (:scales inner-spec)
   :axes (:axes inner-spec)})

(defn add-facet-axes
  "Returns a vector of facetted group marks with axes legends in the
  bottom row and first column"
  [facet-mark [datum & more :as data]]
  (let [first-facet-x (->> *facet-x* (get datum) u/->json)
        last-facet-y (->> *facet-y* (get (last more)) u/->json)
        unlabelled-facet-pred (str "datum." (name *facet-x*) "!==" first-facet-x
                                   " && datum." (name *facet-y*) "!==" last-facet-y)
        x-label-facet-pred (str "datum." (name *facet-x*) "!==" first-facet-x
                                " && datum." (name *facet-y*) "!==" last-facet-y)
        y-label-facet-pred   (str "datum." (name *facet-x*) "===" first-facet-x
                                  " && datum." (name *facet-y*) "!==" last-facet-y)
        xy-label-facet-pred (str "datum." (name *facet-x*) "===" first-facet-x
                                   " && datum." (name *facet-y*) "===" last-facet-y)

        unlabelled-facet-mark
        (-> facet-mark
            (update-in [:axes] #(mapv suppress-axis-labels %))
            (update-in [:from :transform] conj {:type "filter"
                                                :test unlabelled-facet-pred}))
        x-labelled-facet-mark
        (-> unlabelled-facet-mark
            (assoc-in [:from :transform 1] {:type "filter"
                                            :test x-label-facet-pred})
            (merge-spec {:axes ^:replace [{:type "x"
                                           :scale "x"}
                                          {:type "y"
                                           :scale "y" :properties {:labels {:text {:value ""}}}}]}))
        y-labelled-facet-mark
        (-> unlabelled-facet-mark
            (assoc-in [:from :transform 1] {:type "filter"
                                            :test y-label-facet-pred})
            (merge-spec {:axes ^:replace [{:type "x"
                                           :scale "x" :properties {:labels {:text {:value ""}}}}
                                          {:type "y"
                                           :scale "y"}]}))
        xy-labelled-facet-mark
        (-> unlabelled-facet-mark
            (assoc-in [:from :transform 1] {:type "filter"
                                            :test xy-label-facet-pred})
            (merge-spec {:axes ^:replace [{:type "x"
                                           :scale "x"}
                                          {:type "y"
                                           :scale "y"}]}))]
    [unlabelled-facet-mark x-labelled-facet-mark y-labelled-facet-mark xy-labelled-facet-mark]))

(defn facet
  "Returns a vega spec with a facetted layout based on the *facet-x*
  and *facet-y* fields."
  [inner-spec [datum & more :as data]]
  {:pre [(contains? datum *facet-x*) (contains? datum *facet-y*)]}
  (let [facetted-marks (add-facet-axes (->facet-mark inner-spec data)
                                       data)]
    {:data (:data inner-spec)
     :legends (:legends inner-spec)
     :scales (vec (concat [{:name "facet_x_scale"
                            :type "ordinal"
                            :padding 0.15
                            :range "width"
                            :domain {:data *table* :field *facet-x*}}
                           {:name "facet_y_scale"
                            :type "ordinal"
                            :padding 0.15
                            :range "height"
                            :domain {:data *table* :field *facet-y*}}]
                          (:scales inner-spec)))
     :axes [{:type "x" :scale "facet_x_scale" :orient "top" :tickSize 0
             :properties {:axis {:strokeWidth {:value 0}}}}
            {:type "y" :scale "facet_y_scale" :orient "right" :tickSize 0
             :properties {:axis {:strokeWidth {:value 0}}}}]
     :marks facetted-marks}))

(def top-level
  {:width 800
   :height 600
   :padding {:top 100, :left 100, :bottom 100, :right 200}
   :data []
   :scales []
   :axes []
   :marks []
   :legends []})

(defn assoc-hover
  [vg hover-spec]
  (assoc-in vg [:marks 0 :properties :hover] hover-spec))

(defn bar
  [data]
  {:data [{:name *table* :values data}]
   :scales [{:name "x",
             :type "ordinal",
             :range "width",
             :domain {:data *table*, :field *x*}}
            {:name "y",
             :type "linear",
             :range "height",
             :domain {:data *table*, :field *y*},
             :nice true
             :round true}],
   :axes [{:type "x", :scale "x"} {:type "y", :scale "y"}],
   :marks
   [{:type "rect",
     :properties
     {:update
      {:x {:scale "x", :field *x*},
       :width {:scale "x", :band true, :offset -1},
       :y {:scale "y", :field *y*},
       :y2 {:scale "y", :value 0}
       :fill {:value "steelblue"}}}}]})

(def stack-transform
  {:type "stack"
   :groupby [*x*]
   :field *y*})

(defn stacked-bar
  "Stacks bars on the `fill' field"
  [[datum & more :as data]]
  (let [mark {:type "rect"
              :from {:transform [stack-transform]}
              :properties {:update {:x {:scale "x" :field *x*}
                                    :width {:scale "x" :band true :offset -1}
                                    :y {:scale "y" :field "layout_start"}
                                    :y2 {:scale "y" :field "layout_end"}
                                    :fill {:scale "fill" :field *fill*}}}}
        sum_y_data {:name "stats"
                    :source *table*
                    :transform [{:type "aggregate"
                                 :groupby [*x*]
                                 :summarize [{:field *y* :ops "sum"}]}]}]
    (merge-spec (bar data)
                {:data [sum_y_data]
                 :scales
                 ^:replace [{:name "x",
                             :type "ordinal",
                             :domain {:data *table*, :field *x*},
                             :range "width"}
                            {:name "y",
                             :type "linear",
                             :domain {:data "stats", :field (str "sum_" (name *y*))},
                             :range "height",
                             :round true,
                             :nice true}
                            {:name "fill",
                             :type "ordinal",
                             :domain {:data *table*, :field *fill*},
                             :range "category20"}],
                 :axes ^:replace [{:type "x", :scale "x"}
                                  {:type "y", :scale "y"}],
                 :legends (if (contains? datum *fill*) [{:fill "fill"}] [])
                 :marks ^:replace [mark]})))

(defn dodged-bars-mark
  "Dodges bars on the fill. Assumes that a x, y and fill scales have been previously defined."
  []
  {:type "group",
   :from
   {:data *table*,
    :transform [{:type "facet", :groupby [*x*]}]},
   :scales
   [{:name "x-dodge",
     :type "ordinal",
     :range "width",
     :domain {:field *fill*}}],
   :properties
   {:update
    {:x {:scale "x", :field *x*},
     :width {:scale "x", :band true}}},
   :marks
   [{:name "dodged-bars",
     :type "rect",
     :properties
     {:update
      {:x {:scale "x-dodge", :field *fill*},
       :width {:scale "x-dodge", :band true},
       :y {:scale "y", :field *y*},
       :y2 {:scale "y", :value 0},
       :fill {:scale "fill", :field *fill*}}}}]})

(defn dodged-bar
  "Returns a vega spec for plotting a dodged bar plot. Requires *x* and
 *y* aesthetics. Optional aesthetics: *fill*"
  [[datum & more :as data]]
  (let [spec (merge-spec (bar data)
                           {:scales ^:replace [{:name "x",
                                                :type "ordinal",
                                                :padding 0.2,
                                                :range "width",
                                                :domain {:data *table*, :field *x*}}
                                               {:name "y",
                                                :type "linear",
                                                :range "height",
                                                :domain {:data *table*, :field *y*},
                                                :nice true
                                                :round true}]})
        fill-spec {:scales [{:name "fill",
                             :type "ordinal",
                             :domain {:data *table*, :field *fill*},
                             :range "category20"}]
                   :marks ^:replace [(dodged-bars-mark)]
                   :legends [{:fill "fill"}]}
        spec (if-not (contains? datum *fill*) spec
                     (merge-spec spec fill-spec))]
    spec))

(defn point
  "Returns a vega spec for drawing points. Each datum is required to
  have the *x* and *y* keys, and optionally *fill* and *size*."
  [[datum & more :as data]]
  {:pre [(contains? datum *x*) (contains? datum *y*)]}
  (let [x-type (guess-scale-type data *x*)
        x-scale (merge-spec {:name "x" :domain {:data *table* :field *x*} :range "width"}
                            (case x-type
                              "ordinal" {:type "ordinal" :points true :padding 1}
                              "linear" {:type "linear" :nice true :round true}))
        y-type (guess-scale-type data *y*)
        y-scale (merge-spec {:name "y" :domain {:data *table* :field *y*} :range "height"}
                            (case y-type
                              "ordinal" {:type "ordinal" :points true :padding 1}
                              "linear" {:type "linear" :nice true}))
        default-point {:scales [x-scale y-scale]
                       :axes [{:type "x", :scale "x"} {:type "y", :scale "y"}]
                       :data [{:name *table* :values data}]
                       :mark-tmp
                       {:type "symbol",
                        :from {:data *table*},
                        :properties
                        {:update
                         {:x {:scale "x", :field *x*},
                          :y {:scale "y", :field *y*},
                          :fill {:value "steelblue"}
                          :size {:value 10},
                          :fillOpacity {:value 1}},
                         :hover {:fillOpacity {:value 1}}}}}
        spec
        (merge-spec default-point
                    (when-let [fill-type (guess-scale-type data *fill*)]
                      {:scales [{:name "fill",
                                 :type "ordinal",
                                 :domain {:data *table*, :field *fill*},
                                 :range "category20"}]
                       :mark-tmp {:properties {:update
                                               {:fill ^:replace
                                                {:scale "fill", :field *fill*}}}}
                       :legends [{:fill "fill"}]})
                    (when-let [size-type (guess-scale-type data *size*)]
                      {:scales [(merge-spec {:name "size"
                                             :domain {:data *table*, :field *size*}
                                             :range [16 100]}
                                            (case size-type
                                              "linear" {:type "linear" :nice true :round true}
                                              "ordinal" {:type "ordinal" :points true}))]
                       :legends [{:size "size"}]
                       :mark-tmp {:properties {:update
                                               {:size ^:replace
                                                {:scale "size", :field *size*}}}}}))]
    (-> spec (assoc :marks [(:mark-tmp spec)]) (dissoc :mark-tmp))))
