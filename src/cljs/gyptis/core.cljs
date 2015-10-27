(ns gyptis.core
    (:require [reagent.core :as reagent]
              [reagent.session :as session]
              [gyptis.websocket :as ws]
              [taoensso.timbre :as timbre :refer-macros (trace tracef debugf infof warnf errorf)]))

(def ^:dynamic *renderer*
  "either 'svg' or 'canvas'"
  "canvas")

(def ^:dynamic *plot-id*
  "either 'svg' or 'canvas'"
  "gyptis-plot")

(defn swap-plot!
  [vega-spec div-id]
  (let [spec (clj->js vega-spec)
        callback
        (fn [chart]
          (.update (chart #js {:el (str "#" div-id)
                               :renderer *renderer*}))
          (debugf "finished updating vega chart id=%s" div-id))]
    (js/vg.parse.spec spec callback)))

;; -------------------------
;; Components

(defn plot
  [vega-spec div-id]
  (reagent/create-class
   {:display-name "plot-component"
    :component-did-mount
    (fn []
      (swap-plot! vega-spec div-id)
      (debugf "did-mount"))
    :reagent-render
    (fn []
      [:div {:id div-id}])}))

(defn root []
  [:div
   [plot {} *plot-id*]])

;; -------------------------
;; Initialize app
(defn mount-root []
  (ws/start!)
  (reagent/render [root] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

(defmethod ws/gyptis-handler :gyptis/clear
  [{:as ev-msg :keys [?data]}])

(defmethod ws/gyptis-handler :gyptis/plot
  [[event vega-spec]]
  (swap-plot! vega-spec *plot-id*))
