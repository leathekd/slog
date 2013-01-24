(ns slog.log
  (:require [clojure.tools.logging :as log]
            [slog.core :refer [log log-map Sloggable]]))

(defrecord Logger []
  Sloggable
  (log [context log-map]
    (log/log (:level log-map) (pr-str log-map))))
