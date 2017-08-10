(ns vr.pixa.pixa-controller
  (:require [reagent.core :as r]
            [vr.pixa.pixa-model :refer [model keywordize]]
            [vr.pixa.rest :as REST :refer [POST<]]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn swapm! [x y]
  (swap! y (fn [xx] x)))


(defn show-team [team-id]
  (print "show-team" team-id)
  (-> @model
    (assoc-in [:selected :team-id] team-id)
    (update-in [:context :status] conj "projects")
    (swapm! model)))



(defn load-topic [topic-id]
  (REST/POST< "/api/1/query/Topic"
                                 {
                                   :find [
                                           :id
                                           :name
                                           :questionnaire_id
                                           {:questionnaire [:id :name :percentage]}
                                           {:messages [
                                                        :id
                                                        :from_store_id
                                                        :from_user_id
                                                        :conversation_id
                                                        :message
                                                        :last_modified_date
                                                        {:user [:name :id]}
                                                        {:store [:name :id]}]}]


                                   :where {:id topic-id}
                                   :pages {:limit 1  :offset 0}}))





(defn show-project [project-id]
  (print "show-project" project-id)
  (let [current-path [:teams (keywordize (-> @model :selected :team-id)) :topics (keywordize project-id)]
        topic (get-in @model current-path)]
    (go
      (-> @model
        (assoc-in [:selected :project-id] project-id)
        (update-in [:context :status] conj "project")
        (assoc-in [:selected :current-path] current-path)
        (assoc-in current-path (-> (<! (load-topic project-id))  vals first))


        (swapm! model)))))



(defn back []
  (print "back")
  (-> @model
    (assoc-in [:selected :project-id] (get-in @model [:selected :old-project-id]))
    (assoc-in [:selected :team-id] (get-in @model [:selected :old-team-id]))
    (assoc-in [:context :status] (or (get-in @model [:context :old-status] "list")))
    (swapm! model)))



(defn init []
  (print "init")
  (go
    (-> @model
      (assoc :teams (<! (POST<
                          "/api/1/query/Team"
                          {:find
                           [:id
                            :name
                            {:topics
                             [:id
                              :name
                              :questionnaire_id
                              {:questionnaire [:id :name :percentage]}
                              :last_modified_date]}],
                           :where {},
                           :pages {:limit 20, :offset 0}})))
      ((fn set-first-team [state]
          (assoc-in state [:selected :team-id] (-> state :teams vals first :id))))

      (swapm! model))))
