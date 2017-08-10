(ns navigator.ios.core
  (:require
    [reagent.core :as r]
    [vr.pixa.pixa-model :refer [model]]
    [vr.pixa.pixa-controller :as controller]
    [vr.pixa.components :refer [app-registry]]
    [vr.pixa.pixa-views :refer [screen-component]]))


(defn app-root []
  [screen-component @model])

(defn init []
  (.registerComponent app-registry "navigator" #(r/reactify-component app-root))
  (controller/init))
