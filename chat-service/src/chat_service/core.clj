;;;; A Websocket Chat Service

(ns chat-service.core
  (:require
   [clojure.java.io :as jio])
  (:import
   [org.webbitserver
    WebServers
    WebSocketHandler
    HttpHandler
    HttpRequest
    HttpResponse
    HttpControl]
   [org.webbitserver.handler
    StaticFileHandler])
  (:gen-class))


;;; Resources

(defmacro static-resource
  "Serve static resources out of this folder."

  [relpath]
  `(StaticFileHandler. ~relpath))

(defn redirect
  "Send a 302 response, redirecting to `uri`."

  [res uri]
  (doto res
    (.status 302)
    (.header "Location" uri)
    (.end)))

(defmacro defhandler
  "A standard Webbit handler accepts a request, response, and control
  instance. Since Webbit shares the same thread over a request queue,
  handlers should try not to block on synchronous operations."

  [name [req res ctrl] & body]
  `(def ~name
     (proxy  [HttpHandler] []
       (handleHttpRequest [^HttpRequest ~req ^HttpResponse ~res ^HttpControl ~ctrl]
         ~@body))))


;;; Handlers

(defhandler home [req res control]
  "The homepage just redirects to a lobby."
  (redirect res "/lobby.html"))

(defn session-agent
  "Create an agent with an empty collection of sessions. Each
  connection has one session."

  []
  (agent {}))

(defn conj-session
  "Allocate a session for a new connection."

  [agent conn]
  (send agent assoc conn {}))

(defn disj-session
  "A connected dropped, deallocate the session."

  [agent conn]
  (send agent dissoc conn))

(defn all-connections
  "The sequence of all known connections."

  [agent]
  (keys @agent))

(defn broadcast
  "Send a message to all connected clients."

  [agent msg]
  (doseq [conn (all-connections agent)]
    (.send conn msg)))

(def lobby-chat
  "The lobby chatroom exchanges messages through a WebSocket. This is
  the Webbit Websocket handler interface."

  (let [clients (session-agent)]

    (proxy [WebSocketHandler] []
      (onOpen [conn]
        (conj-session clients conn))

      (onClose [conn]
        (disj-session clients conn))

      (onMessage [conn msg]
        (broadcast clients msg)))))


;;; Server

(defn start-app [port]
  (doto (WebServers/createWebServer port)
    (.add (static-resource "./resources/public"))
    (.add "/" home)
    (.add "/lobby/chat" lobby-chat)
    (.start))
  (println "server started, listening on" port))

(defn -main [port]
  (start-app (Integer/parseInt port)))


