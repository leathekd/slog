(ns slog.log
  (:require [clojure.tools.logging :as log]
            [slog.core :refer [log log-map]]))

(defmethod log :log [_ log-map]
  (log/log (:level log-map) (pr-str log-map)))
