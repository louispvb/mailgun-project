(ns mailgun-project.mailtool.core
  (:gen-class)
  (:require [clojure.walk :refer [keywordize-keys]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [response]]
            [ring.middleware.json :as middleware]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [compojure.core :refer :all]
            [compojure.route   :as route]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clj-pid.core      :as pid]
            [clojure.tools.cli :refer [parse-opts]]
            [mailgun-project.templates.core :refer [render-template]]))

; I put a hard limit on the API key. I would have put the config as env vars or a file
; config but Intellij Cursive's REPL was not playing well with it

(def sandbox-domain (atom ""))
(def api-key (atom ""))

(defn send-email
  "Sends an email taking a map containing :to :subject and :body"
  [opts]
  (let [{:keys [to subject template attributes]} opts]
    (client/post (str "https://api.mailgun.net/v3/" @sandbox-domain "/messages")
                 {:basic-auth ["api" @api-key]
                  :form-params {:from (str "Mailgun <postmaster@" @sandbox-domain ">")
                                :to (str "<" to ">")
                                :subject subject
                                :html (render-template template attributes)}
                  :socket-timeout 1000 :conn-timeout 1000})))


; *** REST API ***

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


; *** Message Subscription ***


; The email message handler handles rabbitmq payloads and serializes json to send email.
; If it is unable to send email the exception from Mailgun is caught and the message is displayed to terminal.
; That is where another queue should be setup to send a failure message for failure processing.
(defn email-msg-handler
  [ch {:keys [delivery-tag]} ^bytes payload]
  (let [payload-str (String. payload "UTF-8")
        email-json (json/read-str payload-str)]
    (println "Recieved message with delivery tag " delivery-tag)
    (if (every? email-json ["to" "subject" "template" "attributes"])
      (try
        (do
          (send-email (keywordize-keys email-json))
          (println "Email successfully sent"))
        (catch Exception e
          (println "Unable to send email: " (get (json/read-str (:body (ex-data e))) "message"))))
      (println "Missing one of the necessary keys: to, subject, template, attributes"))))

(defn start-consumer
  [ch queue id]
  (.start (Thread. #(lc/subscribe ch queue email-msg-handler {:auto-ack true}))))

(def cli-options
  [["-d" "--domain DOMAIN" "Sandbox domain to use for mailgun"
    :parse-fn #(String. %)]
   ["-k" "--key KEY" "API key for mailgun"
    :parse-fn #(String. %)]])

(defn -main
  [& args]
  (let [conn (rmq/connect)
        ch (lch/open conn)
        process-id (pid/current)
        exchange "amq.direct"
        queue "mailgun-project"
        options (:options (parse-opts args cli-options))]
    (reset! sandbox-domain (:domain options))
    (reset! api-key (:key options))
    (start-consumer ch queue (str "Consumer " process-id))
    (comment (run-jetty #'reloadable-app {:port 8080}))))