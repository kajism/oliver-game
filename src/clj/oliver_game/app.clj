(ns oliver-game.app)

(defonce ctx
  (atom {:http-server nil
         :nrepl-server nil
         :event-bus nil
         :listen-to-bus-events? true}))
