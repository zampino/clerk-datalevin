;; # 🚸A datalevin todo list
(ns roadmap
  {:nextjournal.clerk/no-cache true}
  (:require [babashka.fs :as fs]
            [datalevin.core :as d]
            [nextjournal.clerk :as clerk]))

{::clerk/visibility {:code :hide :result :hide}}

(def schema
  {:task/description {:db/valueType :db.type/string}
   :task/id {:db/valueType :db.type/uuid
             :db/unique :db.unique/identity}
   :task/completed? {:db/valueType :db.type/boolean}
   :task/category {:db/valueType :db.type/keyword}})

(def conn (d/create-conn "/tmp/storage" schema {:auto-entity-time? true}))

(defn ->map [m] (into {} (remove (comp #{"db"} namespace key)) m))
(defn tasks []
  (->> (d/q '[:find [?t ...] :where [?t :task/id]]
            (d/db conn))
       (map #(d/entity (d/db conn) %))
       (sort-by :db/created-at >)
       (map ->map)))

(defn add-task [text]
  (d/transact conn [{:task/id (random-uuid)
                     :task/description text
                     :task/category :task.category/CLI}]))

(defn update-task [id f & args]
  (let [ref [:task/id (parse-uuid id)]
        updated-entity (apply f (->map (d/entity (d/db conn) ref)) args)
        {:keys [db-after]} (d/transact! conn [updated-entity])]
    (->map (d/entity db-after ref))))

(def task-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [{:as m :task/keys [description completed? id]} _]
                 (println "task" (str id) completed?)
                 [:div.mb-1.flex.bg-amber-200.border.border-amber-400.rounded-md
                  [:div.mt-1.mx-2 [:input {:type :checkbox :checked (boolean completed?)
                                           :on-change (fn [e]
                                                        (.then (nextjournal.clerk.render/clerk-eval
                                                                {:recompute? true}
                                                                (list 'update-task (str id) 'assoc :task/completed? (.. e -target -checked)))))}]]
                  [:div.text-xl.ml-2.mb-0.font-sans description]])})

(def tasks-viewer {:render-fn '(fn [coll opts] (into [:div] (nextjournal.clerk.render/inspect-children opts) coll))})

{::clerk/visibility {:code :hide :result :show}}

(clerk/with-viewer
  '(fn [_ _]
     (let [text (nextjournal.clerk.render.hooks/use-state nil)]
       [:div.p-1.flex.bg-amber-100.border-amber-200.border.rounded-md.h-10.w-full.pl-8.font-sans.text-xl
        [:button.text-xs.px-2.rounded-md.bg-amber-300.mr-2 {:on-click (fn [^js e]
                                                                        (when (not-empty @text)
                                                                          (reset! text nil)
                                                                          (nextjournal.clerk.render/clerk-eval
                                                                           {:recompute? true}
                                                                           (list 'add-task @text))))} "Add"]
        [:input.bg-amber-100.focus:outline-none {:on-change #(reset! text (.. % -target -value))
                                                 :type "text" :placeholder "Enter a task…"}]])) nil)

(clerk/with-viewer tasks-viewer
  (mapv (partial clerk/with-viewer task-viewer) (tasks)))

#_
(comment
  d/get-conn
  (fs/delete-tree "/tmp/storage")
  (fs/list-dir "/tmp/storage")
  (->> (d/q '[:find [?t ...] :where [?t :task/id]]
            (d/db conn))
       (map #(d/entity (d/db conn) %))
       (sort-by :db/created-at >)
       (map d/touch)
       first
       ))
