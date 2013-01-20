(ns slog.test.es
  (:require [carica.core :refer [config override-config]]
            [clojure.test :refer :all]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [slog.core :refer :all]
            [slog.log :refer :all]))

;; TODO