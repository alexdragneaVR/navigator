(ns vr.pixa.rest
  (:require
    [cljs.core.async :refer [chan <! >! put! take! promise-chan]]
    [goog.events :as events]
    [goog.structs :as structs]
    [goog.Uri.QueryData :as query-data]
    [goog.net.XhrIo :as xhr])
  (:require-macros [cljs.core.async.macros :refer [go]]))



(defn- -handle-post [url token json]
  (let [request (new goog.net.XhrIo)
        result (promise-chan)]
    (events/listen request "complete" (fn []
                                        (let [response (js->clj (.getResponseJson request) :keywordize-keys true)]
                                          (put! result (:data response))
                                          (println "received response" result))))
    (println "POST" url json)
    (.send request url "POST" (.stringify js/JSON (clj->js json)) #js{"Content-Type" "application/json"
                                                                      "Cookie" token})
    result))

(def authinfo
  (atom {:username "admin@vr.com"
         :password "1234"
         :url "https://dev.visualretailingcloud.com/api/1/login"
         :session-token nil}))


(defn- -login [authinfo]
  (let [url (:url @authinfo)
        data {:username (:username @authinfo)
              :password (:password @authinfo)}
        request (new goog.net.XhrIo)
        result (promise-chan)]
    (events/listen request "complete" (fn []
                                          (swap! authinfo merge {:session-token (aget (.getResponseHeaders request) "Set-Cookie")})
                                          (put! result @authinfo)))
    (.send request url "POST" (.stringify js/JSON (clj->js data)) #js{"Content-Type" "application/json"})
    result))

(defn POST< [url json]
  (let [url (str "https://dev.visualretailingcloud.com" url)]
    (if-not (nil? (:session-token @authinfo))
      (-handle-post url (:session-token @authinfo) json)
      (go (<! (-login authinfo))
          (<! (-handle-post url (:session-token @authinfo) json))))))
