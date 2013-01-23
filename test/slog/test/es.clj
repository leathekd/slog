(ns slog.test.es
  (:require [carica.core :refer [config override-config]]
            [cheshire.core :as cheshire]
            [clojure.test :refer :all]
            [clj-http.client :as http]
            [slog.core :refer :all]
            [slog.es :refer :all]))

(def index (config :slog :es :index))

(use-fixtures :each
  (fn [f]
    (with-redefs [config (override-config :slog :loggers :es)]
      (when (index-exists? index)
        (http/delete (index-url index)
                     (config :slog :es :request-options)))
      (f))))

(deftest t-log
  ;; These should all end up in ES
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
  (fatalf "this %s be %s" "should" "one message")
  (let [hits (entries index)
        n (count hits)]
    (is (= 12 n))
    (is (apply = (map #(dissoc % :timestamp :exception :level) hits)))))

(deftest t-log-exceptions
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
          "this %s be %s" "should" "one message")
  (let [hits (entries index)
        n (count hits)]
    (is (= 12 n))
    (is (apply = (map #(dissoc % :timestamp :exception :level) hits)))))

(deftest t-log-contexts
  (let [first-context (with-slog-context
                        (warn "this" "should" "be" "one message")
                        (get-context))
        second-context (with-slog-context
                         (warn "this" "should" "be" "one message")
                         (get-context))]
    (let [hits (entries index)
          n (count hits)
          grouped (group-by :context hits)]
      (is (= 2 n))
      (is (get grouped first-context))
      (is (get grouped second-context))
      (is (apply = (map #(dissoc % :timestamp :context) hits))))))
