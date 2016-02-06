(ns gyptis.client
  (:require [reagent.core :as reagent]
            [gyptis.view.websocket :as ws]
            [gyptis.vega-templates :as vega]
            [taoensso.timbre :as timbre :refer-macros (trace tracef debugf infof warnf errorf)]))


;; holds the vg.View instance and vega-spec
;; see: https://github.com/vega/vega/wiki/Runtime#view-component-api
(defonce *state* (reagent/atom {:view nil :spec nil}))

(defn ^:export getState []
  (:view @*state*))

(def ^:dynamic *renderer*
  "either 'svg' or 'canvas'"
  "canvas")

(defn swap-plot!
  [vega-spec div-id plot-cursor]
  (let [spec (clj->js vega-spec)
        callback
        (fn [chart]
          (let [view (chart #js {:el       (str "#" div-id)
                                 :renderer *renderer*})]
            (swap! plot-cursor assoc :view view)
            (.update view))
          (debugf "finished updating vega chart id=%s" div-id))]
    (js/vg.parse.spec spec callback)))

;; -------------------------
;; Components

(defn plot
  [div-id plot-cursor]
  (let [current-spec (atom nil)]
    (reagent/create-class
      {:display-name "plot-component"
       :reagent-render
                     (fn []
                       (when-let [new-spec (:spec @plot-cursor)]
                         (when (not= new-spec @current-spec)
                           (swap! current-spec (constantly new-spec))
                           (swap-plot! new-spec div-id plot-cursor)))
                       [:div {:id div-id}])})))

(defn root []
  [:div
   [plot "gyptis-plot" *state*]])

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [root] (.getElementById js/document "app")))

(defn init! []
  (ws/start!)
  (mount-root))

(defmethod ws/gyptis-handler :gyptis/plot
  [[event vega-spec]]
  (swap! *state* assoc :spec vega-spec))
