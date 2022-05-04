(ns oliver-game.endpoint.routes
  (:require [hiccup.page :as hiccup]
            [reitit.core :as r]
            [reitit.ring :as rring]
            [ring.util.response :as resp]
            [ring.middleware.defaults :as ring-defaults]
            [oliver-game.hiccup :as cljc-hiccup]
            [oliver-game.net :as net]
            [oliver-game.server :as server]
            [oliver-game.version :refer [app-version-info]]))

(defn make-page-ctx [{::r/keys [router]}]
  (let [{:keys [version]} (app-version-info)]
    {:css-href (-> router (r/match-by-name ::css {:version version}) (r/match->path))
     :js-href (-> router (r/match-by-name ::js {:version version}) (r/match->path))}))

(defn- hiccup-response [body]
  (-> (hiccup/html5 {:lang "cs"}
                    body)
      (resp/response)
      (resp/content-type "text/html")
      (resp/charset "utf-8")))

(defn index [req]
  (hiccup-response
    (cljc-hiccup/cljs-landing-page (make-page-ctx req))))

(defn css [_]
  (-> (resp/resource-response "site.css" {:root "public/css"})
      (resp/content-type "text/css")
      (resp/header "ETag" (str "Version-" (:version (app-version-info))))))

(defn js [_]
  (-> (resp/resource-response "app-main.js" {:root "public/cljs-out"})
      (resp/content-type "application/javascript")
      (resp/header "ETag" (str "Version-" (:version (app-version-info))))))

(defn ws [event-queue req]
  (net/ws-handler (fn on-message [channel data]
                    (#'server/ws-consumer event-queue req channel data))))

(defn make-router [{:keys [event-queue]}]
  (rring/router [["/"
                  {:name ::index
                   :get index}]

                 ["/css/site.css/:version"
                  {:name ::css
                   :get css}]

                 ["/js/compiled/app.js/:version"
                  {:name ::js
                   :get js}]

                 ["/ws"
                  {:name ::ws
                   :get (partial ws event-queue)}]

                 ]))

(defn make-handler [deps]
  (rring/ring-handler
    (make-router deps)
    (rring/routes
      (rring/create-resource-handler {:path "/"})
      (rring/create-default-handler))
    {:middleware [[ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                                   (assoc :proxy true
                                                          :security {:anti-forgery false}))]]}))
