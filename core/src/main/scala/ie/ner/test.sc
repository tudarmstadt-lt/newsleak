
import scala.collection.mutable

val m1 = mutable.Map(1 -> 1, 2 ->1)
val m2 = mutable.Map(1 -> 1)

m1 ++= m2.map{ case (k,v) => k -> (v + m2.getOrElse(k,0)) }