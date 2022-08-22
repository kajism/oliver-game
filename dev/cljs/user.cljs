(ns cljs.user
  (:require [cljc.portal :as portal]
            [devtools.core :as devtools]))

(js/console.info "Starting in development mode")

(devtools/install!)

(enable-console-print!)

(def open-portal portal/open)

(def close-portal portal/close)

(def pportal portal/pp)

(def >portal portal/>p)

(def >>portal portal/>>p)

(comment
  (open-portal)
  (pportal "my hello" :hello)
  ;;see more examples in dev/cljc.portal
  @@portal/portal-client-ref
  (close-portal)
  )