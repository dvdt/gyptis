(defproject gyptis "0.1.0-SNAPSHOT"
  :description "Data visualization for clojure"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring-server "0.4.0"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.12"]
                 [reagent-utils "0.1.5"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [prone "0.8.2"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [environ "1.0.1"]
                 [com.taoensso/sente "1.7.0-RC1"]
                 [http-kit "2.1.19"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.h2database/h2 "1.4.190"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-time "0.11.0"]
                 [com.github.fge/json-schema-validator "2.2.6"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]
                 [com.cognitect/transit-cljs "0.8.225"]]

  :plugins [[lein-environ "1.0.1"]
            [lein-asset-minifier "0.2.2"]]

  :ring {:uberwar-name "gyptis.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "gyptis.jar"

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "/js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.4.0"]
                                  [lein-figwheel "0.4.1"]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [com.cemerick/piggieback "0.1.5"]
                                  [pjstadig/humane-test-output "0.7.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.4.1"]
                             [cider/cider-nrepl "0.10.0-SNAPSHOT"]
                             [refactor-nrepl "2.0.0-SNAPSHOT"]
                             [lein-cljsbuild "1.1.0"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:nrepl-port 7002
                              :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"
                                                 "cider.nrepl/cider-middleware"
                                                 "refactor-nrepl.middleware/wrap-refactor"
                                                 ]
                              :css-dirs ["resources/public/css"]}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "gyptis.dev"
                                                         :source-map true}}}}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}
             :prod {:env {:production true}
                    :aot :all
                    :omit-source true
                    :cljsbuild {:builds {:app
                                         {:source-paths ["env/prod/cljs"]
                                          :compiler
                                          {:optimizations :advanced
                                           :externs ["externs/vega.ext.js"]
                                           :pretty-print false}}}}}})
