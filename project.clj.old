(defproject oliver-game "0.0.5-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript  "1.11.4"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.2.11"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [environ "1.2.0"]
                 [integrant "0.8.0"]
                 [luminus/ring-undertow-adapter "1.2.5"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-defaults "0.3.3"]
                 [metosin/reitit-ring "0.5.17"]
                 [hiccup "1.0.5"]
                 [re-frame "1.2.0"]
                 [nrepl "0.9.0"]]

  :plugins [[lein-cljsbuild "1.1.8"]]

  :main ^:skip-aot oliver-game.main

  :min-lein-version "2.5.3"

  :jvm-opts ["-Duser.timezone=UTC" "-XX:-OmitStackTraceInFastThrow" "-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]

  :javac-options ["-target" "1.8" "-source" "1.8"]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/cljs-out" "target"]

  :profiles
  {:dev
   {:source-paths ["dev"]
    :repl-options {:init-ns user}
    :dependencies [[binaryage/devtools "1.0.5"]
                   [com.bhauman/figwheel-main "0.2.16"]
                   [com.bhauman/rebel-readline-cljs "0.1.4"]
                   [cider/piggieback "0.5.3"]
                   [hashp "0.2.1"]
                   [integrant/repl "0.3.2"]]
    :resource-paths ["target"]
    :plugins [[lein-environ "1.2.0"]]}

   :uberjar {:omit-source  true
             :main         oliver-game.main
             :aot          [oliver-game.main]
             :uberjar-name "oliver-game-standalone.jar"
             :prep-tasks   ["javac" "compile" ["cljsbuild" "once" "min"]]}
   }

  :cljsbuild
  {:builds
   [{:id           "min"
     :source-paths ["src/cljs" "src/cljc"]
     :jar true
     :compiler     {:main oliver-game.core
                    :output-to       "resources/public/cljs-out/app-main.js"
                    :optimizations   :advanced
                    :language-in     :ecmascript-next
                    :language-out    :ecmascript-next
                    :parallel-build true
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]}

  :release-tasks
  [["vcs" "assert-committed"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["uberjar"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]])
