package spark.com.test

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import scala.collection.immutable.List
import org.apache.spark.HashPartitioner
import org.apache.spark.streaming.dstream.DStream.toPairDStreamFunctions

object StatefulNetworkWordCount {
  
  def main(args: Array[String]) {
    
    if (args.length < 2) {
      
      System.err.println("Usage: StatefulNetworkWordCount <Hostname> <Port>")
      System.exit(1)
      
    }
    
    // words count update function
    val updateFunc = (values: Seq[Int], state: Option[Int]) => {
      
      val currentCount = values.sum          // values로 넘어오는 Seq Array를 sum한다.
      val previousCount = state.getOrElse(0) // 이전 상태 option value를 가져온다.
      
      Some(currentCount + previousCount)  // Apply curCnt + prevCnt
      
    }
    
    // Iterator 파라미터로 item은 (mapKey, mapValue, Option) 으로 받는다.
    val newUpdateFunc = (iterator: Iterator[(String, Seq[Int], Option[Int])]) => {
      // iterator RDD를 flatMap으로 펼친다. 
      // RDD의 mapValue와 Option을 updateFunc로 넘겨 이전값과 현재 값의 배열합과 sum을 한 후 리턴
      // 이 RDD를 다시 mapKey와 sum값인 mapValue로 만든다. 
      iterator.flatMap(t => updateFunc(t._2, t._3).map { s => (t._1, s) })
    }
    
    val sparkConf = new SparkConf().setAppName("StatefulNetworkWordCount").setMaster("local[*]")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    ssc.checkpoint(".")    // updateStateByKey 를 사용하기 위해
    
    val initialRDD = ssc.sparkContext.parallelize(List(("hello", 1), ("world", 1)))
    
    val lines = ssc.socketTextStream(args(0), args(1).toInt)
    val words = lines.flatMap { _.split(" ") }
    val wordDstream = words.map { x => (x, 1) }
    
    val stateDstream = wordDstream.updateStateByKey(newUpdateFunc, new HashPartitioner(ssc.sparkContext.defaultParallelism), true, initialRDD)
    
    stateDstream.print()
    ssc.start()
    ssc.awaitTermination()
    
  }
  
}