;; # 🚸 A todo list persisted in datalevin
(ns todo
  {:nextjournal.clerk/no-cache true}
  (:require [babashka.fs :as fs]
            [datalevin.core :as d]
            [nextjournal.clerk :as clerk]))

{::clerk/visibility {:code :hide :result :hide}}

(def !tasks (atom nil))

(def task-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [{:as m :task/keys [description completed? id]} _]
                 (println "task" (str id) completed?)
                 [:div.mb-1.flex.bg-amber-200.border.border-amber-400.rounded-md.p-2
                  [:div [:input.mt-2.ml-3 {:type :checkbox :checked (boolean completed?)
                                 :class (str "appearance-none h-4 w-4 rounded bg-amber-300 border border-amber-400 relative"
                                             "checked:border-amber-600 checked:bg-amber-600 checked:bg-no-repeat checked:bg-contain")
                                 :on-change (fn [e]
                                              (.then (nextjournal.clerk.render/clerk-eval
                                                      {:recompute? true}
                                                      (list 'update-task (str id) 'assoc :task/completed? (.. e -target -checked)))))}]]
                  [:div.text-xl.ml-2.mb-0.font-sans description]])})

(def tasks-viewer
  {:transform-fn (clerk/update-val (comp (partial mapv (partial clerk/with-viewer task-viewer)) deref))
   :render-fn '(fn [coll opts] (into [:div] (nextjournal.clerk.render/inspect-children opts) coll))})

{::clerk/visibility {:code :hide :result :show}}

(clerk/with-viewer
  '(fn [_ _]
     (let [text (nextjournal.clerk.render.hooks/use-state nil)
          ref (nextjournal.clerk.render.hooks/use-ref nil)
          handle-key-press (nextjournal.clerk.render.hooks/use-callback
                            (fn [e]
                              (when (and (= "Enter" (.-key e)) (= (.-target e) @ref) (not-empty @text))
                                (reset! text nil)
                                (nextjournal.clerk.render/clerk-eval {:recompute? true} (list 'add-task @text)))) [text])]

      (nextjournal.clerk.render.hooks/use-effect
       (fn []
         (.addEventListener js/window "keydown" handle-key-press)
         #(.removeEventListener js/window "keydown" handle-key-press)) [handle-key-press])

      [:div.p-1.flex.bg-amber-100.border-amber-200.border.rounded-md.h-10.w-full.pl-8.font-sans.text-xl
       [:input.bg-amber-100.focus:outline-none.text-md.w-full
        {:on-change #(reset! text (.. % -target -value))
         :placeholder "Enter text and press Enter…" :ref ref
         :value @text :type "text"}]])) nil)

(clerk/with-viewer tasks-viewer !tasks)

{::clerk/visibility {:code :show :result :hide}}

;; …and here some moving parts (viewers hidden)
(def schema
  {:task/description {:db/valueType :db.type/string}
   :task/id {:db/valueType :db.type/uuid
             :db/unique :db.unique/identity}
   :task/completed? {:db/valueType :db.type/boolean}
   :task/category {:db/valueType :db.type/keyword}})

(def conn (d/create-conn "/tmp/garden/storage/todo" schema {:auto-entity-time? true}))

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

(defn remove-task [id]
  (d/transact conn [[:db/retractEntity [:task/id (parse-uuid id)]]]))

(reset! !tasks (tasks))

#_(comment
    (def id *1)
    d/get-conn
    (fs/delete-tree "/tmp/garden/storage/todo")
    (fs/list-dir "/tmp/garden/storage/todo")
    (->> (d/q '[:find [?t ...] :where [?t :task/id]]
              (d/db conn))
         (map #(d/entity (d/db conn) %))
         (sort-by :db/created-at >)
         (map d/touch)
         (first) :task/id str))
