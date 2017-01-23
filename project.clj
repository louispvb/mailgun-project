(defproject mailgun-project "0.1.0-SNAPSHOT"
  :description "Software Engineering Project."
  :url "http://github.com/Louispvb"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.4.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.5"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [ring "1.5.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-basic-authentication "1.0.5"]
                 [compojure "1.5.2"]]
  :profiles {:mailtool {:main mailgun-project.mailtool.core
                        :uberjar-name "mailtool.jar"}}
  :aot [mailgun-project.mailtool.core])
