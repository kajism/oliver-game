(ns oliver-game.hiccup)

(def html-title "Oliver's Game")

(defn loading []
  [:div
   [:div.ui.active.centered.huge.text.loader "Loading"]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]
   [:br]])

(comment ;;page ctx example
  {:title nil ;; defaults to html-title
   :css-href "css/site.css"
   :js-href "js/compiled/app.js"})

(defn- head-and-body [{:keys [title css-href] :or {title html-title}} body]
  (list
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title title]
     [:link {:rel "stylesheet" :href "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.4.1/semantic.min.css"}]
     [:link {:rel "stylesheet" :href "css/semantic.min.css"}]
     [:link {:rel "stylesheet" :href css-href}]]
    [:body
     body]))

(defn cljs-landing-page [{:keys [js-href] :as page-ctx}]
  (head-and-body page-ctx
                 [:div#app
                 (loading)
                 [:script {:src js-href}]
                 [:script "oliver_game.core.init();"]]))

