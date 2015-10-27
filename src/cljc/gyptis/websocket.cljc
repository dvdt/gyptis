(ns gyptis.websocket
  "Initializes websocket channels for both server and client."
  #?(:clj (:require [taoensso.sente :as sente]
                    [taoensso.sente.packers.transit :as sente-transit]
                    [clojure.core.async :as async  :refer [<! <!! >! >!! put! chan go go-loop]]
                    [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
                    [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf debug)]))
  #?@(:cljs
      [(:require [taoensso.sente :as sente]
                 [taoensso.sente.packers.transit :as sente-transit]
                 [cljs.core.async :as async  :refer (<! >! put! chan)]
                 [taoensso.timbre :as timbre :refer-macros (trace tracef debugf infof warnf errorf)])
       (:require-macros
        [cljs.core.async.macros :as asyncm :refer (go go-loop)])]))

(def ^:dynamic *endpoint*
  "URL endpoint for websocket connections."
  "/chsk")

(def packer (sente-transit/get-flexi-packer :json))

#?(:clj
   (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
                 connected-uids]}
         (sente/make-channel-socket! sente-web-server-adapter
                                     {:packer packer
                                      :user-id-fn (fn [ring-req]
                                                    (:client-id ring-req))})]
     (def ring-ajax-post                ajax-post-fn)
     (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
     (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
     (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
     (def connected-uids                connected-uids) ; Watchable, read-only atom
     )
   :cljs
   (let [{:keys [chsk ch-recv send-fn state]}
         (sente/make-channel-socket! *endpoint* ; Note the same URL as before
                                     {:type :auto
                                      :packer packer
                                      :client-id (-> js/window .-location .-href
                                                     (clojure.string/split #"views/")
                                                     last)})]
     (def chsk       chsk)
     (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
     (def chsk-send! send-fn) ; ChannelSocket's send API fn
     (def chsk-state state)   ; Watchable, read-only atom
     ))

;;;; Routing handlers
(defmulti event-msg-handler :id) ; Dispatch on event-id
;; Wrap for logging, catching, etc.:
(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (debugf "Event: %s" event)
  (event-msg-handler ev-msg))

(defmulti gyptis-handler
  "Handles messages from the server for manipulating plot"
  (fn [[id data]] id))
(defmethod gyptis-handler :some/broadcast
  [[id data]]
  (debugf "Got broadcast msg!!!"))

#?(
:clj ; Server-side methods
(do
  (defmethod event-msg-handler :default ; Fallback
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid     session)]
      (debugf "Unhandled event: %s, with id=" event id)
      (debugf "ev-msg=%s" ev-msg)
      (when ?reply-fn
        (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))
  ;; run this on client: (chsk-send! [:veg/echo {:data "datra"}])
  (defmethod event-msg-handler :gyptis/echo
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn uid]}]
    (debugf "echoing: %s" ?data)
    (chsk-send! uid [:veg/print ?data]))
  (defmethod event-msg-handler :chsk/ws-ping
    [{:as ev-msg :keys [?data]}])

  (defmethod event-msg-handler :chsk/uidport-open
    [{:as ev-msg :keys [?data ring-req]}])))

;; Client-side methods
:cljs
(do (defmethod event-msg-handler :default ; Fallback
      [{:as ev-msg :keys [event]}]
      (debugf "Unhandled event: %s" event))

    (defmethod event-msg-handler :chsk/state
      [{:as ev-msg :keys [?data]}]
      (if (= ?data {:first-open? true})
        (debugf "Channel socket successfully established!")
        (debugf "Channel socket state change: %s" ?data)))

    (defmethod event-msg-handler :chsk/recv
      [{:as ev-msg :keys [?data]}]
      (debugf "Push event from server: %s" ?data)
      (gyptis-handler ?data))

    (defmethod event-msg-handler :chsk/handshake
      [{:as ev-msg :keys [?data]}]
      (let [[?uid ?csrf-token ?handshake-data] ?data]
        (debugf "Handshake: %s" ?data))))
;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...


;; this broadcaster sends random data to the client to plot.
#?(:clj
   (do
     (defn start-broadcaster! []
       (go-loop [i 0]
         (<! (async/timeout 10000))
         (println (format "Broadcasting server>user: %s" @connected-uids))
         (doseq [uid (:any @connected-uids)]
           (chsk-send! uid
                       [:some/broadcast
                        {:what-is-this "A broadcast pushed from server"
                         :how-often    "Every 10 seconds"
                         :to-whom uid
                         :i i}]))
         (recur (inc i))))
))

;;;; Init
(defonce router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start! []
  (start-router!)
 ; #?(:clj (start-broadcaster!))
)
