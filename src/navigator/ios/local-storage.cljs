(ns vr.pixa.local-storage
  (:require [cljs.core.async :refer [put! promise-chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def AsyncStorage (-> (js/require "react-native") .-AsyncStorage))


(defn save! "
   Saves a value by given key to local storage.
   returns a promise channel that contains
      [true value] if successfull
      [false error] if an error has occured while saving the value

   Params :
      key   - any valid clojure value
      value - any valid clojure value
  "
  [key value]
  (let [result (promise-chan)]
    (-> AsyncStorage
      (.setItem (pr-str key) (pr-str value))
      (.then #(put! result [true value]))
      (.catch #(put! result [false %])))
    result))


(defn load! "
   Loads given key from local storage.
   Returns a promise channel that contains
      [true loaded-value] if successfull
      [false error] if not successfull

   Params :
      key   - any valid clojure value
  "
  [key]
  (let [result (promise-chan)]
    (-> AsyncStorage
      (.getItem (pr-str key))
      (.then #(put! result [true (cljs.reader/read-string %)]))
      (.catch #(put! result [false %])))
    result))


(comment
  ;;save a value
  (go (println (<! (save! :value {:hello "world"})))) ;; should return [true {:hello "world"}]
  (go (println (<! (save! {:hello "world"} {:hello "world"})))) ;; should return [true {:hello "world"}]

  (go (println (<! (load! {:hello "world"})))) ;; should return [true {:hello "world"}]

  (go (println (<! (load! :caca)))) ;; should return [true nil]
  (go (println (<! (load! :value))))) ;;should return [true {:hello "world"}]