(defproject slog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[sonian/carica "1.0.2"]
                 [clj-stacktrace "0.2.5"]
                 [clojurewerkz/elastisch "1.0.2"]
                 [org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[log4j "1.2.17"]
                                  [org.clojure/tools.logging "0.2.3"]]}})
