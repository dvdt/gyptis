(ns gyptis.core
    (:require [reagent.core :as reagent]
              [reagent.session :as session]
              [gyptis.websocket :as ws]))

;; -------------------------
;; Components

(defn root []
  [:div [:h2 "!Welcome to gyptis!!"]])

;; -------------------------
;; Initialize app
(defn mount-root []
  (ws/start!)
  (reagent/render [root] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
