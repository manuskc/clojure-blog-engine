# Blog

This is a simple blog engine written in clojure (using compojure)
Some features:
* This does not need any database to host this application. 
* Heavy use of in-memory caching to improve performance
* Supports commenting via disqus
* Write posts using markdown

# How to get started

* One time setup  - update "blog" section in resources/private/posts/posts.json
* All posts are to be written in markdown and the files are to be dropped in resources/private/posts.
* Control which posts appear in blog by adding or removing entry from "posts" section in resources/private/posts/posts.json
* Any publicly available static files needs to be dropped in resources/public (ex: a file hello.txt here will be accessable at mydomain.com/hello.txt)


# Where to host?
Use heroku! I host my blog manuskc.com there ;-)
This project includes necessary files and is compatible for hosting in heroku. Just create a new project in heroku and check this code in!


## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application locally, run:

    lein run -m blog.handler 5000
