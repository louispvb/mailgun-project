# mailgun-project

## Description
![Diagram](http://i.imgur.com/ctUwZgt.png)

For handling a mailing queue architecture, RabbitMQ was chosen with a Direct Exchange 
and single 'outbox' queue. The outbox queue can receive many messages and routes it to
the Direct Exchange, using the same routing key for every message. The Direct exchange
will then dispatch in a load balance in a round-robin configuration to all Mailgun API tool
processes. Ideally they would POST request the API asynchronously to handle many emails at
the same time. When the server cannot handle anymore load, more mail tool nodes should be
added. The benefit of this configuration is that the queue does not have to handle any 
load other than sending messages, while a cluster of consumer API nodes can wait for the 
email response and put it in a success or failure queue when done.

Notes: Git commits contain each part of the assignment in isolation, some code was removed to build
new features on top of old parts.

## Installation

* Install RabitMQ with `brew install rabbitmq`
* Start the service with `brew services start rabbitmq`
* Optionally access the RabbitMQ admin panel at `http://localhost:15672/` with user 'guest' and pass 'guest'
* In the base directory run `lein with-profile mailtool:outbox uberjar` to create jars for both the outbox queue and mailtool
* Run the outbox to initialize the RabbitMQ queue, `java -jar target/outbox.jar --address DUMMY_ADDRESS`
* Run as many mail tool services as needed, replacing the APIKEY and SANDBOXDOMAINS `java -jar target/mailtool.jar -k APIKEY -d SANDBOXDOMAIN`
* Run the outbox again to send four example emails round robin to the mail tools, `java -jar target/outbox.jar --address RECIPIENT_EMAIL_ADDRESS`

## TODO
* Success and failure queues
* Master slave cluster configuration for high availability of queue watching process with RabbitMQ
that replaces master with slave when master fails.

