(ns user
  (:require [oliver-game.server :refer [start-all]]))

(comment
  (require [figwheel-sidecar.repl-api :refer [start-figwheel!]])
  (start-all)
  (start-figwheel!)
  (in-ns 'oliver-game.server))
