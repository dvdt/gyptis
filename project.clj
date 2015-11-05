(defproject gyptis "0.1.0-SNAPSHOT"
  :description "A library for generating and viewing vega.js plots"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.166" :scope "provided"]
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

  :profiles {:dev {:dependencies [[lein-figwheel "0.3.9" :exclusions [[org.clojure/clojure]
                                                                      [org.clojure/core.async]
                                                                      [com.google.javascript/closure-compiler]
                                                                      [com.google.javascript/closure-compiler-externs]
                                                                      [org.codehaus.plexus/plexus-utils]]]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [com.cemerick/piggieback "0.1.5"]
                                  [pjstadig/humane-test-output "0.7.0"]]

                   :plugins [[lein-figwheel "0.3.9" :exclusions [[org.clojure/clojure]
                                                                 [org.clojure/core.async]
                                                                 [com.google.javascript/closure-compiler]
                                                                 [com.google.javascript/closure-compiler-externs]
                                                                 [org.codehaus.plexus/plexus-utils]]]
                             [cider/cider-nrepl "0.10.0-SNAPSHOT"]
                             [refactor-nrepl "2.0.0-20151021.210235-11" :exclusions [org.clojure/clojure]]
                             [lein-cljsbuild "1.1.0"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:nrepl-port 7002
                              :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                                 "cider.nrepl/cider-middleware"
                                                 "refactor-nrepl.middleware/wrap-refactor"]
                              :css-dirs ["resources/public/css"]}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "gyptis.dev"
                                                         :source-map true}}}}}

             ;; For cljs compilation
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
