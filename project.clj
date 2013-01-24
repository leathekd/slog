(defproject slog "0.1.0-SNAPSHOT"
  :description "An experiment in logging Clojure maps rather than unparsable strings"
  :url "https://github.com/leathekd/slog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[sonian/carica "1.0.2"]
                 [cheshire "5.0.1"]
                 [clj-http "0.6.3"]
                 [clj-stacktrace "0.2.5"]
                 [org.clojure/tools.logging "0.2.3"]]
  :profiles {:dev {:dependencies [[log4j "1.2.17"]
                                  [org.clojure/clojure "1.4.0"]]}})
