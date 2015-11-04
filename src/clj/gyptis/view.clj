(ns gyptis.view
  (:require [gyptis.view.server :refer [plot!* new! -main web-server_]]
            [gyptis.view.websocket :as ws]
            [ring.util.codec :refer [url-encode]]))

(def ^:dynamic *current-plot-key* :default)

(defn plot!
  "Plots the given vega-spec on the browser client connected by
  the plot-key. If no client exists, a new browser tab is opened.
  Returns the vega-spec to facilitate REPL interactions with *1."
  ([plot-key vega-spec]
   (let [plot-key (-> plot-key
                      name
                      url-encode)]
     (when-not @web-server_
       (-main))
     (when-not (get-in @ws/connected-uids [:any plot-key])
       (new! :view-name plot-key)
       (alter-var-root #'*current-plot-key* (constantly plot-key)))
     (plot!* plot-key vega-spec)
     vega-spec))
  ([vega-spec]
   (plot! *current-plot-key* vega-spec)))
