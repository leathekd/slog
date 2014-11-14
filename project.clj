(defproject slog "0.2.0-SNAPSHOT"
  :description "An experiment in logging Clojure maps rather than unparsable strings"
  :url "https://github.com/leathekd/slog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.logging "0.3.1"]]
  :profiles {:dev {:dependencies [[log4j "1.2.17"]
                                  [org.clojure/clojure "1.6.0"]]}})
