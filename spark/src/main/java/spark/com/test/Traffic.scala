package spark.com.test

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver
import akka.actor.ActorSystem
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject
import org.apache.spark.sql.SQLContext
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.HTTP
import scala.concurrent.duration.DurationInt
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.http.client.methods.HttpUriRequest

/**
 * 교통정보공개서비스 에서  공사정보 / 사고정보 에 대한 json data를 customer receiver를 생성해서 20초 간격으로 읽어 들인 후
 * Spark SQL로 DataFrame을 생성해서 출력한다.
 * 
 * 
 */
object Traffic {
  
  def main(args : Array[String]) = {
    
    val conf = new SparkConf().setAppName("Traffic")
    val ssc = new StreamingContext(conf, Seconds(20)) // 교통정보서비스가 Stream을 20초 간격으로 내보냄.
    
    // Custom receiver 생성 (2가지 공사정보 receiver, 사고정보 receiver)
    val eventStream = ssc.receiverStream(new EventReceiver)
    val incidentStream = ssc.receiverStream(new incidentReceiver)
    
    val info = eventStream.union(incidentStream)
    
    // 분산환경일 경우에는 직렬화 처리가 가능한 객체 여부를 고려해서 생성해 준다.
    info.foreachRDD(rdd => {
      
      println(rdd.count()+"!!!!!!!!!!!!!!!!!test1!!!!!!!!!!!!!!") 
      
      if(rdd.count() > 0) {
        
        println("!!!!!!!!!!!!!!!!!test2!!!!!!!!!!!!!!") 
      
//        rdd.foreachPartition(record => {
          
         println("test!!!!!!!!!!!!!!")
         
          // sql context 생성 
         val sqlContext = new SQLContext(rdd.context)
         
         
         println(rdd.collect())
         println(sqlContext)
         
         // RDD에 해당하는 DataFrame 생성
         // JSON 형태의 DataFrame은 자동으로 json의 key를 Schema의 Column으로 인식한다.
//         val trafficInfo = sqlContext.read.json(rdd)
         val trafficInfo = sqlContext.jsonRDD(rdd)
         
         trafficInfo.registerTempTable("TrafficData")
         
         val result = sqlContext.sql("select type, time, x, y, message from TrafficData").distinct()
         
         result.foreach(println)
          
//        })
      
      }
      
    })
    
    ssc.start()
    ssc.awaitTermination()
    
  }
  
  // 공사정보 receiver,
  class EventReceiver extends Receiver[String](StorageLevel.MEMORY_AND_DISK) {
    
    override def onStart() {
      
      println("start.....")
      
      receive();
      
    }
    
    override def onStop() {
      
      println("stop.....")
      
    }
    
    // 1. 이벤트를 받아오는 부분
    // 2. 받아온 이벤트를 parsing 하여 처리하는 부분
    // 20 초 간격으로 getEventData가 처리되도록 Schedule을 처리
    def receive() {
      
      // 20 초 간격으로 getEventData가 처리되도록 Schedule을 처리
      val actorSystem = ActorSystem("MyActorSystem") 
      
      // seconds를 사용하기 위해 import 함
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._
      
      actorSystem.scheduler.schedule(0 seconds, 20 seconds){
        
        val json = getEventData()
      
        parse(json)
        
      }
      
    }
    
    // http client를 생성해서 해당 url 에서 json string data를 읽어 드린다.
    def getEventData() = {
      
      // JSON TYPE URL
      val baseUrl = "http://openapi.its.go.kr/api/NEventIdentity?key=1447062467480&ReqType=2&getType=json&MinX=127.100000&MaxX=128.890000&MinY=34.100000&MaxY=39.100000&type=its"
      
      val client = HttpClients.createDefault()
      
      val httpGet = new HttpGet(baseUrl)
      
      val response = client.execute(httpGet)
      
      try {
        
        val entity = response.getEntity
        
//        new String(EntityUtils.toByteArray(entity))  // return JSON String Data Type
        EntityUtils.toString(entity, HTTP.UTF_8)        
      }
      finally {
        
        response.close()
        client.close()
        
      }
    }
      
    // json string data를 읽어 들여서 json object를 생성한 뒤 receiver stream에 저장한다. 
    def parse(json:String) = {
      
      val jsonArray = new JSONArray(json)
      
      for(i <-0 to jsonArray.length() - 1) {
        
        val jstr = jsonArray.getJSONObject(i)
        val newJson = new JSONObject()
        
        newJson.put("type", "Event")
        newJson.put("x", jstr.getDouble("coordx"))
        newJson.put("y", jstr.getDouble("coordy"))
        newJson.put("message", jstr.getString("eventstatusmsg"))
        newJson.put("time", new java.util.Date().toString())
        
        // JSON 데이터를 receiver stream에 저장
        store(newJson.toString())
        
      }
      
    }
      
  }
  
  
  // 사고정보 receiver,
  class incidentReceiver extends Receiver[String](StorageLevel.MEMORY_AND_DISK) {
    
    override def onStart() {
      
      println("start.....")
      
      receive();
      
    }
    
    override def onStop() {
      
      println("stop.....")
      
    }
    
    // 1. 이벤트를 받아오는 부분
    // 2. 받아온 이벤트를 parsing 하여 처리하는 부분
    // 20 초 간격으로 getEventData가 처리되도록 Schedule을 처리
    def receive() {
      
      // 20 초 간격으로 getEventData가 처리되도록 Schedule을 처리
      val actorSystem = ActorSystem("MyActorSystem") 
      
      // seconds를 사용하기 위해 import 함
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._
      
      actorSystem.scheduler.schedule(0 seconds, 20 seconds){
        
        val json = getEventData()
      
        parse(json)
       
      }
      
    }
    
    // http client를 생성해서 해당 url 에서 json string data를 읽어 드린다.
    def getEventData() = {
      
      // JSON TYPE URL
      val baseUrl = "http://openapi.its.go.kr/api/NIncidentIdentity?key=1447062467480&ReqType=2&getType=json&MinX=127.100000&MaxX=128.890000&MinY=34.100000&MaxY=39.100000&type=its"
      
      val client = HttpClients.createDefault()
      val httpGet = new HttpGet(baseUrl)
      
      val response = client.execute(httpGet)
      
      try {
        
        val entity = response.getEntity
        
//        new String(EntityUtils.toByteArray(entity))  // return JSON String Data Type
        EntityUtils.toString(entity, HTTP.UTF_8)
        
      }
      finally {
        
        response.close()
        client.close()
        
      }
    }
      
    // json string data를 읽어 들여서 json object를 생성한 뒤 receiver stream에 저장한다. 
    def parse(json:String) = {
      
      val jsonArray = new JSONArray(json)
      
      for(i <-0 to jsonArray.length() - 1) {
        
        val jstr = jsonArray.getJSONObject(i)
        val newJson = new JSONObject()
        
        newJson.put("type", "Event")
        newJson.put("x", jstr.getDouble("coordx"))
        newJson.put("y", jstr.getDouble("coordy"))
        newJson.put("message", jstr.getString("incidentmsg"))
        newJson.put("time", new java.util.Date().toString())
        
        // JSON 데이터를 receiver stream에 저장
        store(newJson.toString())
        
      }
      
    }
      
  }
  
}