package com.cliftbar.akkadisaster

import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json.DefaultJsonProtocol._
import spray.json._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cliftbar.disastermodeling.hurricane.{nws23 => nws}

// Server model definition
//  Defines routes and converts incoming http data to scala types before
//  passing data AkkaDisasterApp.  Also converts scala types back to appropriate form for http.
object AkkaDisasterModel extends HttpApp with App {
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
        val j = Map("imageUri" -> "path\\here", "nws" -> nws.model.Rho0_kPa.toString)
        complete(j.toJson.toString) // Completes with some text
      }
    } ~
    path("calculateHurricane") { // Calculate a hurricane.  Parameters and track passed in with JSON
      get {
        entity(as[String]) { json =>
          val parsedJson = JsonParser(json).asJsObject
          println(json)

          // Get top level fields
          val maxDist: Int = parsedJson.fields("maxDist").convertTo[Int]
          val par: Int = parsedJson.fields("par").convertTo[Int]
          val fspeed: Int = parsedJson.fields("fspeed").convertTo[Int]
          val rmax: Int = parsedJson.fields("rmax").convertTo[Int]

          //get Bbox without custom protocol
          val Bbox = parsedJson.fields("BBox").asJsObject
          val pxPerDegreeX: Int = Bbox.fields("pxPerDegreeX").convertTo[Int]
          val pxPerDegreeY: Int = Bbox.fields("pxPerDegreeY").convertTo[Int]
          val botLatY: Float = Bbox.fields("botLatY").convertTo[Float]
          val topLatY: Float = Bbox.fields("topLatY").convertTo[Float]
          val rightLonX: Float = Bbox.fields("rightLonX").convertTo[Float]
          val leftLonX: Float = Bbox.fields("leftLonX").convertTo[Float]

          // Custom Protocol for Parsing track points in the json.
          //  This is "our" job, not the libraries, the library only defines the case class
          //  since it doesn't know how the data will be parsed.
          object TrackPointJsonProtocol extends DefaultJsonProtocol with NullOptions {
            implicit object TrackPointJsonFormat extends RootJsonFormat[nws.TrackPoint] {
              def write(tp: nws.TrackPoint) =
                JsArray(JsString(tp.stormName), JsNumber(tp.rMax_nmi), JsNumber(tp.fSpeed_kts), JsNumber(tp.gwaf))
              def read (value: JsValue) = value match {
                case JsObject(fields) => {
                  val catalogNumber: Option[Int] = fields.get("catalogNumber").map(x => x.convertTo[Int])
                  val stormName: String = fields("stormName").convertTo[String]
                  val basin: Option[String] = fields.get("basin").map(x => x.convertTo[String])
                  val timestamp: String = fields("timestamp").convertTo[String]
                  val eyeLat_y: Double = fields("eyeLat_y").convertTo[Double]
                  val eyeLon_x: Double = fields("eyeLon_x").convertTo[Double]
                  val maxWind_kts: Option[Double] = fields.get("maxWind_kts").map(x => x.convertTo[Double])
                  val minCp_mb: Option[Double] = fields.get("minCp_mb").map(x => x.convertTo[Double])
                  val sequence: Double = fields("sequence").convertTo[Double]
                  val fSpeed_kts: Double = fields("fSpeed_kts").convertTo[Double]
                  val isLandfallPoint: Boolean = fields("isLandfallPoint").convertTo[Boolean]
                  val rMax_nmi: Double = fields("rMax_nmi").convertTo[Double]
                  val gwaf: Double = fields("gwaf").convertTo[Double]
                  val heading: Option[Double] = fields.get("heading").map(x => x.convertTo[Double])

                  new nws.TrackPoint(
                    catalogNumber
                    , stormName
                    , basin
                    , LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))
                    , eyeLat_y
                    , eyeLon_x
                    , maxWind_kts
                    , minCp_mb
                    , sequence
                    , fSpeed_kts
                    , isLandfallPoint
                    , rMax_nmi
                    , gwaf
                    , heading
                  )
                }
                case _ => deserializationError("Track Point expected")
              }
            }
          }

          import TrackPointJsonProtocol._
          val track = parsedJson.fields("track").convertTo[List[nws.TrackPoint]]

          print(track(0).minCp_mb.get)
          print(", ")
          println("parsed successfully")
          complete("read json") // Completes with return text
        }
      }
    }


  // This will start the server until the return key is pressed
  startServer("localhost", 9000)
}
