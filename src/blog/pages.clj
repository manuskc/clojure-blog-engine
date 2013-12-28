(ns blog.pages
  (:use [markdown.core]
        [clj-time.coerce]
        [clj-time.format])
  (:require [clojure.data.json :as json]
            [blog.library :as lib]
            [clj-time.core :as time]
            [clojure.data.xml :as xml]))

(def read-file (lib/memoize-for-a-while slurp :minutes 120))

(def read-post (lib/memoize-for-a-while #(md-to-html-string (slurp %)) :minutes 1440))

(def read-json (lib/memoize-for-a-while (fn [path & args] (apply json/read-str (slurp path) args)) :minutes 1440))

(defn get-blog-meta-data [] 
  (:blog (read-json "resources/private/posts/posts.json" :key-fn keyword)))

(def page-head (lib/memoize-for-a-while 
                 #(clojure.string/replace (slurp "resources/private/head.html.template") #"\{TITLE\}" (:title (get-blog-meta-data))) 
                 :minutes 120))

(def page-tail (lib/memoize-for-a-while 
                 #(clojure.string/replace (slurp "resources/private/tail.html.template") #"\{AUTHOR\}" (:author (get-blog-meta-data))) 
                 :minutes 120))

(def page-comments (lib/memoize-for-a-while 
                     #(if (empty? (:disqus-short-name (get-blog-meta-data)))
                        (str "")
                        (clojure.string/replace (slurp "resources/private/commentsection.html.template") #"\{DISQUS\}" (:disqus-short-name (get-blog-meta-data))))
                     :minutes 120))

(defn get-posts-sorted []
  (lib/index-map-list 
    :index 
    (sort-by :created #(compare (to-long %2) (to-long %1))
             (map #(let [post-date (parse (formatters :year-month-day) (:created %))]  
                     (assoc % :year (time/year post-date) :month (time/month post-date) :date (time/day post-date)))
                  (:posts (read-json "resources/private/posts/posts.json" :key-fn keyword))))))

(def get-posts-sorted (lib/memoize-for-a-while get-posts-sorted :minutes 60))

(defn page [& content] 
  (str (page-head) (apply str content) (page-tail)))

(defn get-title-tag [title] 
  (str "<div id='articleheading'>" title "</div>"))

(defn get-date-tag [date] 
  (str "<div id='date'>" date "</div>"))

(defn get-prev-link [index]
  (if (> index 0) 
    (let [post (nth (get-posts-sorted) (dec index))] 
      (str "<a class='prev-link' href='/post/" (subs (:file post) 0 (.lastIndexOf (:file post) ".")) "'>&laquo; "  (:title post) "</a>"))))

(defn get-next-link [index]
  (let [post-count (count (get-posts-sorted))] 
    (if (< index (dec post-count)) 
      (let [post (nth (get-posts-sorted) (inc index))] 
      (str "<a class='next-link' href='/post/" (subs (:file post) 0 (.lastIndexOf (:file post) ".")) "'>"  (:title post) " &raquo;</a>")))))

(defn get-post-position [index]
  (str "<span class='post-index'>" (inc index) "/" (count (get-posts-sorted)) "</span>"))

(defn get-navigator [index]
    (str "<div id='navigator'><hr/>" (get-prev-link index) (get-post-position index) (get-next-link index) "</div>"))

(defn get-tags-div [tags]
  (str "<br/><div id='tags'>Tags : " (reduce #(str %1 "<a href='/tagged/" %2 "'>" %2 "</a> &#149; ") "" tags) "</div>"))

(defn render-post [post]
  (page (get-title-tag (:title post)) 
        (get-date-tag (str (:year post) " " (lib/get-month-name (:month post)) " " (:date post))) 
        (read-post (str "resources/private/posts/" (:file post)))
        (get-navigator (:index post))
        (get-tags-div (:tags post))
        (page-comments)))

(defn get-name-index-map []
  (reduce #(assoc %1 (:file %2) (:index %2)) {} (get-posts-sorted)))

(def get-name-index-map (lib/memoize-for-a-while get-name-index-map :minutes 60))

(defn get-404-page []
  (page (get-title-tag "4oh4") (read-post (str "resources/private/posts/404.md"))))

(defn get-about-page []
  (page (get-title-tag "About Me") (read-post (str "resources/private/posts/about.md"))))

(defn get-home-page [] 
  (render-post (first (get-posts-sorted))))

(defn get-page [post-name]
  (let [file-name (str post-name ".md")]
    (if (contains? (get-name-index-map) file-name) 
      (render-post (nth (get-posts-sorted) ((get-name-index-map) file-name))) 
      (get-404-page))))

(defn get-post-link [post]
  (let [file (:file post)]
    (str "<li class='post'><a href='/post/" (subs file 0 (.lastIndexOf file ".")) "'>" (:title post) "</a></li>\n" )))

(defn get-month-link [post]
  (str "<li class='month'>" (lib/get-month-name (:month post)) "</li>\n" ))

(defn get-year-link [post]
  (str "<li class='year'>" (:year post) "</li>\n" ))

(defn get-archive-link-text [post last-year last-month]
  (if (= (:year post) last-year) 
    (if (= (:month post) last-month) 
      (str (get-post-link post)) 
      (str (get-month-link post) (get-post-link post))) 
    (str (get-year-link post) (get-month-link post) (get-post-link post))))

(defn get-archive-list [post-map & date]
  (let [last-year (if (empty? date) 0 (nth date 0)),
        last-month(if (empty? date) 0 (nth date 1)),
        post (first post-map)] 
    (if (not (empty? post-map)) (str (get-archive-link-text post last-year last-month) (get-archive-list (rest post-map) (:year post) (:month post))))))

(defn get-archive-page [] 
  (page (get-title-tag "Archive") "<ul>\n" (get-archive-list (get-posts-sorted)) "</ul>\n" ))

(defn get-archive-for-tag [tag]
  (page (get-title-tag (str "Tagged: <i>" tag "</i>")) "<ul>\n" (get-archive-list (filter #(some (fn [t] (= t tag)) (:tags %)) (get-posts-sorted))) "</ul>\n" ))

;Source: http://en.wikipedia.org/wiki/RSS
;<item>
;  <title>Example entry</title>
;  <description>Here is some text containing an interesting description.</description>
;  <link>http://www.wikipedia.org/</link>
;  <guid>unique string per item</guid>
;  <pubDate>Mon, 06 Sep 2009 16:20:00 +0000 </pubDate>
; </item>

(defn get-rss-item [post]
  (xml/element :item {} 
               (xml/element :title {} (:title post))
               (xml/element :description {} "")
               (xml/element :link {} (str (:domain (get-blog-meta-data)) "/post/" (subs (:file post) 0 (.lastIndexOf (:file post) "."))))
               (xml/element :guid {} (subs (:file post) 0 (.lastIndexOf (:file post) ".")))
               (xml/element :pubDate {} (:created post))))

;<?xml version="1.0" encoding="UTF-8" ?>
;<rss version="2.0">
;<channel>
; <title>RSS Title</title>
; <description>This is an example of an RSS feed</description>
; <link>http://www.someexamplerssdomain.com/main.html</link>
; <lastBuildDate>Mon, 06 Sep 2010 00:01:00 +0000 </lastBuildDate>
; <ttl>1800</ttl>
;[<item>...</item>]*
;</channel>
;</rss>
; (print (xml/indent-str (apply xml/element :test {} (map get-rss-item (get-posts-sorted)))))

(defn get-rss-feed []
  (let [blog-data (get-blog-meta-data)]
    (xml/indent-str 
      (xml/element :rss {:version "2.0"} 
                   (xml/element :channel {} 
                                (xml/element :title {} (:title blog-data)) 
                                (xml/element :description {} (:description blog-data)) 
                                (xml/element :link {} (:domain blog-data)) 
                                (xml/element :lastBuildDate {} (:created (first (get-posts-sorted)))) 
                                (xml/element :ttl {} "1800") 
                                (map get-rss-item (get-posts-sorted)))))))

(def get-rss (lib/memoize-for-a-while #(hash-map :headers {"Content-Type" "text/xml; charset=utf-8"} :body (get-rss-feed)) :minutes 60))

(defn reload-service [password]
  (if (= (:reload-password (get-blog-meta-data)) password) 
    (page "<h1>RELOAD COMPLETE</h1>Reloaded " (count (lib/reset-all-memoized-functions)) " functions") 
    (page "<h1>ERROR</h1>")))