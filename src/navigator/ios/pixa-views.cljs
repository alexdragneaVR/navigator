(ns vr.pixa.pixa-views
  (:require [reagent.core :as r]
            [vr.pixa.pixa-controller :as controller]
            [vr.pixa.components :refer [ReactNative text view button rn-list-view scroll-view animated-view animated-event touchable-highlight DataSource icon-back image touchable-opacity
                                        animated-value animated-timing ease ease-out camera-roll text-input camera-icon keyboard-avoid-view scroll-view]]))

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
        height (.-height (Dimensions.get "window"))
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
        [scroll-view {:style {:height (- height 100)}}
         (when-let [messages (-> (get-in state (get-in state [:selected :path]))
                               :messages
                               vals)]
          (for [m messages]
           ^{:key (:id m)}
           [view {:style {:margin-left 5 :margin-right 5}}
            [text {:style {:font-weight "bold" :text-align (if (= (:store m) {}) "left" "right")}}
             [text (first (first (:user m)))]]
            [text {:style {:padding 5 :text-align (if (= (:store m) {}) "left" "right")}} (:message m)]]))
         (when-let [path (:new-picture-data-url state)]

           [view {:style {:height 500 :width "auto"}}
             [image {:source {:uri path}
                     :style {:width "auto" :height 480 :margin 10}
                     :resize-mode "stretch"}]])]
        [keyboard-avoid-view {:style {:margin-top 5 :height 100 :flex 1 :flex-direction "row"} :behavior "padding"}
        ;  [button {:style {:flex 1 :height 30 :border-color "grey" :border-width 0.5} :title "Camera" :on-press #(controller/take-photo!)}]
         [touchable-highlight {:on-press #(controller/take-photo!)}
          [image {:style {:margin-left 10} :source camera-icon}]]
         [text-input {:style {:flex 3 :height 30 :border-width 1 :border-color "grey" :margin-left 10}}]
         [button {:style {:flex 1 :height 30} :title "Send" :on-press #(controller/send-picture)}]]]]))
