import ratpack.handlebars.HandlebarsModule
import ratpack.jackson.JacksonModule

import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.jackson.Jackson.json
import static ratpack.groovy.Groovy.*
import static ratpack.websocket.WebSockets.websocket

import offtopic.KafkaService
import offtopic.KafkaSubscriber
import offtopic.Configuration
import offtopic.OfftopicClient

ratpack {
    bindings {
        offtopic.Configuration.instance.loadDefaults()
        offtopic.curator.CuratorPool.prepare(Configuration.instance.zookeepers)
        module HandlebarsModule
        module JacksonModule
    }

    handlers {
        get {
            render handlebarsTemplate('index.html')
        }

        get('topics') {
            topics = KafkaService.fetchTopics()
            println topics

            if (request.headers.get('Content-Type') == 'application/json') {
                render(json(topics))
            }
            else {
                render handlebarsTemplate('topics.html', topics: topics)
            }
        }

        get('topics/:name') {
            render "Fetching info for ${pathTokens.name}"
        }

        get('topics/:name/watch') {
            render handlebarsTemplate('topic-watch.html', name: pathTokens.name)
        }

        get('topics/:name/websocket') { ctx ->
            def client = new OfftopicClient(Configuration.instance)
            def grepper = null

            websocket(ctx) { ws ->
                println "Connected ${ws}"
                client.onMessageCallback = { m ->
                    println "called back with ${m} (grep: ${grepper})"
                    if ((grepper == null) || (m.raw =~ grepper)) {
                        ws.send(groovy.json.JsonOutput.toJson(m))
                    }
                }
                client.createSubscribersFor(pathTokens.name)
                print "subscribers created for ${pathTokens.name}"
                client.startSubscribers()
            } connect { sock ->
                sock.onClose {
                    client.shutdown()
                }
                sock.onMessage { msg ->
                    grepper = msg.text
                }
            }
        }

        get('brokers') {
            brokers = KafkaService.fetchBrokers()

            if (request.headers.get('Content-Type') == 'application/json') {
                render(json(brokers))
            }
            else {
                render handlebarsTemplate('brokers.html', brokers: brokers)
            }
        }

        /* set up a demo/dummy websocket listener */
        get("ws") { context ->
            websocket(context) { ws ->
            } connect {
                it.onClose {
                    println "closing"
                } onMessage {
                    "client sent me ${it}"
                }
            }
        }

        fileSystem "public", { f -> f.files() }
    }
}

