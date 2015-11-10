(ns ^:figwheel-no-load gyptis.dev
  (:require [gyptis.client :as client]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback client/mount-root)

(client/init!)
