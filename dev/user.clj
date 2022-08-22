(ns user
  (:require [cljc.portal :as portal]
            [clojure.java.io :as io]
            [clojure.repl]
            [clojure.tools.namespace.repl]
            [oliver-game.system :as system]
            [integrant.repl :as ig-repl :refer [halt]]
            [integrant.repl.state :as ig-state]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]))

(clojure.tools.namespace.repl/set-refresh-dirs "src" "test")

(ig-repl/set-prep! (fn [] system/config))

(defonce cljs-started? (atom false))

(defn reset-cljs-build []
  (shadow-server/stop!)
  (shadow-server/start!)
  (shadow/watch :app {:autobuild false})
  (reset! cljs-started? true))

(defn reset []
  (ig-repl/reset)
  (when-not @cljs-started?
    (reset-cljs-build))
  (shadow/watch-compile! :app))

(def open-portal portal/open)

(def close-portal portal/close)

(def pportal portal/pp)

(def >portal portal/>p)

(def >>portal portal/>>p)

(comment
  (open-portal)
  (portal.api/clear)
  (pportal "my data" {:my "data" :a 1})
  ;;see more examples in dev/cljc.portal
  (deref (deref portal/portal-client-ref))
  (close-portal)

  (shadow-server/reload!)
  (shadow/active-builds)
  (shadow/repl :app)
  (shadow/compile :app)
  (shadow/watch :app)
  (shadow/watch :app {:autobuild false})
  (shadow/watch-compile! :app)
  (shadow/watch-compile-all!)

  (reset)
  (halt)

  (remove-ns 'user)

  (clojure.tools.namespace.repl/clear)
  (clojure.tools.namespace.repl/refresh-all)

  )

(when (io/resource "local.clj")
  (load "local"))
