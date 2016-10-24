package spark.com.test

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream.toPairDStreamFunctions

object NetworkWordCount {
  
  def main(args: Array[String]) {
    
    if (args.length < 2) {
      
      System.err.println("Usage: NetworkWordCount <Hostname> <port>")
      System.exit(1)
      
    }
    
    // Create the context with a 1 second batch size
    val sparkConf = new SparkConf().setAppName("NetworkWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    
    
    val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)
    val words = lines.flatMap { _.split(" ") }
    val wordCounts = words.map { x => (x, 1) }.reduceByKey(_+_)
    
    wordCounts.print()
    ssc.start()
    ssc.awaitTermination()
    
  }
  
}