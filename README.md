Gyptis, a clojure/script data visualization library for the web, based on vega.js
===

**[Vega](https://github.com/vega/vega)** is a javascript library for
  creating data visualizations through a declarative JSON format.

**Gyptis** helps you produce, modify and render Vega JSON specs. It
  supports common visualization designs like bar and line charts, and
  even choropleth maps.

Leiningen coordinates
---
```clojure
[gyptis "0.1.0"]
```

Usage
---
[![IMAGE ALT TEXT HERE](http://img.youtube.com/vi/YOUTUBE_VIDEO_ID_HERE/0.jpg)](http://www.youtube.com/watch?v=YOUTUBE_VIDEO_ID_HERE)

Also try looking at [examples/usage](examples/usage)

```clojure
(use 'gyptis.core)
(require '[gyptis.view.server :refer [plot!]])

(def fibonacci [{:x "a", :y  1 :fill 1}
                {:x "a", :y  1 :fill 2}
                {:x "b", :y  2 :fill 1}
                {:x "b", :y  3 :fill 2}
                {:x "c", :y  5 :fill 1}
                {:x "c", :y  8 :fill 2}
                {:x "d", :y 13 :fill 1}
                {:x "d", :y 21 :fill 2}])

(plot! (stacked-bar fibonacci))
```

Documentation
---
- [API Docs](http://dvdt.github.io/gyptis/)
