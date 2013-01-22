(ns slog.test.log
  (:require [carica.core :refer [config override-config]]
            [clojure.test :refer :all]
            [slog.core :refer :all]
            [slog.log :refer :all])
  (:import (org.apache.log4j Logger)))

(use-fixtures :once
  (fn [f]
    (with-redefs [config (override-config :slog :loggers :log)]
      (f))))

(defmacro with-log-output [& body]
  `(let [log# (atom [])
         appender# (proxy [org.apache.log4j.AppenderSkeleton] []
                     (append [log-event#]
                       (swap! log# conj (.getMessage log-event#))))]
     (try
       (.addAppender (Logger/getRootLogger) appender#)
       ~@body
       (deref log#)
       (finally
         (.removeAppender (Logger/getRootLogger) appender#)))))

(deftest t-log
  (testing "one level in some depth"
    (let [plain (with-log-output
                  (trace "this" "should" "be" "one message")
                  (tracef "this %s be %s" "should" "one message"))
          exceptions (with-log-output
                       (trace (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (tracef (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message"))]
      (is (seq plain))
      (is (seq exceptions))
      (let [p (read-string (first plain))]
        (is (= "this should be one message" (:message p)))
        (is (= :trace (:level p))))
      (let [e (:exception (read-string (first exceptions)))]
        (is (= "java.lang.Exception" (:class e)))
        (is (= "ignorable test exception occurred" (:message e)))
        (is (seq (:trace-elems e))))
      (is (every? = (map #(dissoc (read-string %) :timestamp :exception)
                         (concat plain exceptions))))))
  (testing "all levels, superficially"
    (let [plain (with-log-output
                  (trace "this" "should" "be" "one message")
                  (tracef "this %s be %s" "should" "one message")
                  (debug "this" "should" "be" "one message")
                  (debugf "this %s be %s" "should" "one message")
                  (info "this" "should" "be" "one message")
                  (infof "this %s be %s" "should" "one message")
                  (warn "this" "should" "be" "one message")
                  (warnf "this %s be %s" "should" "one message")
                  (error "this" "should" "be" "one message")
                  (errorf "this %s be %s" "should" "one message")
                  (fatal "this" "should" "be" "one message")
                  (fatalf "this %s be %s" "should" "one message"))
          exceptions (with-log-output
                       (trace (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (tracef (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message")
                       (debug (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (debugf (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message")
                       (info (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (infof (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message")
                       (warn (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (warnf (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message")
                       (error (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (errorf (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message")
                       (fatal (Exception. "ignorable test exception occurred")
                              "this" "should" "be" "one message")
                       (fatalf (Exception. "ignorable test exception occurred")
                               "this %s be %s" "should" "one message"))]
      (is (seq plain))
      (is (seq exceptions))
      (is (every? = (map #(dissoc (read-string %) :timestamp :exception)
                         (concat plain exceptions)))))))
