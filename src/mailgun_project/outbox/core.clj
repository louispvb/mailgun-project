(ns mailgun-project.outbox.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.util.concurrent TimeUnit ScheduledThreadPoolExecutor Callable]))


(def es (ScheduledThreadPoolExecutor. 4))

(defn periodically
  [n f]
  (.scheduleWithFixedDelay es ^Runnable f 0 n TimeUnit/MILLISECONDS))

(defn after
  [n f]
  (.schedule es ^Callable f n TimeUnit/MILLISECONDS))


; Send an email preriodically every 800ms using the welcome template inserting a count attribute
(defn example-queue-mails
  [ch exchange key address]
  (let [i      (atom 0)
        future (periodically 800 (fn []
                                   (try
                                     (println (str "Sending an email, #" @i))
                                     (lb/publish ch exchange key
                                                 (json/write-str {:to address
                                                                  :subject "Welcome"
                                                                  :template "welcome"
                                                                  :attributes {:user "Louis"
                                                                               :company "Credit"
                                                                               :link "http://example.com"
                                                                               :count @i}}))
                                     (swap! i inc)
                                     (catch Throwable t
                                       (.printStackTrace t)))))])

(def cli-options
  [["-a" "--address ADDR" "Email address to send example emails"
    :parse-fn #(String. %)]])

(defn -main
  [& args]
  ;; N connections imitate N apps
  (let [conn    (rmq/connect)
        chx      (lch/open conn)
        exchange "amq.direct"                               ; Use a direct exchange for round robin
        queue    "mailgun-project"                          ; Use a single outbox queue for all producers and consumers
        address (get-in (parse-opts args cli-options) [:options :address])]
    (lq/declare chx queue {:auto-delete false :exclusive false})
    (lq/bind    chx queue exchange {:routing-key "key1"})
    (example-queue-mails chx exchange "key1" address)
    (after 2800 (fn []
                  (println "Shutting down...")
                  (.shutdownNow es)
                  (lq/purge chx queue)
                  (rmq/close chx)
                  (rmq/close conn)))))
