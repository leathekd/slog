(ns slog.es
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [slog.core :refer [log Sloggable]]
            [carica.core :refer [config]]))

(def mapping
  {"log_entry"
   {:properties {:context {:type "string"
                           :index "not_analyzed"}
                 :level {:type "string"}
                 :message {:type "string"}
                 :namespace {:type "multi_field"
                             :fields {:namespace {:type "string"}
                                      :namespace-v
                                      {:type "string"
                                       :index "not_analyzed"}}}
                 ;; will dynamic take care of the rest?
                 :exception {:type "object"}
                 :stacktrace {:type "multi_field"
                              :fields {:stacktrace {:type "string"}
                                       :stacktrace-verbatim
                                       {:type "string"
                                        :index "not_analyzed"}}}
                 :timestamp {:type "date"
                             :format "yyyy-MM-dd'T'HH:mm:ss'Z'"}
                 :hostname {:type "string"}
                 :ip-address {:type "ip"}
                 :thread-name {:type "string"}}}})

(defn index-url [index]
  (str (config :slog :es :connection-url) "/" index))

(defn index-exists? [index]
  (try
    (http/head (str (config :slog :es :connection-url) "/" index)
               (config :slog :es :request-options))
    true
    (catch Exception e
      (if-let [data (ex-data e)]
        (when-not (= 404 (:status data))
          (log/error e "There was a problem checking on the slog index"))
        (log/error e "There was a problem checking on the slog index")))))

(defn ensure-index [index]
  (if (index-exists? index)
    index
    (try
      (http/post (index-url index)
                 (merge {:body (cheshire/encode {:mappings mapping})}
                        (config :slog :es :request-options)))
      index
      (catch Exception e
        (log/error e "Error creating the slog ES index.")))))

(defn refresh-index [index]
  (http/post (str (index-url index) "/_refresh")
             (config :slog :es :request-options)))

(defn entries [index & [size]]
  ;; make sure the index is ready to search
  (refresh-index index)
  (try
    (let [resp
          (cheshire/parse-string
           (:body
            (http/get (str (index-url (ensure-index index))
                           "/log_entry/_search?sort=timestamp&q=*:*&size="
                           (or size 50))
                      (config :slog :es :request-options)))
           true)]
      (map :_source (get-in resp [:hits :hits])))
    (catch Exception e
      (log/error e "An error occurred why searching for slog entries."))))

(defrecord Logger []
  Sloggable
  (log [_ log-map]
    (let [index (config :slog :es :index)
          index-url (index-url index)]
      (when-let [index (ensure-index index)]
        (try
          (http/post (str index-url "/log_entry")
                     (merge {:body (cheshire/encode log-map)}
                            (config :slog :es :request-options)))
          (catch Exception e
            ;; TODO what to do with log-map? attempt slog.log it?
            (log/error e
                       "An error occurred while indexing the slog entry.")))))))
