(ns vr.pixa.pixa-views
  (:require [reagent.core :as r]
            [vr.pixa.pixa-controller :as controller]
            [vr.pixa.components :refer [ReactNative text view button rn-list-view scroll-view animated-view
                                        touchable-highlight DataSource]]))

; styles
(def list-view-row
  {:height 30})





(def Dimensions (.-Dimensions ReactNative))

(defn data-source [props]
  (new DataSource (clj->js props)))

(defn clone-ds-with-rows [ds rowData]
  (.cloneWithRows ds (clj->js rowData)))

(defn list-view-item [func item]
  [text {:style list-view-row
         :on-press #(func (:id item))}
        ;  #(swap! offset assoc :x -375)}
        (:name item)])




(defn list-view [props]
  (let [data-source (data-source {:rowHasChanged not=})]
    (fn [props]
      [rn-list-view {:style (:style props)
                     :dataSource (clone-ds-with-rows data-source (or (:items props) []))
                     :renderRow #(r/as-element [(:renderRow props) (js->clj % :keywordize-keys true)])
                     :enableEmptySections true}])))

(defn header [title left-button right-button]
  [view {:style {:flex-direction "row" :justify-content "space-between"}}
   (if left-button
     [button {:style {:text-align "left"}
              :on-press controller/back
              :title left-button}]
     [text ""])
   [text {:style { :font-size 18 :text-align "center" :margin-top 7}}
         title]
   (if right-button
     [button {:style {:text-align "right"}
              :on-press #()
              :title right-button}]
     [text ""])])


(defn screen-component [state]
  (let [dataSource (data-source {:rowHasChanged not=})
        width (.-width (Dimensions.get "window"))
        [page id] (controller/current-page state)
        offset (cond
                     (= :teams page) 0
                     (= :team page) -375
                     (= :topic page) -750
                    :else 0)]


      [animated-view {:style {:flex-direction "row"
                              :width (* 3 width)
                              :transform [{:translateX offset}]}}
       [view {:style {:margin-top 20
                      :width width}}
        [header "Teams" "" ""]

        [list-view {:items (-> state :teams vals)

                    :renderRow (partial list-view-item controller/show-team)}]]
       [view {:style {:margin-top 20
                      :width width}}
        [header "Projects" "Back" "Add"]
        (let [topics (-> state :teams (get (-> id str keyword)) :topics vals)]

             [list-view {:items topics
                         :renderRow (partial list-view-item controller/show-project)}])]
       [view {:style {:margin-top 20
                      :width width}}
        [header "Details" "Back"]
        [text "project details"]]]))
