(ns oliver-game.version
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import java.net.JarURLConnection
           java.util.Date))

(defn read-app-version-info
  "Returns a map with :date instant and :version number string"
  []
  (let [pom-url (io/resource "META-INF/maven/oliver-game/oliver-game/pom.properties")
        pom (some-> pom-url
                    (slurp)
                    (str/split #"\n")
                    (->> (map #(str/split % #"="))
                         (filter #(= 2 (count %)))
                         (into {})))]
    (if-not pom-url
      {:date (Date.)
       :version "Dev"}
      {:date (let [c (.openConnection pom-url)]
               (Date.
                 (if (instance? JarURLConnection c)
                   (.getTime (.getJarEntry ^JarURLConnection c))
                   (let [ms (.getLastModified c)]
                     (.close (.getInputStream c))
                     ms))))
       :version (get pom "version")})))

(def app-version-info (memoize read-app-version-info))
