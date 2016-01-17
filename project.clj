(defproject gyptis "0.2.0"
  :description "A library for generating and viewing vega.js plots"
  :url "https://github.com/dvdt/gyptis"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]
                 [org.clojure/core.async "0.2.371"]
                 [reagent "0.5.1"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [com.taoensso/timbre "4.1.4"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [clj-time "0.11.0"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [com.taoensso/sente "1.6.0"]]

  :plugins [[lein-codox "0.9.0"]]

  :min-lein-version "2.5.0"

  :source-paths ["src/clj" "src/cljc"]
  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:output-to     "resources/public/js/gyptis.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "/js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {;; For cljs compilation
             ;; TIMBRE_LEVEL=':warn' lein with-profile prod do clean, cljsbuild once app
             :prod {:clean-targets ^{:protect false} [:target-path
                                                      [:cljsbuild :builds :app :compiler :output-dir]
                                                      [:cljsbuild :builds :app :compiler :output-to]]
                    :omit-source true
                    :cljsbuild {:builds {:app
                                         {:source-paths ["env/prod/cljs"]
                                          :compiler
                                          {:optimizations :advanced
                                           :externs ["externs/vega.ext.js"]
                                           :pretty-print false}}}}}})
