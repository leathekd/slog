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
  [level namespace message & [throwable]]
  {:context (get-context)
   :level level
   :message message
   :exception (when throwable
                (parse-exception throwable))
   :namespace (.getName namespace)
   :hostname (utils/hostname)
   :ip-address (utils/ip-address)
   :thread-name (.getName (Thread/currentThread))
   :timestamp (Date.)})

(defprotocol Sloggable
  (log [context log-map])
  (success [context])
  (failure [context]))

(defn log*
  "Logs the message to all configured loggers"
  [level namespace throwable message]
  (let [msg (log-map level namespace message throwable)
        loggers (config :slog :loggers)
        loggers (if (coll? loggers)
                  loggers
                  (vector loggers))]
    (doseq [logger (set loggers)]
      (log (.newInstance (ns-resolve (symbol (name logger)) 'Logger))
           msg))))

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

(defmacro make-logger [name]
  `(do
     (defmacro ~(symbol name)
       ~(str name " level logging using print-style args.")
       {:arglists '([message & more] [throwable message & more])}
       [~'& args#]
       `(logp :trace ~*ns* ~@args#))
     (defmacro ~(symbol (str name "f"))
       ~(str name " level logging using format.")
       {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
       [~'& args#]
       `(logf :trace ~*ns* ~@args#))))

(defmacro make-loggers [& loggers]
  `(do ~(map (fn [l] `(make-logger ~l)) loggers)))

;; level-specific macros
(make-loggers trace debug info warn error fatal)
