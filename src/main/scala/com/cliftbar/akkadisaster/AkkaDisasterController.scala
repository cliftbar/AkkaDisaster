package com.cliftbar.akkadisaster

import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.{HttpApp, Route}
import spray.json.DefaultJsonProtocol._
import spray.json._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time._

//import akka.http.javadsl.model.headers.RawHeader
import akka.http.scaladsl.model.headers.RawHeader
import cliftbar.disastermodeling.hurricane.TrackPoint
import cliftbar.disastermodeling.hurricane.{nws23 => nws}

// Server model definition
//  Defines routes and converts incoming http data to scala types before
//  passing data AkkaDisasterApp.  Also converts scala types back to appropriate form for http.
object AkkaDisasterController extends HttpApp with App {
  // Instance of model class
  val model = new AkkaDisasterModel
  val id = (math.random() * 1000).toInt
  // Routes that this WebServer must handle are defined here
  def routes: Route =
  pathEndOrSingleSlash { // Listens to the top `/`
    Thread.sleep(1500)
    respondWithHeader(RawHeader("id", id.toString)) {
      complete("Server " + id.toString + " up and running") // Completes with some text
    }
  } ~
    path("async") { // Hello path
      get { // Listens only to GET requests
        entity(as[String]) { json =>
          Thread.sleep(1500)
          val parsedJson = JsonParser(json).asJsObject
          val callId: Int = parsedJson.fields("call_id").convertTo[Int]
          respondWithHeader(RawHeader("call_id", callId.toString)) {
            complete("async hellp") // Completes with some text
          }
        }
      }
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
    path("calculate" / "hurricane" / "nws23") { // Calculate a hurricane.  Parameters and track passed in with JSON
      withoutRequestTimeout {
        get {
          entity(as[String]) { json =>
            val parsedJson = JsonParser(json).asJsObject
            println(json)

            // Get top level fields
            val maxDist: Int = parsedJson.fields("maxDist").convertTo[Int]
            val par: Int = parsedJson.fields("par").convertTo[Int]
            val fspeed: Option[Double] = parsedJson.fields.get("fspeed").map(x => x.convertTo[Double])
            val rmax: Double = parsedJson.fields("rmax").convertTo[Double]
            val id: String = parsedJson.fields("id").convertTo[String]

            //get Bbox without custom protocol
            val jsonBbox = parsedJson.fields("BBox").asJsObject
            val pxPerDegreeX: Int = jsonBbox.fields("pxPerDegreeX").convertTo[Int]
            val pxPerDegreeY: Int = jsonBbox.fields("pxPerDegreeY").convertTo[Int]
            val botLatY: Float = jsonBbox.fields("botLatY").convertTo[Float]
            val topLatY: Float = jsonBbox.fields("topLatY").convertTo[Float]
            val rightLonX: Float = jsonBbox.fields("rightLonX").convertTo[Float]
            val leftLonX: Float = jsonBbox.fields("leftLonX").convertTo[Float]
            val bBox = new nws.BoundingBox(topLatY, botLatY, leftLonX, rightLonX)

            // Custom Protocol for Parsing track points in the json.
            //  This is "our" job, not the libraries, the library only defines the case class
            //  since it doesn't know how the data will be parsed.
            object TrackPointJsonProtocol extends DefaultJsonProtocol with NullOptions {

              implicit object TrackPointJsonFormat extends RootJsonFormat[TrackPoint] {
                val dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

                def write(tp: TrackPoint) = JsObject(
                  "catalogNumber" -> tp.catalogNumber.map(JsNumber(_)).getOrElse(JsNull)
                  , "stormName" -> JsString(tp.stormName)
                  , "basin" -> tp.basin.map(JsString(_)).getOrElse(JsNull)
                  , "timestamp" -> JsString(tp.timestamp.format(dtFormatter))
                  , "eyeLat_y" -> JsNumber(tp.eyeLat_y)
                  , "eyeLon_x" -> JsNumber(tp.eyeLon_x)
                  , "maxWind_kts" -> tp.maxWind_kts.map(JsNumber(_)).getOrElse(JsNull)
                  , "minCp_mb" -> tp.minCp_mb.map(JsNumber(_)).getOrElse(JsNull)
                  , "sequence" -> JsNumber(tp.sequence)
                  , "fSpeed_kts" -> JsNumber(tp.fSpeed_kts)
                  , "isLandfallPoint" -> JsBoolean(tp.isLandfallPoint)
                  , "rMax_nmi" -> JsNumber(tp.rMax_nmi)
                  , "gwaf" -> JsNumber(tp.gwaf)
                  , "heading" -> tp.heading.map(JsNumber(_)).getOrElse(JsNull)
                )

                def read(value: JsValue) = value match {
                  case JsObject(fields) => {
                    val catalogNumber: Option[Int] = fields("catalogNumber").convertTo[Option[Int]]
                    val stormName: String = fields("stormName").convertTo[String]
                    val basin: Option[String] = fields("basin").convertTo[Option[String]]
                    val timestamp: String = fields("timestamp").convertTo[String]
                    val eyeLat_y: Double = fields("eyeLat_y").convertTo[Double]
                    val eyeLon_x: Double = fields("eyeLon_x").convertTo[Double]
                    val maxWind_kts: Option[Double] = fields("maxWind_kts").convertTo[Option[Double]]
                    val minCp_mb: Option[Double] = fields("minCp_mb").convertTo[Option[Double]]
                    val sequence: Double = fields("sequence").convertTo[Double]
                    val fSpeed_kts: Double = fields("fSpeed_kts").convertTo[Double]
                    val isLandfallPoint: Boolean = fields("isLandfallPoint").convertTo[Boolean]
                    val rMax_nmi: Double = fields("rMax_nmi").convertTo[Double]
                    val gwaf: Double = fields("gwaf").convertTo[Double]
                    val heading: Option[Double] = fields("heading").convertTo[Option[Double]]

                    new TrackPoint(
                      catalogNumber
                      , stormName
                      , basin
                      , LocalDateTime.parse(timestamp, dtFormatter)
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
            val track = parsedJson.fields("track").convertTo[Seq[TrackPoint]]

            println("parsed successfully")
            println(track.toJson)

            // Run calculation
            val retMap = model.CalculateHurricane(track, bBox, fspeed, rmax, (pxPerDegreeX, pxPerDegreeY), maxDist, par)
            println(retMap)
            // package return map as json to send back to caller
            val retJson = retMap.toJson
            //complete(retJson.toString) // Return the image name as JSON string
            import java.io.File
            import akka.http.scaladsl.model.{HttpEntity, MediaTypes}

            import java.nio.file.Files
            val fi = new File(retMap("imageUri"))
            val fileContent = Files.readAllBytes(fi.toPath)

            val responseEntity = HttpEntity(
              MediaTypes.`image/png`
              ,fileContent
            )
            complete(responseEntity)
          }
        }
      }
    }


  // This will start the server until the return key is pressed
  val httpConfig = ConfigFactory.load().getConfig("akka.http")
  val interface = httpConfig.getString("server.interface")
  val port = httpConfig.getInt("server.port")
  println(interface)
  println(port)
  println("ServerID " + id.toString)
  startServer(interface, port)
}
