(ns blog.handler
  (:use [compojure.core]
        [markdown.core])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [blog.pages :as pages]
            [clj-time.core :as time]
            [ring.adapter.jetty :as jetty]
            ))

(defroutes app-routes
  (GET "/" [] (pages/get-home-page))
  (GET ["/:index" :index #"index\.[a-z]{2,4}"] [] (pages/get-home-page)) ;;Just for fun! index.htm, index.html, index.php, index.py, index.jsp, index.rb :D
  (GET "/about" [] (pages/get-about-page))
  (GET "/archive" [] (pages/get-archive-page))
  (GET "/tagged/:tag" [tag] (pages/get-archive-for-tag tag))
  (GET "/rss" [] (pages/get-rss))
  (GET "/post/:page-name" [page-name] (pages/get-page page-name))
  (GET "/reload" [password] (pages/reload-service password))
  (route/resources "/")
  (route/not-found (pages/get-404-page)))

(def app
  (handler/site app-routes))

(defn -main [port]
  (jetty/run-jetty app {:port (Integer. port) :join? false}))

