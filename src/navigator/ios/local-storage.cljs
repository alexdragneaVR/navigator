(ns vr.pixa.local-storage
  (:require [cljs.core.async :refer [put! promise-chan] :as async]
            [cljs.reader :as reader]
            [vr.pi :refer [error]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [vr.macros :refer [gosafe]]))

(def ReactNative (js/require "react-native"))
(def AsyncStorage (-> ReactNative .-AsyncStorage))
(def NetInfo (-> ReactNative .-NetInfo))




(defn net-connected
  "returns a channed of network connection events
  events can be
      :connected when network connects
      :disconnected when network disconnects"
  []
  (let [out (async/chan)]
    (-> NetInfo
      .-isConnected
      (.addEventListener "connect"
        #(put! out [true (if % :connected :disconnected)])))
    out))


(defn net-connected?
  "Return a promise-chan that will have either
  true or false based on wether or not the network is currently connected"
  []
  (let [out (promise-chan)
        handler (fn handler [connection]
                  (put! out [true (if connection :connected :disconnected)])
                  (-> NetInfo .-isConnected
                      (.removeEventListener "connect" handler)))]
    (-> NetInfo
      .-isConnected
      (.addEventListener "connect" handler))
    (-> NetInfo
      .-isConnected
      .fetch
      (.then #(put! out (if % [true :connected] [true :disconnected])))
      (.catch #(put! out [false %])))
    out))

(comment
  (let [connection-info (net-connected)]
    (go-loop []
      (println (<! connection-info))
      (recur)))
  (go
    (println (<! (net-connected?)))))

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
      (.then #(put! result [true (reader/read-string %)]))
      (.catch #(put! result [false %])))
    result))


(comment
  ;;save a value
  (go (println (<! (save! :value {:hello "world"})))) ;; should return [true {:hello "world"}]
  (go (println (<! (save! {:hello "world"} {:hello "world"})))) ;; should return [true {:hello "world"}]

  (go (println (<! (load! {:hello "world"})))) ;; should return [true {:hello "world"}]

  (go (println (<! (load! :caca)))) ;; should return [true nil]
  (go (println (<! (load! :value))))) ;;should return [true {:hello "world"}]
