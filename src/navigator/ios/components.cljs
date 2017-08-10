(ns vr.pixa.components
  (:require [reagent.core :as r]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def button (r/adapt-react-class (.-Button ReactNative)))
(def rn-list-view (r/adapt-react-class (.-ListView ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def animated-view (r/adapt-react-class (-> ReactNative .-Animated .-View)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))


(def DataSource (.-DataSource (.-ListView ReactNative)))
