(ns slog.core
  (:require [carica.core :refer [config]]
            [clj-stacktrace.core :as stacktrace]
            [slog.utils :as utils])
  (:import (java.util Date UUID)))

(def ^:dynamic *slog-id*
  "Placeholder var for the unique identifier."
  (str (UUID/randomUUID)))

;; TODO: context-successful, context-failed, discard-context

(defn get-context []
  *slog-id*)

(defmacro with-slog-context [& body]
  (let [[context body] (if (string? (first body))
                         [(first body) (rest body)]
                         [(str (UUID/randomUUID)) body])]
    `(binding [*slog-id* ~context]
       ~@body)))

(defn parse-exception [throwable]
  (let [parsed-ex (stacktrace/parse-exception throwable)
        parsed-ex (if-let [data (ex-data throwable)]
                    (assoc parsed-ex :data data)
                    parsed-ex)]
    (update-in parsed-ex [:class] #(.getName %))))

(defn log-map
  "Creates and populates the map that will be logged."
  [level namespace env message & [throwable]]
  {:context (get-context)
   :level level
   :env env
   :message message
   :exception (when throwable
                (parse-exception throwable))
   :namespace (.getName namespace)
   :hostname (utils/hostname)
   :ip-address (utils/ip-address)
   :thread-name (.getName (Thread/currentThread))
   :timestamp (Date.)})

;; TODO
(defmacro environment
  "Expands to code that generates a map of locals: names to values"
  []
  `(zipmap '~(keys &env) [~@(keys &env)]))

;; TODO: protocol instead? I guess it would allow Java to play
;; along... Might make more sense if the interface increases (e.g.,
;; queueing messages to log, doing specific actions upon success or
;; failure of the context)
(defmulti log
  "Hook point for logging messages to a specific backend"
  (fn [logger _] logger))

(defn log*
  "Logs the message to all configured loggers"
  [level namespace env throwable message]
  (let [msg (log-map level namespace env message throwable)
        loggers (config :slog :loggers)
        loggers (if (coll? loggers)
                  loggers
                  (vector loggers))]
    (doseq [logger (set loggers)]
      (log logger msg))))

(defn logp
  "Logs a message using print style args. Can optionally take a throwable as its
  second arg. See level-specific macros, e.g., debug."
  {:arglists '([level ns env message & more]
                 [level ns env hrowable message & more])}
  [level ns env x & more]
  (if (instance? Throwable x)
    (log* level ns env x (apply print-str more))
    (log* level ns env nil (apply print-str x more))))

(defn logf
  "Logs a message using a format string and args. Can optionally take a
  throwable as its second arg. See level-specific macros, e.g., debugf."
  {:arglists '([level ns env fmt & fmt-args]
                 [level ns env throwable fmt & fmt-args])}
  [level ns env x & more]
  (if (instance? Throwable x)
    (log* level ns env x (apply format more))
    (log* level ns env nil (apply format x more))))

;; level-specific macros

(defmacro trace
  "Trace level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :trace ~*ns* (environment) ~@args))

(defmacro debug
  "Debug level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :debug ~*ns* (environment) ~@args))

(defmacro info
  "Info level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :info ~*ns* (environment) ~@args))

(defmacro warn
  "Warn level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :warn ~*ns* (environment) ~@args))

(defmacro error
  "Error level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :error ~*ns* (environment) ~@args))

(defmacro fatal
  "Fatal level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :fatal ~*ns* (environment) ~@args))

(defmacro tracef
  "Trace level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :trace ~*ns* (environment) ~@args))

(defmacro debugf
  "Debug level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :debug ~*ns* (environment) ~@args))

(defmacro infof
  "Info level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :info ~*ns* (environment) ~@args))

(defmacro warnf
  "Warn level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :warn ~*ns* (environment) ~@args))

(defmacro errorf
  "Error level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :error ~*ns* (environment) ~@args))

(defmacro fatalf
  "Fatal level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :fatal ~*ns* (environment) ~@args))
