(ns ops
  (:require [nextjournal.clerk :as clerk]
            [nrepl.server]))

(defonce !nrepl (atom nil))

(defn boot [opts]
  (reset! !nrepl (nrepl.server/start-server :bind "0.0.0.0" :port 6666))

  (clerk/show! "notebooks/datalevin_playground.clj")
  (clerk/serve! opts))

(defn stop []
  (nrepl.server/stop-server @!nrepl)
  (clerk/halt!))
