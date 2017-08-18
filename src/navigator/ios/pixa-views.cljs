(ns vr.pixa.pixa-views
  (:require [reagent.core :as r]
            [vr.pixa.pixa-controller :as controller]
            [vr.pixa.components :refer [ReactNative text view button rn-list-view scroll-view animated-view animated-event touchable-highlight DataSource icon-back image touchable-opacity
                                        animated-value animated-timing ease ease-out camera-roll ImagePicker]]))

; styles

(def Dimensions (.-Dimensions ReactNative))

(defn data-source [props]
  (new DataSource (clj->js props)))

(defn clone-ds-with-rows [ds rowData]
  (.cloneWithRows ds (clj->js rowData)))

(defn list-view-item [func item]
  [touchable-highlight
   {:on-press #(func (:id item))
    :underlay-color "#e3e3e3"
    :style {:padding-left 10}}
   [view {:style {:height 30
                  :border-bottom-width 0.3
                  :border-bottom-color "#bbb"
                  :margin-top 10
                  :flex-direction "row"}}
    [text {:style {:margin-left 2.5 :flex 10}}
          (:name item)]
    [text {:style {:color "lightgrey"
                   :font-size 24
                   :margin-top -7
                   :flex 1}} ">"]]])

(defn list-view [props]
  (let [data-source (data-source {:rowHasChanged not=})]
    (fn [props]
      [rn-list-view {:style (:style props)
                     :dataSource (clone-ds-with-rows data-source (or (:items props) []))
                     :renderRow #(r/as-element [(:renderRow props) (js->clj % :keywordize-keys true)])
                     :enableEmptySections true}])))

(defn header [title left-button right-button]
  [view {:style {:flex-direction "row" :height 40 :border-bottom-width 0.3 :border-bottom-color "#bbb" :padding-bottom 3}}
   [view {:style {:flex 1 :align-items "flex-start"}}
    (if left-button
      [touchable-opacity {:style {:flex-direction "row"} :on-press controller/back}
       [view {:style {:flex-direction "row"}}
        [image {:style {:margin 9 :margin-right 6} :source icon-back}]
        [text {:style {:color "#007aff" :text-align "center" :padding-top 8 :font-size 18}} left-button]]])]

   [view {:style {:flex 1}}
    [text {:style {:font-size 18 :font-weight "500" :text-align "center" :margin-top 7}}
      title]]
   [view {:style {:flex 1 :align-items "flex-end"}}
    (if right-button
     [button {:color "#007aff"
              :on-press #()
              :title right-button}])]])

(defn animate-function [key f initial-value]
  (letfn [(upd [state]
            (let [[_ value] (:rum/args state)]
              (.start (animated-timing (key state)
                        #js {:toValue (f value)
                             :duration 200
                             :easing (ease-out ease)})
               state)))]
   {:init (fn [state props]
           (assoc state key
             (new animated-value (f initial-value))))
    :did-mount upd
    :will-update upd}))

(defn screen-component [state]
  (let [dataSource (data-source {:rowHasChanged not=})
        width (.-width (Dimensions.get "window"))
        image-uri ""
        [page id] (controller/current-page state)
        offset (cond
                     (= :teams page) 0
                     (= :team page) (* -1 width)
                     (= :topic page) (* -2 width)
                    :else 0)]

      [animated-view {:style {:flex-direction "row"
                              :width (* 3 width)
                              :transform [{:translateX offset}]}}

       [view {:style {:margin-top 20
                      :width width}}
        [header "Teams"]

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
        [view {:style {:background-color "lightgrey" :height 1000}}
         [button {:style {:margin 50} :title "Camera Roll" :on-press #(-> camera-roll
                                                                          (.getPhotos #js{:first 20, :assetType "All"})
                                                                          (.then (fn [r] (println r))))}]
         [button {:style {:margin-left 50 :margin-top 70} :title "Camera" :on-press #(-> ImagePicker
                                                                                         (.showImagePicker nil (fn [r] (println (.-uri r)))))}]
         [view {:style {:height 400 :width 400 :background-color "grey" :margin-top 90 :margin-left 10}}
          (if (= image-uri "") [text ""] [image {:source (js/require (if (= image-uri "") "" image-uri))
                                                 :style {:width 300 :height 300}}])]]]]))
