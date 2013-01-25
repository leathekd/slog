(ns slog.core
  (:require [carica.core :refer [config]]
            [clj-stacktrace.core :as stacktrace]
            [clojure.stacktrace :refer [print-cause-trace]]
            [clojure.string :as str]
            [slog.utils :as utils])
  (:import (java.util Date UUID)))

(def ^:dynamic *slog-id*
  "Placeholder var for the unique identifier."
  (str (UUID/randomUUID)))

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

;; I stole this from fipp and modified to suit my needs
(defmulti pretty class)

(defmethod pretty :default [x]
  (println :default (class x))
  (pr-str x))

(defmethod pretty clojure.lang.IPersistentVector [v]
  (apply str "[" (apply print-str (map pretty (seq v))) "]"))

(defmethod pretty clojure.lang.ISeq [s]
  (str "(" (apply print-str (map pretty s)) ")"))

(defn pretty-map [m]
  (let [kvps (map (fn [[k v]]
                    (str (pretty k) " " (pretty v)))
                  m)]
    (str "{" (apply print-str kvps) "}")))

(defmethod pretty clojure.lang.IPersistentMap [m]
  (pretty-map m))

(defmethod pretty clojure.lang.IRecord [r]
  (str "#" (-> r class .getName) (pretty-map r)))

(prefer-method pretty clojure.lang.IRecord clojure.lang.IPersistentMap)

(defmethod pretty clojure.lang.IPersistentSet [s]
  (str "#{" (apply print-str (map pretty s)) "}"))

(defn system-id [obj]
  (Integer/toHexString (System/identityHashCode obj)))

(defmethod pretty clojure.lang.Atom [a]
  (str "#<Atom@" (system-id a) " " (pretty @a) ">"))

(defmethod pretty java.util.concurrent.Future [f]
  (let [value (if (future-done? f)
                (pretty @f)
                ":pending")]
    (str "#<Future@" (system-id f) " " value ">")))
;; end theft


(defn log-map
  "Creates and populates the map that will be logged."
  [level namespace env message & [throwable]]
  {:context (get-context)
   :level level
   :message message
   ;; this causes an oome when reading
   :environment (into {} (for [[k v] env] [k (pretty v)]))
   :exception (when throwable
                (parse-exception throwable))
   :stacktrace (when throwable
                 (str/split (with-out-str (print-cause-trace throwable))
                            #"\n"))
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
  [level namespace env throwable message]
  (let [msg (log-map level namespace env message throwable)
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
  {:arglists '([level ns env message & more]
                 [level ns env throwable message & more])}
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

(defmacro environment
  "Expands to code that generates a map of locals: names to values"
  []
  `(zipmap '~(keys &env) [~@(keys &env)]))


(defmacro make-logger [name]
  `(do
     (defmacro ~(symbol name)
       ~(str name " level logging using print-style args.")
       {:arglists '([message & more] [throwable message & more])}
       [~'& args#]
       `(logp :trace ~*ns* (environment) ~@args#))
     (defmacro ~(symbol (str name "f"))
       ~(str name " level logging using format.")
       {:arglists '([fmt & fmt-args] [throwable fmt & fmt-args])}
       [~'& args#]
       `(logf :trace ~*ns* (environment) ~@args#))))

(defmacro make-loggers [& loggers]
  `(do ~(map (fn [l] `(make-logger ~l)) loggers)))

;; level-specific macros
(make-loggers trace debug info warn error fatal)
