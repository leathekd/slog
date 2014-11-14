(ns slog.test.core
  (:require [clojure.test :refer :all]
            [slog.core :refer :all])
  (:import (java.util UUID)
           (org.apache.log4j Logger)))

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
                  (trace :msg "the message"
                         :some :other
                         :stuff :here
                         :more "stuff"
                         :five 5)
                  (trace {:msg "the message"
                          :some :other
                          :stuff :here
                          :more "stuff"
                          :five 5}))
          exceptions (with-log-output
                       (trace (Exception. "ignorable test exception")
                              :msg "the message"
                              :some :other
                              :stuff :here
                              :more "stuff"
                              :five 5)
                       (trace (Exception. "ignorable test exception")
                              {:msg "the message"
                               :some :other
                               :stuff :here
                               :more "stuff"
                               :five 5})
                       (trace :msg "the message" :some :other :stuff :here
                              :exception (Exception. "ignorable test exception")
                              :more "stuff"
                              :five 5)
                       (trace {:msg "the message" :some :other :stuff :here
                               :exception (Exception. "ignorable test exception")
                               :more "stuff"
                               :five 5}))]
      (is (seq plain))
      (is (= 2 (count plain)))
      (is (seq exceptions))
      (is (= 4 (count exceptions)))
      (testing "plain logging works with kvs or maps"
        (doseq [p plain
                :let [p (read-string p)]]
          (is (map? p) (pr-str p))
          (is (= "the message" (get-in p [:msg :msg])) (pr-str p))
          (is (= 5 (get-in p [:msg :five])) (pr-str p))
          (is (= :trace (get-in p [:env :level])) (pr-str p))))
      (testing "exception logging works with exceptions "
        (testing "in the leading position"
          (doseq [e (take 2 exceptions)
                  :let [e (read-string e)]]
            (is (map? e) (pr-str e))
            (is (= "the message" (get-in e [:msg :msg])) (pr-str e))
            (is (= 5 (get-in e [:msg :five])) (pr-str e))
            (is (= :trace (get-in e [:env :level])) (pr-str e))
            (is (re-find #"ignorable test exception" (:exception e))
                (pr-str e))))
        (testing "inside a log map"
          (doseq [e (drop 2 exceptions)
                  :let [e (read-string e)]]
            (is (map? e) (pr-str e))
            (is (= "the message" (get-in e [:msg :msg])) (pr-str e))
            (is (= 5 (get-in e [:msg :five])) (pr-str e))
            (is (= :trace (get-in e [:env :level])) (pr-str e))
            (is (re-find #"ignorable test exception"
                         (get-in e [:msg :exception])) (pr-str e)))))))
  (testing "all levels, superficially"
    (let [plain (with-log-output
                  (trace :msg "hello world")
                  (debug :msg "hello world")
                  (info :msg "hello world")
                  (warn :msg "hello world")
                  (error :msg "hello world")
                  (fatal :msg "hello world"))
          exceptions (with-log-output
                       (trace (Exception. "ignorable test exception occurred")
                              :msg "hello world")
                       (debug (Exception. "ignorable test exception occurred")
                              :msg "hello world")
                       (info (Exception. "ignorable test exception occurred")
                             :msg "hello world")
                       (warn (Exception. "ignorable test exception occurred")
                             :msg "hello world")
                       (error (Exception. "ignorable test exception occurred")
                              :msg "hello world")
                       (fatal (Exception. "ignorable test exception occurred")
                              :msg "hello world"))]
      (is (seq plain))
      (is (seq exceptions))
      (is (every? = (map #(get-in (read-string %) [:msg :msg])
                         (concat plain exceptions)))))))
