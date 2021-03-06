(ns vr.pixa.pixa-controller
  (:require [reagent.core :as r]
            [vr.pixa.pixa-model :refer [model keywordize]]
            [vr.pixa.rest :as REST :refer [POST<]]
            [vr.pixa.local-storage :as ls]
            [cljs.core.async :as a :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def web-view-in (a/chan))
(def web-view-out (a/chan))


(defmulti process-web-view-action! (fn [[event _]] event))

(defmethod process-web-view-action! :default [[event payload]]
  (println event " " payload))

(defmethod process-web-view-action! :web-3d-view/load-material [[_ payload]]
  (let [url (:url payload)]
    (a/put! web-view-in {:event "load-material"
                         :url url
                         :data ""})))


(defmethod process-web-view-action! :web-3d-view/load-texture [[_ payload]]
  (let [url (:url payload)]
    (a/put! web-view-in {:event "load-texture"
                         :url url
                         :data ""})))


(defmethod process-web-view-action! :web-3d-view/load-obj [[_ payload]]
  (let [url (:url payload)]
    (a/put! web-view-in {:event "load-obj"
                         :url url
                         :data ""})))


(defn offline? [state]
  (boolean (get-in state [:context :offline])))

(defn pt [& messages]
  (apply println messages)
  true)

(defn set-network-status [state status]
  (assoc-in state [:context :network] status))

(defn current-page [state]
  (peek (get-in state [:context :status])))

(defn go-to [state path]
  (update-in state [:context :status] conj path))

(defn go-back [state]
  (update-in state [:context :status] pop))

(defn swapm!
  ([value atom] (swapm! value atom false))
  ([value atom save?]
   (when save?
     (go
        (let [[ok _] (<! (ls/save! :pixa/model value))]
          (if ok
            (println "Model saved to local storage")
            (println "Error in saveing model to local storage")))))
   (swap! atom (constantly value))))

(defn show-team [team-id]
  ; (print "show-team" team-id)
  (-> @model
      (go-to [:team team-id])
      (swapm! model :save)))


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
  (let [[_ team-id] (current-page @model)
        current-path [:teams (keywordize team-id) :topics (keywordize project-id)]
        topic (get-in @model current-path)
        connected-to-network? (-> @model :context :network (= :connected))]
    (go
      (try
        (-> @model
            (assoc-in [:selected :project-id] project-id)
            (assoc-in [:selected :path] current-path)
            (go-to [:topic project-id])
            (swapm! model)
            (assoc-in current-path (first-val (<! (load-topic project-id))))
            (swapm! model :save))
        (catch :default e (println e))))))

(defn back []
  ; (print "back")
  (-> @model
      (go-back)
      (swapm! model :save)))

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


(defn assoc-if-connected [state key value]
  (-> state
    (assoc :ta value)
    ((fn [state]
       (if (:ta state)
         (-> state
           (assoc key (:ta state))
           (dissoc :ta))
         (dissoc state :ta))))))

(defn take-photo! []
  (go
    (let [[ok? photo-base64] (<! (ls/take-photo!))]
      (println ok? photo-base64)
      (-> @model
        (assoc :new-picture-data-url photo-base64)
        (swapm! model)))))


(defn post-picture [picture-base-64]
  (POST<
    "/api/1/upload"
    {
      :data picture-base-64
      :name "asddef.jpg"
      :type "image/jpeg"}))

(defn send-picture []
  (println "Selected: " (:selected @model))
  (println "Context: " (:context @model))
  (println "Status: " (:status (:context @model)))
  ; (println "Messages: " ())
  (println "Teams: " (:teams @model)))

  ; (go
  ;  (println "Response: " (<! (post-picture (:new-picture-data-url @model))))))
  ; response :serving_url



(defn init []
  ;; web-view event handler
  (go-loop []
    (let [web-view-action (<! web-view-out)]
      (process-web-view-action! web-view-action))
    (recur))

  (let [connection-info (ls/net-connected)]
    (go
      (let [[_ connected-to-network?] (<! (ls/net-connected?))]
        (-> @model
          (load-local-model (<! (ls/load! :pixa/model)))
          (set-network-status (if connected-to-network? :connected :disconnected))
          (swapm! model)
          (assoc-if-connected :teams (when connected-to-network? (<! (load-teams))))
          (swapm! model :save)))

      (loop []
        (-> @model
            (set-network-status (second (<! connection-info)))
            (swapm! model))
        (recur)))))
