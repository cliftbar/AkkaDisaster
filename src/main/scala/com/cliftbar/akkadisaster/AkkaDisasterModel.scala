package com.cliftbar.akkadisaster

import java.io.FileWriter
import java.nio.file.Paths
import java.time

import cliftbar.disastermodeling.hurricane.TrackPoint
import cliftbar.disastermodeling.hurricane.nws23._


class AkkaDisasterModel {
  def CalculateHurricane(trackPoints:Seq[TrackPoint], bBox:BoundingBox, fSpeed_kts:Option[Double], rMax_nmi:Double, pxPerDegree:(Int, Int), maxDist:Int, par:Int = -1):Map[String, String] = {

    //println("Start Calculate Hurricane with NWS23")
    //println(time.LocalDateTime.now())
    val grid = new LatLonGrid(bBox.topLatY, bBox.botLatY, bBox.leftLonX, bBox.rightLonX, pxPerDegree._1, pxPerDegree._2)
    val event = new HurricaneEvent(grid, trackPoints.toList, rMax_nmi)

    val imageName = "test_image.png"
    event.DoCalcs(maxDist, par, imageName, "test_text.txt")

    //println(time.LocalDateTime.now())
    //println("Did test")

    val retPath = Paths.get(System.getProperty("user.dir"), imageName)
    //val retPath = System.getProperty("user.dir") + "\\" + imageName
    val ret = Map("imageUri" -> retPath.toAbsolutePath.toString)

    return ret
  }
}
