;; # 🏪 Datalevin Playground
(ns datalevin-playground
  (:require [datalevin.core :as d]
            [cljmb.core :as cljmb]
            [clojure.data.csv :as csv]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as viewer]))

(def conn (d/get-conn "/tmp/storage"))

(def bach-id
  (-> (cljmb/search "artist" "Johann Sebastian Bach" 100 1)
      :artists
      first
      :id))

(-> (cljmb/browse "release" {"artist" bach-id} 100 1)
    :releases
    second)

(-> (cljmb/browse "recording" {"artist" bach-id} 100 1)
    :recordings
    last)
