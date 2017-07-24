/**
  * Created by cwbarclift on 7/23/2017.
  */
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import spray.json._
import DefaultJsonProtocol._


// Server definition
object FlaPyScalaApp extends HttpApp with App {
  // Routes that this WebServer must handle are defined here
  def routes: Route =
  pathEndOrSingleSlash { // Listens to the top `/`
    complete("Server up and running") // Completes with some text
  } ~
    path("hello") { // Hello path
      get { // Listens only to GET requests
        complete("Say hello to akka-http") // Completes with some text
      }
    } ~
    path("returnJson") { // Path to return json
      get {
        val j = Map("imageUri" -> "path\\here")
        complete(j.toJson.toString) // Completes with some text
      }
    } ~
    path("jsonIn") { // Path to read and print json.  It's a little round about.  Test with {"item": "value"}
      get {
        entity(as[String]) { json =>
          val parsed = JsonParser(json)
          println(json)
          println(parsed.asJsObject.fields("item").toString)
          complete("read json") // Completes with some text
        }
      }
    }


  // This will start the server until the return key is pressed
  startServer("localhost", 9000)
}

// Starting the server
//