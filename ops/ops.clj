(ns ops
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.webserver :as webserver]
            [nrepl.server]))

(defonce !nrepl (atom nil))

(defn boot [opts]
  (reset! !nrepl (nrepl.server/start-server :bind "0.0.0.0" :port 6666))
  ;; cannot create a concurrent connectionsk, this is caused by old container during re-deploy of a new version
  ;;
  ;; Execution error (ExceptionInfo) at nextjournal.clerk.eval/eval+cache! (eval.clj:155).
  ;; Execution error (LmdbNativeException$ConstantDerivedException) at org.lmdbjava.ResultCodeMapper/checkRc (ResultCodeMapper.java:114).
  ;; Platform constant error code: EAGAIN Resource temporarily unavailable (11)
  ;;
  ;; see http://www.lmdb.tech/doc/

  #_
  (clerk/show! "notebooks/todo.clj")

  ;; hack to get have / go to notebook
  (reset! webserver/!doc {:nav-path "/notebooks/todo"})
  (clerk/serve! opts))

(defn stop []
  (nrepl.server/stop-server @!nrepl)
  (clerk/halt!))
