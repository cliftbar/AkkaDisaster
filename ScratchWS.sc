import java.time._
import java.time.format.DateTimeFormatter
val ts = "1900-01-01-15-12"
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
val dt = LocalDateTime.parse(ts, formatter)