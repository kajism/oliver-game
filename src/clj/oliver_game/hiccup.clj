(ns oliver-game.hiccup
  (:require [clojure.pprint :refer [pprint]]
            [hiccup.page :as hiccup]
            [ring.util.response :as response]
            [oliver-game.common :as common]))

(defn hiccup-response
  [body]
  (-> (hiccup/html5 {:lang "cs"}
                    body)
      (response/response)
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn- hiccup-frame [version body]
  (list
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title common/html-title]
    [:link {:rel "stylesheet" :href "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.4.1/semantic.min.css"}]
    [:link {:rel "stylesheet" :href "css/semantic.min.css"}]
    [:link {:rel "stylesheet" :href (str "css/site.css/" version)}]]
   [:body
    body]))

(defn cljs-landing-page [version]
  (hiccup-response
   (hiccup-frame version
    [:div#app
     (common/loading)
     [:script {:src (str "js/compiled/app.js/" version)}]
     [:script "oliver_game.core.init();"]])))

(defn login-page
  ([title] (login-page title nil))
  ([title msg]
   (hiccup-response
    (hiccup-frame
     title
     [:div.ui.container
      [:br]
      [:h2.ui.header "Login form"]
      (when msg
        [:div.ui.red.message msg])
      [:form.ui.form {:method "post" :role "form"}
       [:div.field
        [:label {:for "username"} "Username"]
        [:input#username {:name "username" :type "text"}]]
       [:div.field
        [:label {:for "heslo"} "Password"]
        [:input#heslo {:name "pwd" :type "password"}]]
       [:button.ui.button {:type "submit"} "Login"]]]))))
