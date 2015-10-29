# Gyptis, a clojure/script data visualization library for the web, based on vega.js

## Usage
```{clojure}
(use 'gyptis.core)

;; Opens a browser tab. Plots will be displayed here
(new-window! "figure 1")


(def figure-1
    (point [{:x :a, :y 0.08}
{:x :a, :y 0.08}]
(plot "figure 1" )
```

## How does it work

# compile
TIMBRE_LEVEL=':warn' lein with-profile prod do clean, cljsbuild once app
