(ns gyptis.view.server
  "Web server for pushing (clojure) vega.js plot specifications to a browser client for rendering."
  (:require [clojure.java.browse :as browse]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [gyptis.view.websocket :as ws]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css include-js]]
            [environ.core :refer [env]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as routes]
            [clojure.core.async :as async  :refer [<! <!! >! >!! put! chan go go-loop]]))

(timbre/set-level!)
(defonce web-server_ (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start web server

(defn stop-web-server! [] (when-let [m @web-server_] ((:stop-fn m))))

(defn start-web-server!* [ring-handler port]
  (stop-web-server!)
  (println "Starting http-kit...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server  nil ; http-kit doesn't expose this
     :port    (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(declare app)
(defn -main [& args]
  (ws/start!)
  (let [port (Integer/parseInt (or (env :port) "3211"))]
    (reset! web-server_  (start-web-server!* #'app port))))

(defn new!
  "Opens a new browser tab and returns that tab's uid.
  Blocks until browser tab is connected by websocket."
  [& {:keys [view-name]}]
  (let [view-name (or view-name "default")
        watch-key (keyword view-name)
        connect-chan (chan)]
    (browse/browse-url (str "http://localhost:" (:port @web-server_) "/views/" view-name))
    (when-not (get-in @ws/connected-uids [:any view-name])
      (add-watch ws/connected-uids watch-key
                 (fn [k r old new]
                   (debugf "New connected-uids=%s" (str new))
                   (when (and (not (get-in old [:any view-name]))
                              (get-in new [:any view-name]))
                     (>!! connect-chan view-name))))
      (let [[msg ch] (async/alts!! [connect-chan (async/timeout 10000)])]
        (if-not msg
          (warnf "Couldn't connect websocket to browser window!")
          (debugf "We've got a websocket!"))) ; blocks until the view-name uid is connected-uids
      (remove-watch ws/connected-uids watch-key)
      view-name)))

(defn plot!*
  [ch vega-spec]
  (ws/chsk-send! ch [:gyptis/plot vega-spec]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes

(defn view-page
  [uid]
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]

     (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
     (include-js "/vendor/d3.min.js")
     (include-js "/vendor/d3.geo.projection.v0.min.js")
     (include-js (if (env :dev) "/vendor/vega.js" "/vendor/vega.min.js"))]
    [:body
     (if (env :dev)
       [:div#app
        [:h3 "ClojureScript has not been compiled!"]
        [:p "please run "
         [:b "lein figwheel"]
         " in order to start the compiler"]]
       [:div#app
        [:h3 "Loading..."]])
     (if (env :dev)
       (include-js "/js/app.js")
       (include-js "/js/gyptis.js"))]]))

(defroutes routes
  (GET  ws/*endpoint* req (ws/ring-ajax-get-or-ws-handshake req))
  (POST ws/*endpoint* req (ws/ring-ajax-post req))
  (GET "/views/:uid"
       [uid]
       (view-page uid))
  (routes/resources "/")
  (routes/not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    handler))
