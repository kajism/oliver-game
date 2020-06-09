(defproject oliver-game "0.0.4-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 ;;[org.clojure/core.async "1.1.587"]
                 [org.clojure/tools.logging "1.1.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.264"]
                 [re-frame "0.10.6"]
                 [reagent "0.8.1"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [ring "1.7.1" ;; :exclusions [ring/ring-jetty-adapter]
                  ]
                 [ring/ring-defaults "0.3.2"]
                 [aleph "0.4.7-alpha5"]
                 [clj-commons/secretary "1.2.4"]
                 [compojure "1.6.1"]
                 [environ "1.2.0"]
                 [re-pressed "0.3.1"]
                 [nrepl "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-javac-resources "0.1.1"]]

  :main ^:skip-aot oliver-game.main

  :min-lein-version "2.5.3"

  :jvm-opts ["-Duser.timezone=UTC" "-XX:-OmitStackTraceInFastThrow"]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :hooks [leiningen.javac-resources]

  :clean-targets ^{:protect false} ["resources/public/cljs-out" "target"]

  :profiles
  {:dev
   {:source-paths ["dev"]
    :repl-options {:init-ns user}
    :dependencies [[binaryage/devtools "1.0.0"]
                   [com.bhauman/figwheel-main "0.2.4"]]

    :resource-paths ["target"]
    }

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
