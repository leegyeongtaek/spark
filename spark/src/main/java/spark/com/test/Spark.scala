package spark.com.test

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

object Spark extends App {
  
  
  val conf  = new SparkConf().setAppName("Spark").setMaster("local[*]")
  val sc    = new SparkContext(conf)
  
  val readme = sc.textFile("D:\\HDFS\\spark-1.3.0-bin-hadoop2.3\\README.md")
  
  // Spark 이라는 단어의 라인 RDD 추출  => Array[Int] = Array("Apache Spark", "Spark is a fast and general cluster computing system for Big Data. It provides", .....)
  val lineWithSpark = readme.filter(_.contains("Spark"))  
  
  // Spark line RDD를  공백으로 쪼개 단어별 map RDD를 만든 이후, 다시 단어별 1로 map RDD를 만듬
  // 처번째 map => Array(Array[Int], ....) = Array(Array("Apache", "Spark"), Array("Spark", "is", "a", "fast"....), Array(...), ....)
  // 두번째 map => Array(Array[Int], ....) = Array(Array("Apache"), Array("Spark"), Array(...), ....) , 배열의 첫번째 데이터 추출
  val words = lineWithSpark.map(_.split(" ")).map(r => r(0))  
  
  words.foreach(println)
  
  val aCnt = words.filter(_.contains("a")).count()
  val bCnt = words.filter(_.contains("b")).count()
  
  println("a count : %s, b count : %s".format(aCnt, bCnt))
  
}