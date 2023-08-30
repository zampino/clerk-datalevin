(ns ops
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as viewer]
            [nextjournal.clerk.webserver :as webserver]
            [nrepl.server]))

(defonce !nrepl (atom nil))

(defn boot [opts]
  (reset! !nrepl (nrepl.server/start-server :bind "0.0.0.0" :port 6666))

  ;; cannot create concurrent connections, e.g. caused by old container during re-deploy of a new version
  ;;
  ;; Execution error (ExceptionInfo) at nextjournal.clerk.eval/eval+cache! (eval.clj:155).
  ;; Execution error (LmdbNativeException$ConstantDerivedException) at org.lmdbjava.ResultCodeMapper/checkRc (ResultCodeMapper.java:114).
  ;; Platform constant error code: EAGAIN Resource temporarily unavailable (11)
  ;;
  ;; see Caveats section at http://www.lmdb.tech/doc/

  #_
  (clerk/show! "notebooks/todo.clj")

  ;; hack to get have / go to notebook
  (reset! webserver/!doc {:nav-path "'nextjournal.clerk.index"})

  ;; disable header ?
  #_
  (clerk/reset-viewers! :default
                        (clerk/add-viewers [(assoc viewer/header-viewer
                                                   :render-fn '(fn [_ _] [:div.mt-10]))]))
  ;; boot clerk
  (clerk/serve! opts))

(defn stop []
  (nrepl.server/stop-server @!nrepl)
  (clerk/halt!))
