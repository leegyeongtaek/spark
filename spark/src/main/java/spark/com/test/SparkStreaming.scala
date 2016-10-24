package spark.com.test

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds

object SparkStreaming extends App {
  
  val sparkConf = new SparkConf().setAppName("SparkStreaming")
  val ssc       = new StreamingContext(sparkConf, Seconds(1))
  
  
}