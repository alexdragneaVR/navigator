(ns vr.pixa.pixa-controller
  (:require [reagent.core :as r]
            [vr.pixa.pixa-model :refer [model keywordize]]
            [vr.pixa.rest :as REST :refer [POST<]]
            [vr.pixa.local-storage :as ls]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn offline? [state]
  (boolean (get-in state [:context :offline])))

(defn pt [& messages]
  (apply println messages)
  true)

(defn set-offline-status [state status]
  {:pre [(pt "pre set-offline-status")]
   :post [(pt "post set-offline-status")]}
  (assoc-in state [:context :offline] status))

(defn current-page [state]
  (peek (get-in state [:context :status])))

(defn go-to [state path]
  (update-in state [:context :status] conj path))

(defn go-back [state]
  (update-in state [:context :status] pop))

(defn swapm! [value atom]
  (swap! atom (constantly value))
  (go
      (let [[ok _] (<! (ls/save! :pixa/model value))]
        (if ok
          (println "Model saved to local storage")
          (println "Error in saveing model to local storage")))))

(defn show-team [team-id]
  (print "show-team" team-id)
  (-> @model
      (go-to [:team team-id])
      (swapm! model)))

(do @model)

(defn load-topic [topic-id]
  (REST/POST< "/api/1/query/Topic"
              {:find [:id
                      :name
                      :questionnaire_id
                      {:questionnaire [:id :name :percentage]}
                      {:messages [:id
                                  :from_store_id
                                  :from_user_id
                                  :conversation_id
                                  :message
                                  :last_modified_date
                                  {:user [:name :id]}
                                  {:store [:name :id]}]}] :where {:id topic-id}
               :pages {:limit 1  :offset 0}}))

(defn maybe-load-topic [state current-path topic-id]
  (if-not (offline? state)
    ()))

(defn first-val [x]
  (first (vals x)))

(defn show-project [project-id]
  (print "show-project" project-id)
  (let [[_ team-id] (current-page @model)
        current-path [:teams (keywordize team-id) :topics (keywordize project-id)]
        topic (get-in @model current-path)]
    (println current-path)
    (go
      (try
        (-> @model
            (assoc-in [:selected :project-id] project-id)
            (go-to [:topic project-id])
            (assoc-in current-path (first-val (<! (load-topic project-id))))
            (swapm! model))
        (catch :default e (println e))))))

(defn back []
  (print "back")
  (-> @model
      (go-back)
      (swapm! model)))

(defn load-local-model [state [ok? local-value]]
  (if-not (or ok? (nil? local-value))
    (do
      (println "Error in loading model")
      state)
    local-value))


(defn load-teams []
  (POST<
     "/api/1/query/Team"
     {:find
      [:id
       :name
       {:topics
        [:id
         :name
         :questionnaire_id
         {:questionnaire [:id :name :percentage]}
         :last_modified_date]}]
      :where {}
      :pages {:limit 20, :offset 0}}))

(defn pp [state & messages]
  (apply println messages)
  state)


(defn init []
  (println "init")
  (go-loop []
      (-> @model
          (set-offline-status (= :connected (<! (ls/net-connected))))
          ((fn [state] (swap! model (constantly state))))
          (offline?)
          (println "Status"))
      (recur))
  (go
    (-> @model
      (pp "Loaded local shit")
      (load-local-model (<! (ls/load! :pixa/model)))
      ((fn [state] (println state) (swap! model (constantly state)))))

    (let [connected-to-network? true]
      (if-not connected-to-network?
        (-> @model
            (set-offline-status true)
            (pp "Offline")
            (swapm! model))

        (-> @model
            (set-offline-status false)
            (assoc :teams (<! (load-teams)))
            (pp "Online")
            (swapm! model))))))
