(ns vr.pixa.components
  (:require [reagent.core :as r]))

(def ReactNative (js/require "react-native"))
(def ImagePicker (js/require "react-native-image-picker"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def button (r/adapt-react-class (.-Button ReactNative)))
(def rn-list-view (r/adapt-react-class (.-ListView ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def animated-view (r/adapt-react-class (-> ReactNative .-Animated .-View)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def camera-roll (.-CameraRoll ReactNative))
; (def camera (r/adapt-react-class (.-Camera ReactNativeCamera)))

(def animated-value (r/adapt-react-class (-> ReactNative .-Animated .-Value)))
(def animated-timing (r/adapt-react-class (-> ReactNative .-Animated .-timing)))
(def ease (r/adapt-react-class (-> ReactNative .-Easing .-ease)))
(def ease-out (r/adapt-react-class (-> ReactNative .-Easing .-out)))

(def DataSource (.-DataSource (.-ListView ReactNative)))

(def icon-back (js/require "./images/icon-back.png"))
(def chat-icon (js/require "./images/chat-icon.png"))
(def project-icon (js/require "./images/project-icon.png"))

(defn animated-event [props]
  (let [event-fn (-> ReactNative .-Animated .-event)]
    (event-fn (clj->js props))))
