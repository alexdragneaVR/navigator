(ns vr.pixa.pixa-model
  (:require [reagent.core :as r]))

(def keywordize (comp keyword str))


(def model
  (r/atom
    {
      :context {
                :status '([:teams])
                :teams_filters_open? false
                :projects_filters_open? false
                :teams_open? true
                :projects_open? true}

     :selected {
                :team-id nil
                :project-id nil}

     :user {}


     :teams nil}))


(defn current-path [state]
   [:teams (keywordize (-> state :selected :team-id)) :topics (keywordize  (-> state :selected :topic-id))])


(defn current-topic [state]
    (get-in state (current-path state)))


(defn jsmodel []
  (clj->js @model))
