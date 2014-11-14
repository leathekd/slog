(ns slog.core
  (:require [clojure.stacktrace :refer [print-cause-trace]]
            [clojure.tools.logging :as log]
            [slog.utils :as utils]))

(def ip-address (utils/ip-address))
(def hostname (utils/hostname))

(def ^:dynamic *slog-id*
  "Placeholder var for the unique identifier."
  nil)

(defn get-context []
  *slog-id*)

(defmacro with-slog-context [& body]
  (let [[context body] (if (string? (first body))
                         [(first body) (rest body)]
                         [(utils/uuid) body])]
    `(binding [*slog-id* ~context]
       ~@body)))

(defn exception->str [ex]
  (with-out-str (print-cause-trace ex)))

(defn log-env-map
  "Creates and populates the map that will be logged."
  [level namespace]
  (merge {:level level
          :namespace (.getName namespace)
          :hostname hostname
          :ip-address ip-address
          :thread-name (.getName (Thread/currentThread))
          :timestamp (utils/now)}
         (when-let [ctx (get-context)]
           {:slog-context ctx})))

(defn build-log-map [level ns & more]
  (let [[throwable kv-pairs] (if (instance? Throwable (first more))
                               [(first more) (rest more)]
                               [nil more])
        log-context (log-env-map level ns)
        kv-pairs (if (map? (first kv-pairs))
                   (first kv-pairs)
                   (apply hash-map kv-pairs))]
    (merge
     {:env log-context
      :msg (if (:exception kv-pairs)
             (update-in kv-pairs [:exception] exception->str)
             kv-pairs)}
     (when throwable
       {:exception (exception->str throwable)}))))

(defn logm [level ns & more]
  (log/log level (pr-str (apply build-log-map level ns more))))

(defn make-logger [name]
  (let [name-sym (symbol name)
        name-key (keyword (str name))]
    `(defmacro ~name-sym
       ~(str name " level map or kv-pair logging")
       {:arglists '([log-map] [throwable log-map]
                      [& {:as kv-pairs}] [throwable & {:as kv-pairs}])}
       [~'& args#]
       `(when (log/enabled? ~~name-key)
          (logm ~~name-key ~*ns* ~@args#)))))

(defmacro make-loggers [& loggers]
  `(do ~(map make-logger loggers)))

;; level-specific macros
(make-loggers trace debug info warn error fatal)
