(defproject mailgun-project "0.1.0-SNAPSHOT"
  :description "Software Engineering Project"
  :url "http://github.com/Louispvb"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main mailgun-project.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.4.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.5"]]
  :aot [mailgun-project.core])
