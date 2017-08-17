(ns vr.pixa.components
  (:require [reagent.core :as r]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def button (r/adapt-react-class (.-Button ReactNative)))
(def rn-list-view (r/adapt-react-class (.-ListView ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def animated-view (r/adapt-react-class (-> ReactNative .-Animated .-View)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def web-view (r/adapt-react-class (.-WebView ReactNative)))

(def DataSource (.-DataSource (.-ListView ReactNative)))

(defn animated-event [props]
  (let [event-fn (-> ReactNative .-Animated .-event)]
    (event-fn (clj->js props))))


(defn web-view-event [{:keys [event] :as raw-event}]
  (if (nil? event)
    [:web-3d-view/bad-messages-from-viewer raw-event]
    (let [event-type (keyword "web-3d-view" event)]
      [event-type raw-event])))


(defn web-3d-view [{:keys [url action-input action-output] :as props}]
  (let [webview (atom nil)
        input (a/chan 1 (comp (map (fn [message] {:data message}))
                              (map clj->js)
                              (map #(.stringify js/JSON %))))

        output (a/chan 1 (comp (map #(-> % .-nativeEvent .-data))
                               (map #(.parse js/JSON %))
                               (map #(js->clj % :keywordize-keys true))
                               (map :data)
                               (map web-view-event)))
        closer (a/chan)]

    (a/pipe action-input input)
    (a/pipe output action-output)

    (go-loop []
      (let [[val c] (a/alts! [input closer])]
        (when-not (or (= c closer) (nil? val))
          (try
            (some-> @webview (.postMessage val))
            (catch :default e
              (.trace js/console e)))
          (recur))))

    (r/create-class
      {:reagent-render
         (fn [{:keys [url style action-input action-output] :as props}]
           [view (merge style {:on-layout #(let [values (-> % .-nativeEvent .-layout)
                                                 layout {:layout {:x (.-x values) :y (.-y values)
                                                                  :width (.-width values) :height (.-height values)}}]
                                              (a/put! action-output [:web-3d-view/on-layout layout]))})

            [web-view {:ref #(reset! webview %)
                       :source {:uri url}
                       :bounces false
                       :scroll-enabled false
                       :on-message #(a/put! output %)}]])


       :component-will-unmount
         (fn [_] (a/put! closer :close) (a/close! closer))})))
