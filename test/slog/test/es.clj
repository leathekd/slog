(ns slog.test.es
  (:require [carica.core :refer [config override-config]]
            [clojure.test :refer :all]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [slog.core :refer :all]
            [slog.es :refer :all]))

(def index (config :slog :es :index))

(use-fixtures :each
  (fn [f]
    (with-redefs [config (override-config :slog :loggers :es)]
      (f)))
  (fn [f]
    (esr/connect! (str (config :slog :es :connection-url)))
    (when (esi/exists? index)
      (esd/delete-by-query index "log-entry" "*:*")
      (esi/delete index))
    (f)))

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
  ;; make sure the index is ready to search
  (esi/refresh index)
  (let [res (esd/search index "log-entry" :query (q/match-all))
        n (esrsp/total-hits res)
        hits (esrsp/hits-from res)]
    (is (= 12 n))
    (is (every? = (map #(dissoc % :timestamp :exception :level)
                       (map :_source hits))))))

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
  ;; make sure the index is ready to search
  (esi/refresh index)
  (let [res (esd/search index "log-entry" :query (q/match-all))
        n (esrsp/total-hits res)
        hits (esrsp/hits-from res)]
    (is (= 12 n))
    (is (every? = (map #(dissoc % :timestamp :exception :level)
                       (map :_source hits))))))

(deftest t-log-contexts
  (let [first-context (get-context)]
    (info (Exception. "ignorable test exception occurred")
          "this" "should" "be" "one message")
    (new-context)
    (warn (Exception. "ignorable test exception occurred")
          "this" "should" "be" "one message")
    ;; make sure the index is ready to search
    (esi/refresh index)
    (let [res (esd/search index "log-entry" :query (q/match-all))
          n (esrsp/total-hits res)
          hits (esrsp/hits-from res)]
      (is (= 2 n))
      (is (every? = (map #(dissoc % :timestamp :exception :level)
                         (map :_source hits)))))
    (let [res (esd/search index "log-entry" :query
                          (q/term :context first-context))
          n (esrsp/total-hits res)
          hits (esrsp/hits-from res)]
      (is (= 1 n))
      (is (every? = (map #(dissoc % :timestamp :exception :level)
                         (map :_source hits)))))
    (let [res (esd/search index "log-entry" :query
                          (q/term :context (get-context)))
          n (esrsp/total-hits res)
          hits (esrsp/hits-from res)]
      (is (= 1 n))
      (is (every? = (map #(dissoc % :timestamp :exception :level)
                         (map :_source hits)))))))
