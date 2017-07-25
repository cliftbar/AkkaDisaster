package com.cliftbar.akkadisaster

import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cliftbar.disastermodeling.hurricane.{nws23 => nws}

// Server definition
object AkkaDisasterApp extends HttpApp with App {
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
    path("calculateHurricane") { // Path to read and print json.  It's a little round about.  Test with {"item": "value"}
      get {
        entity(as[String]) { json =>
          println(Some("NaN".toDouble))
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

          object TrackPointJsonProtocol extends DefaultJsonProtocol with NullOptions {
            implicit object TrackPointJsonFormat extends RootJsonFormat[nws.TrackPoint] {
              def write(tp: nws.TrackPoint) =
                JsArray(JsString(tp.stormName), JsNumber(tp.rMax_nmi), JsNumber(tp.fSpeed_kts), JsNumber(tp.gwaf))
              def read (value: JsValue) = value match {
                case JsObject(fields) =>
                  println(fields)
                  val catalogNumber: Option[Int] = fields.get("catalogNumber").map(x => x.convertTo[Int]) //fields.get("catalogNumber").asInstanceOf[Some[Int]]
                  val stormName: String = fields("stormName").convertTo[String]
                  val basin: Option[String] = fields.get("basin").map(x => x.convertTo[String])
                  val timestamp: String = fields("timestamp").convertTo[String]
                  val eyeLat_y: Double = fields("eyeLat_y").convertTo[Double]
                  val eyeLon_x: Double = fields("eyeLon_x").convertTo[Double]
                  val maxWind_kts: Option[Double] = fields.get("maxWind_kts").map(x => x.convertTo[Double]) //fields.get("maxWind_kts").asInstanceOf[Some[Double]].orElse(None)
                  val minCp_mb: Option[Double] = fields.get("minCp_mb").map(x => x.convertTo[Double]) //fields.get("minCp_mb").asInstanceOf[Some[Double]].orElse(None)
                  val sequence: Double = fields("sequence").convertTo[Double]
                  val fSpeed_kts: Double = fields("fSpeed_kts").convertTo[Double]
                  val isLandfallPoint: Boolean = fields("isLandfallPoint").convertTo[Boolean]
                  val rMax_nmi: Double = fields("rMax_nmi").convertTo[Double]
                  val gwaf: Double = fields("gwaf").convertTo[Double]
                  val heading: Option[Double] = fields.get("heading").map(x => x.convertTo[Double]) //fields.get("heading").asInstanceOf[Some[Double]].orElse(None)
                  new nws.TrackPoint(
                    catalogNumber
                    ,stormName
                    ,basin
                    ,LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) //LocalDateTime.now()
                    ,eyeLat_y
                    ,eyeLon_x
                    ,maxWind_kts
                    ,minCp_mb
                    ,sequence
                    ,fSpeed_kts
                    ,isLandfallPoint
                    ,rMax_nmi
                    ,gwaf
                    ,heading
                  )
//              def read(value: JsValue) = {
//                value.asJsObject.getFields(
//                  "catalogNumber"
//                  ,"stormName"
//                  ,"basin"
//                  ,"timestamp"
//                  ,"eyeLat_y"
//                  ,"eyeLon_x"
//                  ,"maxWind_kts"
//                  ,"minCp_mb"
//                  ,"sequence"
//                  ,"fSpeed_kts"
//                  ,"isLandfallPoint"
//                  ,"rMax_nmi"
//                  ,"gwaf"
//                  ,"heading"
//                ) match {
//                  case Seq(
//                    JsNumber(catalogNumber)
//                    ,JsString(stormName)
//                    ,JsString(basin)
//                    , JsString(timestamp)
//                    , JsNumber(eyeLat_y)
//                    , JsNumber(eyeLon_x)
//                    , JsNumber(maxWind_kts)
//                    , JsNull
//                    , JsNumber(sequence)
//                    , JsNumber(fSpeed_kts)
//                    , JsBoolean(isLandfallPoint)
//                    , JsNumber(rMax_nmi)
//                    , JsNumber(gwaf)
//                    , JsNumber(heading)
//                  ) =>
//                      new nws.TrackPoint(
//                      Some(catalogNumber.toInt)
//                      ,stormName
//                      ,Some(basin)
//                      ,LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"))
//                      ,eyeLat_y.toDouble
//                      ,eyeLon_x.toDouble
//                      ,Some(maxWind_kts.toDouble)
//                      ,Some(0) //minCp_mb.contains()
//                      ,sequence.toDouble
//                      ,fSpeed_kts.toDouble
//                      ,isLandfallPoint
//                      ,rMax_nmi.toDouble
//                      ,gwaf.toDouble
//                      ,Some(heading.toDouble)
//                  )
//                  case _ => throw new DeserializationException(value.asJsObject.getFields("minCp_mb")(0))
//                }
              }
            }
          }

          import TrackPointJsonProtocol._
          val parsedTrack = track.convertTo[List[nws.TrackPoint]]
          //val trackList = track
//
          println(parsedTrack(0).catalogNumber.get)
          val str = parsedTrack(0).basin.get
          println(str)

          println(maxDist)

          complete("read json") // Completes with some text
        }
      }
    }


  // This will start the server until the return key is pressed
  startServer("localhost", 9000)
}
