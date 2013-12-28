(ns blog.library
  (:require [clj-time.core :as time]))

(defn memoize-for-a-while
  "Returns a memoized version of the function
  The results are cached only for a given time
  The function is re-executed after the time out"
  [f & options]
  (let [mem (atom {}), opts (apply hash-map options)]
    (fn [& args]
      (if (= :reset! (first args)) 
        (swap! mem (fn [m] {})) 
        (if (or 
              (not (contains? @mem args)) 
              (time/after? (time/now) (time/plus (:date (@mem args)) (time/minutes (if (contains? opts :minutes) (:minutes opts) 60)))))
          (let [res (apply f args)]
            (swap! mem assoc args (hash-map :res res :date (time/now))) res) 
          (:res (@mem args)))))))

(defn store-all-first-args [f]
  (let [mem (atom #{})]
    (fn [first-param & args]
      (if (= :apply-on-all-first-params first-param) 
        (map (first args) @mem) 
        (let [res (apply f first-param args)] 
          (swap! mem conj res) res)))))

(def memoize-for-a-while (store-all-first-args memoize-for-a-while))

(defn reset-all-memoized-functions []
  (memoize-for-a-while :apply-on-all-first-params #(% :reset!)))

(defn get-month-name [month]
  (let [monthNames ["", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]]
    (nth monthNames month)))

(defn index-map-list [key list & start]
  (let [index (if (empty? start) 0 (first start))]
    (if (not (empty? list)) (cons (assoc (first list) key index) (index-map-list key (rest list) (inc index))))))