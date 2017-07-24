/**
  * Created by cwbarclift on 7/23/2017.
  */
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import spray.json._
import DefaultJsonProtocol._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cliftbar.disastermodeling.hurricane.{nws23 => nws}

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
        val j = Map("imageUri" -> "path\\here", "nws" -> nws.model.Rho0_kPa.toString)
        complete(j.toJson.toString) // Completes with some text
      }
    } ~
    path("jsonIn") { // Path to read and print json.  It's a little round about.  Test with {"item": "value"}
      get {
        entity(as[String]) { json =>
          val parsed = JsonParser(json).asJsObject
          println(json)

          val maxDist: Int = parsed.fields("maxDist").convertTo[Int]
          val par: Int = parsed.fields("par").convertTo[Int]
          val fspeed: Int = parsed.fields("fspeed").convertTo[Int]
          val rmax: Int = parsed.fields("rmax").convertTo[Int]
          //Bbox

          val Bbox = parsed.fields("BBox").asJsObject
          val pxPerDegreeX: Int = Bbox.fields("pxPerDegreeX").convertTo[Int]
          val pxPerDegreeY: Int = Bbox.fields("pxPerDegreeY").convertTo[Int]
          val botLatY: Float = Bbox.fields("botLatY").convertTo[Float]
          val topLatY: Float = Bbox.fields("topLatY").convertTo[Float]
          val rightLonX: Float = Bbox.fields("rightLonX").convertTo[Float]
          val leftLonX: Float = Bbox.fields("leftLonX").convertTo[Float]

          val track = parsed.fields("track")

          object TrackPointJsonProtocol extends DefaultJsonProtocol {
            implicit object TrackPointJsonFormat extends RootJsonFormat[nws.TrackPoint] {
              def write(tp: nws.TrackPoint) =
                JsArray(JsString(tp.stormName), JsNumber(tp.rMax_nmi), JsNumber(tp.fSpeed_kts), JsNumber(tp.gwaf))
              def read(value: JsValue) = {
                value.asJsObject.getFields(
                  "catalogNumber"
                  ,"stormName"
                  ,"basin"
                  ,"timestamp"
                  ,"eyeLat_y"
                  ,"eyeLon_x"
                  ,"maxWind_kts"
                  ,"minCp_mb"
                  ,"sequence"
                  ,"fSpeed_kts"
                  ,"isLandfallPoint"
                  ,"rMax_nmi"
                  ,"gwaf"
                  ,"heading"
                ) match {
                  case Seq(
                    JsNumber(catalogNumber)
                    ,JsString(stormName)
                    ,JsString(basin)
                    , JsString(timestamp)
                    , JsNumber(eyeLat_y)
                    , JsNumber(eyeLon_x)
                    , JsNumber(maxWind_kts)
                    , JsNumber(minCp_mb)
                    , JsNumber(sequence)
                    , JsNumber(fSpeed_kts)
                    , JsBoolean(isLandfallPoint)
                    , JsNumber(rMax_nmi)
                    , JsNumber(gwaf)
                    , JsNumber(heading)
                  ) =>
                      new nws.TrackPoint(
                      Some(catalogNumber.toInt)
                      ,stormName
                      ,Some(basin)
                      ,LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))
                      ,eyeLat_y.toDouble
                      ,eyeLon_x.toDouble
                      ,Some(maxWind_kts.toDouble)
                      ,Some(minCp_mb.toDouble)
                      ,sequence.toDouble
                      ,fSpeed_kts.toDouble
                      ,isLandfallPoint
                      ,rMax_nmi.toDouble
                      ,gwaf.toDouble
                      ,Some(heading.toDouble)
                  )
                  case _ => throw new DeserializationException("Expected Track Point")
                }
              }
            }
          }

          import TrackPointJsonProtocol._
          val parsedTrack = track.convertTo[Seq[nws.TrackPoint]]
          //val trackList = track
//
          println(parsedTrack(0).stormName)

          println(maxDist)
//          val catalogNumber : scala.Option[scala.Int]
//          val stormName : _root_.scala.Predef.String
//          val basin : scala.Option[_root_.scala.Predef.String]
//          val timestamp : java.time.LocalDateTime
//          val eyeLat_y : scala.Double
//          val eyeLon_x : scala.Double
//          val maxWind_kts : scala.Option[scala.Double]
//          val minCp_mb : scala.Option[scala.Double]
//          val sequence : scala.Double
//          val fSpeed_kts : scala.Double
//          val isLandfallPoint : scala.Boolean
//          val rMax_nmi : scala.Double
//          val gwaf : scala.Double
//          val heading : scala.Option[scala.Double]
          complete("read json") // Completes with some text
        }
      }
    }


  // This will start the server until the return key is pressed
  startServer("localhost", 9000)
}

// Starting the server
//