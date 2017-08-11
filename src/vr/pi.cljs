(ns vr.pi
  (:require [reagent.core :as r :refer [atom]]
            [cljs.pprint :refer [pprint cl-format]]
            [cljs-time.core :as d]
            [cljs-time.format :as dt])

  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [vr.macros :refer [safe gosafe <?]]))


(def errors (r/atom #{}))
(def loading (r/atom {:loading false :percentage nil}))


(defn swapm! [x y]
;;   (println (clj->js
            (swap! y (fn [xx] x)))
;;             ))



;;pretty print
(defn info [ & x]
  (pprint  x))
;;   (do (pprint x)
;;     "------"))

(defn error [ & x]
  (pprint  x))

(defn debug-state [state]
  (info 200 state)
  state)

;; (format-number   1.2345 "~,2f") ; => returns "1.23"
;; (format-number   1.2345) ; => returns "1.23"
(defn format-number
  ([number]
   (format-number number "~,2f"))
  ([number number-format]
   (cl-format nil  number-format number)))



(defn notnil? [x]
  (not (nil? x)))


(defn copy-value-in [object source-path destination-path]
  (assoc-in object destination-path (get-in object source-path)))



(def default-date-formatter (dt/formatter "yyyy-MM-dd hh:mm:ss"))

(defn format-date [date-as-string-in-default-format output-python-format]
  (if (notnil? date-as-string-in-default-format)
    (try
      (dt/unparse
       (dt/formatter
        (condp = output-python-format
          "yy-mm-dd" "yyyy-MM-dd hh:mm:ss"
          "mm-dd-yy" "MM-dd-yyyy hh:mm:ss"
          "dd-mm-yy" "dd-MM-yyyy hh:mm:ss"
          "yyyy-MM-dd hh:mm:ss"))
       (dt/parse default-date-formatter date-as-string-in-default-format))
      (catch :default e
        date-as-string-in-default-format))))


(defn add-error [error]
  (swap! loading assoc :loading false)
;;   (swap! errors conj  error))
  (swap! errors #(into #{} (conj % error))))

(defn add-errors [error-list]
  (swap! loading assoc :loading false)
  (swap! errors #(into #{} (concat error-list %))))




(def memoized-sort-vals
  (memoize sort-by))


(defn sort-vals
  ([sort-fn  obj]
   (sort-vals sort-fn < obj true))
  ([sort-fn sort-direction obj]
   (sort-vals sort-fn sort-direction obj true))
  ([sort-fn sort-direction obj memoize?]
   (let [values (if (map? obj) (vals obj) obj)]
    (if memoize?
      (memoized-sort-vals sort-fn sort-direction values)
      (sort-by sort-fn sort-direction values)))))





(defn sortby [k direction coll]
;;   (sort-by #(if (notnil? (k %)) (clojure.string/lower-case (k %)) (k %)) direction coll)
  (sort-vals #(if (notnil? (k %)) (clojure.string/lower-case (k %)) (k %)) direction coll))


(defn clear-errors []
  (reset! errors []))


(defn url [path]
  (str "http://localhost:8080/" path))


(defn ^:extern set-loading [state value]
  (swap! loading assoc :loading value)
  state)

;; TODO: Use this for text translations
(defn text [t]
   (let [translated (aget js/text t)]
     (if (not (nil? translated))
       translated
       (str "??" t "??"))))

(defn keywordize [x]
  (keyword (str x)))

(defn to-map [coll k]
  (let [r (group-by k coll)]
    (zipmap (map #(keyword (str %)) (keys r)) (map #(get % 0) (vals r)))))



(defn assoc-multi [state dic]
  (if (> (count dic) 1)
    (let [f (first dic)]
      (assoc-multi (assoc state (f 0) (f 1)) (rest dic)))
    (let [f (first dic)]
      (assoc state (f 0) (f 1)))))

(defn assoc-multi-in [state dic]
  (if (> (count dic) 1)
    (let [f (first dic)]
      (assoc-multi-in (assoc-in state (f 0) (f 1)) (rest dic)))
    (let [f (first dic)]
      (assoc-in state (f 0) (f 1)))))



(defn update-values-in-map [m f & args]
 (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

;; (map-vals inc {:a 1, :b 2})
;; => {:a 2, :b 3}
(defn map-vals [f m]
  (zipmap (keys m)
          (map f (vals m))))

;; (map-keys inc {1 "a", 2 "b"})
;; => {2 "a", 3 "b"}
(defn map-keys [f m]
  (zipmap (map f (keys m))
          (vals m)))


;; (map-keys-vals inc inc {1 1, 2 2})
;; => {2 2, 3 3}
(defn map-keys-vals [kfn vfn m]
  (zipmap (map kfn (keys m))
          (map vfn (vals m))))


;;DEPRECATED
(defn safe-run [func]
  (try
    (func)
    (catch :default e
      (add-error (str e))
      (info e))))




(defn throw-err [something]
  (if
    (or
     (= (type something) js/Error.)
     (= (type something) ExceptionInfo))

    (throw something)
    ;;else
    something))



(defn index-exclude [r ex]
   "Take all indices execpted ex"
    (filter #(not (ex %)) (range r)))


(defn dissoc-idx [v & ds]
   (map v (index-exclude (count v) (into #{} ds))))


(defn dissoc-in-remove-empty-struct
  {:added "0.1.0"}
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in-remove-empty-struct nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn dissoc-in
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (assoc m k newmap))
      m)
    (dissoc m k)))





(defn deep-merge
  "Merge maps, recursively merging nested maps whose keys collide."
  ([] {})
  ([m] m)
  ([m1 m2]
   (reduce (fn [m1 [k2 v2]]
             (if-let [v1 (get m1 k2)]
               (if (and (map? v1) (map? v2))
                 (assoc m1 k2 (deep-merge v1 v2))
                 (assoc m1 k2 v2))
               (assoc m1 k2 v2)))
           m1 m2))
  ([m1 m2 & more]
   (apply deep-merge (deep-merge m1 m2) more)))


(defn ^:extern jserrors []
  (clj->js @errors))


(defn translate [mykey mystring]
  ;; mykey = "en_GB"
  ;; mystring = "fr_FR:Nom|ro_RO:Nume|en_GB:Name|en_US:Name|nl_NL:|de_DE:|es_ES:|it_IT:|af_ZA:"
  ;;   => "Name"
  ;; mykey = "af_ZA" => "af_ZA" !!!
  (def item (filter #(= mykey (get % 0)) (map #(clojure.string/split % ":") (clojure.string/split mystring "|"))))
  (last (first item)))


(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (let [result (str text)]
    ;;(clojure.string/escape result {\< "&lt;", \> "&gt;", \& "&amp;",  "\"" "&quot;", "'" "&apos;"})
    (clojure.string/escape result {\< "&lt;", \> "&gt;", "'" "&apos;", "|" "&#124;"})))



(defn un-escape-html
  "Change HTML character entities into special characters."
  [text]
  (let [result (str text)]
;;     (clojure.string/replace
      (clojure.string/replace
        (clojure.string/replace
          (clojure.string/replace
            (clojure.string/replace
              (clojure.string/replace
                (clojure.string/replace result #"&lt;" "<")
                #"&gt;" ">")
              #"&apos;" "'")
            #"&amp;" "&")
          #"&quot;" "\"")
        #"&#x27;" "'")))
;;       #"&#124;" "|")



(defn input-value [ev]
  "Trim string and escape html tags"
  (escape-html (clojure.string/trim (-> ev .-target .-value))))


(defn exclude-keys [exclude-set dict]
  (into {} (filter (fn [[k v]] (not (some exclude-set [k]))) dict)))


(defn change! [modelatom changes]
  (safe
    (-> @modelatom
        (assoc-multi-in changes)
        (swapm! modelatom))))


(defn round-to
  "Round a double to the given precision (number of significant digits)"
  [d precision]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))
