(ns slog.core
  (:require [carica.core :refer [config]]
            [clj-stacktrace.core :refer [parse-exception]]
            [slog.utils :as utils])
  (:import (java.util Date UUID)))

(def ^:dynamic ^:private *slog-id*
  "Placeholder var for the unique identifier."
  (str (UUID/randomUUID)))

;; TODO: context-successful, context-failed, discard-context

(defn get-context []
  *slog-id*)

(defn set-context [new-context]
  (set! *slog-id* new-context))

(defn new-context
  "Sets a thread local variable to a uniqe identifier for purposes of
  grouping related operations."
  []
  (set-context (str (UUID/randomUUID))))

(defn log-map
  "Creates and populates the map that will be logged."
  [level namespace message & [throwable]]
  {:context *slog-id*
   :level level
   :message message
   :exception (when throwable
                (let [parsed-ex (parse-exception throwable)]
                  (if-let [data (ex-data throwable)]
                    (assoc parsed-ex :data data)
                    parsed-ex)))
   :namespace (.getName namespace)
   :hostname (utils/hostname)
   :ip-address (utils/ip-address)
   :thread-name (.getName (Thread/currentThread))
   :timestamp (Date.)})

;; TODO: protocol instead? I guess it would allow Java to play
;; along... Might make more sense if the interface increases (e.g.,
;; queueing messages to log, doing specific actions upon success or
;; failure of the context)
(defmulti log
  "Hook point for logging messages to a specific backend"
  (fn [logger _] logger))

(defn log*
  "Logs the message to all configured loggers"
  [level namespace throwable message]
  (let [msg (log-map level namespace message throwable)
        loggers (config :slog :loggers)
        loggers (if (coll? loggers)
                     loggers
                     (vector loggers))]
    (doseq [logger (set loggers)]
      (log logger msg))))

(defn logp
  "Logs a message using print style args. Can optionally take a throwable as its
  second arg. See level-specific macros, e.g., debug."
  {:arglists '([level ns message & more] [level ns throwable message & more])}
  [level ns x & more]
  (if (instance? Throwable x)
    (log* level ns x (apply print-str more))
    (log* level ns nil (apply print-str x more))))

(defn logf
  "Logs a message using a format string and args. Can optionally take a
  throwable as its second arg. See level-specific macros, e.g., debugf."
  {:arglists '([level ns fmt & fmt-args] [level ns throwable fmt & fmt-args])}
  [level ns x & more]
  (if (instance? Throwable x)
    (log* level ns x (apply format more))
    (log* level ns nil (apply format x more))))

;; level-specific macros

(defmacro trace
  "Trace level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :trace ~*ns* ~@args))

(defmacro debug
  "Debug level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :debug ~*ns* ~@args))

(defmacro info
  "Info level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :info ~*ns* ~@args))

(defmacro warn
  "Warn level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :warn ~*ns* ~@args))

(defmacro error
  "Error level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :error ~*ns* ~@args))

(defmacro fatal
  "Fatal level logging using print-style args."
  {:arglists '([message & more] [throwable message & more])}
  [& args]
  `(logp :fatal ~*ns* ~@args))

(defmacro tracef
  "Trace level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :trace ~*ns* ~@args))

(defmacro debugf
  "Debug level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :debug ~*ns* ~@args))

(defmacro infof
  "Info level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :info ~*ns* ~@args))

(defmacro warnf
  "Warn level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :warn ~*ns* ~@args))

(defmacro errorf
  "Error level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :error ~*ns* ~@args))

(defmacro fatalf
  "Fatal level logging using format."
  {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
  [& args]
  `(logf :fatal ~*ns* ~@args))
