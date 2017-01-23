(ns mailgun-project.mailtool.core
  (:require [clojure.walk :refer [keywordize-keys]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [response]]
            [ring.middleware.json :as middleware]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [mailgun-project.templates.core :refer [render-template]]))


; I put a hard limit on the API key. I would have put the config as env vars or a file
; config but Intellij Cursive's REPL was not playing well with it
(def sandbox-domain "sandbox848a1c9e0126423d929e22ab6f3603ee.mailgun.org")
(def api-key "key-cf9242208bf8f59a63d250884b779d89")

(defn send-email
  "Sends an email taking a map containing :to :subject and :body"
  [opts]
  (let [{:keys [to subject template attributes]} opts]
    (client/post (str "https://api.mailgun.net/v3/" sandbox-domain "/messages")
                 {:basic-auth ["api" api-key]
                  :form-params {:from (str "Mailgun <postmaster@" sandbox-domain ">")
                                :to (str "<" to ">")
                                :subject subject
                                :html (render-template template attributes)}
                  :socket-timeout 1000 :conn-timeout 1000})))

(defn send-example
  "Sends an example email"
  []
  (send-email {:to "louispvb@gmail.com"
               :subject "Part 1."
               :body "<html><body><h1>Hello World!</h1></body></html>"}))
; The POST /sendmail route takes a json body of four keys, if they are not found a failure message is sent
; to the client. If they are found, and an error occurs sending email, another failure message is sent to
; the client.

; Since this is Clojure ring running on top of Java's jetty, it is reasonably performant and can handle
; a large amount of requests. The send-email function is a synchronous request, so the client would only get
; a response when the email has been successfully sent. This architecture could be changed to an asynchronous
; send-email request that queues an email to be sent, so that during peak email sending times the server wouldn't be
; overloaded. That is the next part however.

(defroutes app
           (GET "/" request
                {:status 200
                 :body {:success "Welcome to the sendmail API"}})
           (POST "/sendmail" request
                 (let [body (:body request)]
                   (if (every? body ["to" "subject" "template" "attributes"])
                     (try
                       (do
                         (send-email (keywordize-keys body))
                         {:status 200
                          :body {:success "Email sent"}})
                       (catch Exception e
                         {:status 200
                          :body {:fail "Unable to send email"}}))
                     {:status 400
                      :body {:fail "Missing one of the necessary keys: to, subject, template, attributes"}})))
           (route/not-found {:status 404
                             :body {:fail "Page not found"}}))

; Basic authentication middleware according to RFC2617 section 2, the send mail API cannot be accessed without name
; and password.
(defn authenticated? [name pass]
  (and (= name "Louis")
       (= pass "Mailgun")))

(def reloadable-app
  (-> app
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)
      (wrap-basic-authentication authenticated?)
      (wrap-reload)))

(def cli-options
  [["-j" "--json JSON" "Email json representation"
    :parse-fn #(json/read-str % :key-fn keyword)]])

(defn -main
  [& args]
  (run-jetty #'reloadable-app {:port 8080}))