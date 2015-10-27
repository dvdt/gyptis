(ns gyptis.view
  (:require [clojure.java.browse :as browse]
            [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [gyptis.websocket :as ws]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css include-js]]
            [environ.core :refer [env]]
            [org.httpkit.server :as http-kit]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as routes]
            [clojure.core.async :as async  :refer [<! <!! >! >!! put! chan go go-loop]]))

(def *channels* (atom {}))
(defonce web-server_ (atom nil))

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
      (<!! connect-chan) ; blocks until the view-name uid is connected-uids
      (remove-watch ws/connected-uids watch-key)
      (debugf "We've got a websocket!")
      view-name)))

(defn clear!
  "Clears all plots in the given channel"
  [ch]
  (ws/chsk-send! ch [:gyptis/clear nil]))

(defn plot!
  "Appends a plot into the channel"
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
     (include-css (if (env :dev) "/vendor/bootstrap-theme.css" "/vendor/bootstrap-theme.min.css"))
     (include-css (if (env :dev) "/vendor/bootstrap.css" "/vendor/bootstrap.min.css"))
     (include-js "/vendor/d3.min.js")
     (include-js (if (env :dev) "/vendor/vega.js" "/vendor/vega.min.js"))]
    [:body
     [:div {:class "container"}
      [:div {:class "row"}
       [:div {:class "col-lg-11 col-lg-offset-1"}
        [:div#app
         [:h3 "ClojureScript has not been compiled!"]
         [:p "please run "
          [:b "lein figwheel"]
          " in order to start the compiler"]]]]]
     (include-js "/js/app.js")]]))

(defn wrap-uid
  [handler]
  (fn [ring-req]
    (prn "Got a ring-req=" ring-req)
    (-> ring-req handler)))

(defroutes routes
  (GET "/" []

       (html [:html [:body "no non no"]]))
  (GET  ws/*endpoint* req (ws/ring-ajax-get-or-ws-handshake req))
  (POST ws/*endpoint* req (ws/ring-ajax-post req))
  (GET "/views/:uid"
       [uid]
       (view-page uid))
  (routes/resources "/")
  (routes/not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))

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

(defn -main [& args]
  (ws/start!)
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (reset! web-server_  (start-web-server!* #'app port))))
