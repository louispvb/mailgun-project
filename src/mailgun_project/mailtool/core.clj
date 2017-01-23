(ns mailgun-project.mailtool.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))


; I put a hard limit on the API key. I would have put the config as env vars or a file
; config but Intellij Cursive's REPL was not playing well
(def sandbox-domain "sandbox848a1c9e0126423d929e22ab6f3603ee.mailgun.org")
(def api-key "key-cf9242208bf8f59a63d250884b779d89")

(defn send-email
  "Sends an email taking a map containing :to :subject and :body"
  [opts]
  (let [{:keys [to subject body]} opts]
    (client/post (str "https://api.mailgun.net/v3/" sandbox-domain "/messages")
                 {:basic-auth ["api" api-key]
                  :form-params {:from (str "Mailgun <postmaster@" sandbox-domain ">")
                                :to (str "<" to ">")
                                :subject subject
                                :html body}})))

(defn send-example
  "Sends an example email"
  []
  (send-email {:to "louispvb@gmail.com"
               :subject "Part 1"
               :body "<html><body><h1>Hello World!</h1></body></html>"}))

(def cli-options
  [["-j" "--json JSON" "Email json representation"
    :parse-fn #(json/read-str % :key-fn keyword)]])

(defn -main
  "Does things"
  [& args]
  (let [options (:options (parse-opts args cli-options))
        email-map (:json options)]
    (send-email email-map)))