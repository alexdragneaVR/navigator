(ns vr.pixa.local-storage
  (:require [cljs.core.async :refer [put! promise-chan] :as async]
            [cljs.reader :as reader]
            [vr.pi :refer [error]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [vr.macros :refer [gosafe]]))

(def ReactNative (js/require "react-native"))
(def AsyncStorage (-> ReactNative .-AsyncStorage))
(def NetInfo (-> ReactNative .-NetInfo))
(def RNFetchBlob (.-default (js/require "react-native-fetch-blob")))
(def ImagePicker (js/require "react-native-image-picker"))

(def SHA1 (js/require "crypto-js/sha1"))


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


(defn fetch-file! [url]
  (let [result (promise-chan)]
    (-> RNFetchBlob
      (.config #js{})
      (.fetch "GET" url)
      (.then #(.text %))
      (.then #(put! result [true %]))
      (.catch #(put! result [false %])))
    result))

(defn fetch-image-base64! [url]
  (let [result (promise-chan)]
    (-> RNFetchBlob
      (.config #js{})
      (.fetch "GET" url)
      (.then #(.base64 %))
      (.then #(str "data:image/jpeg;base64," %))
      (.then #(put! result [true %]))
      (.catch #(put! result [false %])))
    result))

(defn load-image-base64! [path]
  (let [result (promise-chan)]
    (-> RNFetchBlob
      .-fs
      (.readFile path "base64")
      (.then #(str "data:image/jpeg;base64," %))
      (.then #(put! result [true %]))
      (.catch #(put! result [false %])))
    result))

(defn show-image-picker!
  ([] (show-image-picker! nil))
  ([options]
   (let [result (promise-chan)]
     (-> ImagePicker
        (.showImagePicker options #(put! result [true (.-data %)])))
     result)))


(defn take-photo! []
  (go
    (let [[_ path] (<! (show-image-picker!))]
      [true (str "data:image/jpeg;base64," path)])))

(defn with-cache [f]
  (fn get-file! [url]
    (go (let [key (SHA1 url)
              [cache-ok? cached-value] (<! (load! key))]
          (if (and cache-ok? (not (nil? cached-value)))
            [true cached-value]
            (let [[ok? result] (<! (f url))]
              (if ok?
                (do (save! key result)
                    [true result])
                [false result])))))))

(def get-file-contents! (with-cache fetch-file!))

(def get-image-base64! (with-cache fetch-image-base64!))


(comment
  ;this clears the whole thing, for all apps
  (.clear AsyncStorage)

  (go (println (<! (get-file-contents! "http://10.0.1.28:8080/static/3d/fixture1.mtl"))))
  ;;save a value
  (go (println (<! (save! :value {:hello "world"})))) ;; should return [true {:hello "world"}]
  (go (println (<! (save! {:hello "world"} {:hello "world"})))) ;; should return [true {:hello "world"}]

  (go (println (<! (load! {:hello "world"})))) ;; should return [true {:hello "world"}]

  (go (println (<! (load! :caca)))) ;; should return [true nil]
  (go (println (<! (load! :value))))) ;;should return [true {:hello "world"}]
