(ns gyptis-usage.vega-interop
  (:require [gyptis.core :refer :all]
            [gyptis.vega-templates :as vt]
            [gyptis.view :refer [plot!]]
            [clojure.data.json :as json]
            [clj-http.client :as client]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plotting an existing vega spec
(def vega-bars (-> "http://vega.github.io/vega-editor/app//spec/vega/bar.json"
                   client/get
                   :body
                   json/read-str
                   clojure.walk/keywordize-keys))

(plot! vega-bars)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plot Vega driving example

(def vega-driving (-> "https://vega.github.io/vega-editor/app//spec/vega/driving.json"
                      client/get
                      :body
                      json/read-str
                      clojure.walk/keywordize-keys
                      ;; replace the data url with an absolute path
                      (assoc-in [:data 0 :url] "https://vega.github.io/vega-editor/app/data/driving.json")))

(plot! (title vega-driving "Plotted by gyptis"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plotting the `cars' dataset
(def car-data (-> "https://raw.githubusercontent.com/vega/vega-lite/master/data/cars.json"
                  client/get
                  :body
                  json/read-str
                  clojure.walk/keywordize-keys))

(binding [vt/*x* :Name
          vt/*y* :Miles_per_Gallon
          vt/*fill* :Cylinders]
  (-> car-data
      point
      plot!))
