(defproject blog "0.1.0-SNAPSHOT"
  :description "Clojure Blog"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [markdown-clj "0.9.35"]
                 [org.clojure/data.json "0.2.3"]
                 [clj-time "0.6.0"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [org.clojure/data.xml "0.0.7"]]
  :plugins [[lein-ring "0.8.8"]]
  :ring {:handler blog.handler/app}
  :uberjar-name "blog.jar"
  :profiles 
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
