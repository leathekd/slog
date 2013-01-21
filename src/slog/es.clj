(ns slog.es
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [slog.core :refer [log]]
            [carica.core :refer [config]]))

(def mapping
  {:entry
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
                 :timestamp {:type "date"
                             :format "yyyy-MM-dd'T'HH:mm:ss'Z'"}
                 :hostname {:type "string"}
                 :ip-address {:type "ip"}
                 :thread-name {:type "string"}}}})

(defn ensure-index []
  (let [index (config :slog :es :index)]
    (if (esi/exists? index)
      index
      (let [resp (esi/create index :mappings mapping)]
        (if (esrsp/ok? resp)
          index
          (log/error "An error occurred while creating the slog ES index."
                     "ES response:" resp))))))

(defmethod log :es [log-map]
  (esr/connect! (str (config :slog :es :connection-url)))
  (when-let [index (ensure-index)]
    (let [resp (esd/create index :entry log-map)]
      (when-not (esrsp/ok? resp)
        ;; TODO what to do with log-map? attempt slog.log it?
        (log/error "An error occurred while indexing the slog entry."
                   "ES response:" resp)))))