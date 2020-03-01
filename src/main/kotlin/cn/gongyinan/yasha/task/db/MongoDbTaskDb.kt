package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.converter.DefaultDbDataConverter
import cn.gongyinan.yasha.task.db.converter.IDbDataConverter
import cn.gongyinan.yasha.task.db.modals.YashaDbModal
import com.alibaba.fastjson.JSON
import com.google.gson.Gson
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import org.apache.logging.log4j.LogManager
import org.bson.*
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import org.bson.types.Binary
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.MutableList
import kotlin.collections.forEach
import kotlin.collections.getValue
import kotlin.collections.set
import kotlin.collections.sortBy


open class MongoDbTaskDb(private val mongoDatabase: MongoDatabase, private val collectionName: String, private val converter: IDbDataConverter = DefaultDbDataConverter(), val mongoDbObjectConverter: MongoDbObjectConverter = MongoDbObjectConverter()) : AbstractMemoryTaskDb(converter) {


    private val logger = LogManager.getLogger(MongoDbTaskDb::class.java)

    interface IMongoDbObjectConverter {
        fun convertToMongoDbObject(yashaDbModal: YashaDbModal): Document
        fun convertToYashaDbModal(mongoDbObj: Document): YashaDbModal
    }

    open class MongoDbObjectConverter() : IMongoDbObjectConverter {
        override fun convertToMongoDbObject(yashaDbModal: YashaDbModal): Document {
            val document = Document()
            document["_id"] = BsonString(yashaDbModal.taskIdentifier)

            if (yashaDbModal.parentTaskIdentifier != null) {
                document["parentTaskIdentifier"] = BsonString(yashaDbModal.parentTaskIdentifier)
            }

            document["taskDepth"] = yashaDbModal.taskDepth

            if (yashaDbModal.requestHeadersData != null) {
                document["requestHeadersData"] = BsonString(yashaDbModal.requestHeadersData)
            }

            document["requestUrl"] = BsonString(yashaDbModal.requestUrl)
            document["requestMethod"] = BsonString(yashaDbModal.requestMethod)

            if (yashaDbModal.requestBody != null) {
                document["requestBody"] = Binary(yashaDbModal.requestBody)
            }

            if (yashaDbModal.responseBody != null) {
                document["responseBody"] = Binary(yashaDbModal.responseBody)
            }


            if (yashaDbModal.responseUrl != null) {
                document["responseUrl"] = BsonString(yashaDbModal.responseUrl)
            }
            if (yashaDbModal.responseHeadersData != null) {
                document["responseHeadersData"] = BsonString(yashaDbModal.responseHeadersData)
            }

            if (yashaDbModal.responseCode != null) {
                document["responseCode"] = BsonInt32(yashaDbModal.responseCode!!)
            }

            if (yashaDbModal.extraData != null) {
                document["extraData"] = Binary(yashaDbModal.extraData)
            }

            if (yashaDbModal.subTaskCommands?.any() == true) {
                document["subTaskCommands"] = BsonArray(yashaDbModal.subTaskCommands!!.map { c -> BsonString(c) })
            }

            document["taskCommand"] = yashaDbModal.taskCommand

            if (yashaDbModal.contentType != null) {
                document["contentType"] = BsonString(yashaDbModal.contentType)
            }


            document["updateTime"] = BsonInt64(yashaDbModal.updateTime)
            document["createTime"] = BsonInt64(yashaDbModal.createTime)
            document["ready"] = BsonBoolean(yashaDbModal.ready)
            document["success"] = BsonBoolean(yashaDbModal.success)

            document["taskClass"] = BsonString(yashaDbModal.taskClass)

            if (yashaDbModal.taskBundleId != null) {
                document["taskBundleId"] = BsonString(yashaDbModal.taskBundleId)
            }

            if (yashaDbModal.nextFetchTime != null) {
                document["nextFetchTime"] = BsonInt64(yashaDbModal.nextFetchTime!!)
            }


            return document
        }


        override fun convertToYashaDbModal(mongoDbObj: Document): YashaDbModal {
            return YashaDbModal(
                    taskIdentifier = mongoDbObj.getString("_id"),
                    parentTaskIdentifier = if (mongoDbObj.containsKey("parentTaskIdentifier")) mongoDbObj.getString("parentTaskIdentifier") else null,
                    taskDepth = mongoDbObj.getInteger("taskDepth"),
                    requestHeadersData = if (mongoDbObj.containsKey("requestHeadersData")) mongoDbObj.getString("requestHeadersData") else null,
                    requestUrl = mongoDbObj.getString("requestUrl"),
                    requestMethod = mongoDbObj.getString("requestMethod"),
                    requestBody = if (mongoDbObj.containsKey("requestBody")) (mongoDbObj["requestBody"] as Binary).data else null,
                    responseBody = if (mongoDbObj.containsKey("responseBody")) (mongoDbObj["responseBody"] as Binary).data else null,
                    responseUrl = if (mongoDbObj.containsKey("responseUrl")) mongoDbObj.getString("responseUrl") else null,
                    responseHeadersData = if (mongoDbObj.containsKey("responseHeadersData")) mongoDbObj.getString("responseHeadersData") else null,
                    responseCode = if (mongoDbObj.containsKey("responseCode")) mongoDbObj.getInteger("responseCode") else null,
                    extraData = if (mongoDbObj.containsKey("extraData")) (mongoDbObj["extraData"] as Binary).data else null,
                    subTaskCommands = if (mongoDbObj.containsKey("subTaskCommands")) (mongoDbObj["subTaskCommands"] as ArrayList<String>).toTypedArray() else null,
                    taskCommand = mongoDbObj.getString("taskCommand"),
                    contentType = if (mongoDbObj.containsKey("contentType")) mongoDbObj.getString("contentType") else null,
                    updateTime = mongoDbObj.getLong("updateTime"),
                    createTime = mongoDbObj.getLong("createTime"),
                    success = mongoDbObj.getBoolean("success"),
                    ready = mongoDbObj.getBoolean("ready"),
                    nextFetchTime = if (mongoDbObj.containsKey("nextFetchTime")) mongoDbObj.getLong("nextFetchTime") else null,
                    taskBundleId = if (mongoDbObj.containsKey("taskBundleId")) mongoDbObj.getString("taskBundleId") else null
            )
        }
    }

    override val finishedTaskCount: Int
        get() = finishedTaskIdSet.size

    open fun upsertDocument(collection: MongoCollection<Document>, yashaDbModal: YashaDbModal) {
        val updateOptions = UpdateOptions()
        updateOptions.upsert(true)
        collection.updateOne(Filters.eq("_id", yashaDbModal.taskIdentifier), Document("\$set", mongoDbObjectConverter.convertToMongoDbObject(yashaDbModal)), updateOptions)
    }

    private val collection = mongoDatabase.getCollection(collectionName)

    override lateinit var yasha: Yasha

    override fun pushTask(yashaTask: YashaTask, force: Boolean, pushToStackBottom: Boolean, beforePushFunc: (YashaDbModal.() -> Unit)?): ITaskDb.PushTaskResult {
        val pushResult = super.pushTask(yashaTask, force, pushToStackBottom, beforePushFunc)
        if (pushResult.success) {
            upsertDocument(collection, pushResult.yashaDbModal)
        }
        return pushResult
    }

    override fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit): YashaDbModal {
        val dbModal = super.updateTask(yashaTask, beforeUpdateFunc)
        val doc = BsonDocument.parse(JSON.toJSONString(dbModal))
        doc["_id"] = BsonString(yashaTask.taskIdentifier)
        if (dbModal.success) {
            finishedTaskIdSet.add(dbModal.taskIdentifier)
        }
        upsertDocument(collection, dbModal)
        return dbModal
    }

    override fun isTaskFinished(taskIdentifier: String): Boolean {
        return finishedTaskIdSet.contains(taskIdentifier)
    }

    override fun canPush(yashaDbModal: YashaDbModal): Boolean {
        return !finishedTaskIdSet.contains(yashaDbModal.taskIdentifier) && !unfinishedTaskMap.containsKey(yashaDbModal.taskIdentifier)
    }

    override fun getNextTask(): YashaTask? {
        return super.getNextTask()
    }

    //初始化一个ID缓存
    private val finishedTaskIdSet = HashSet<String>()

    open fun init(collection: MongoCollection<Document>, finishedTaskIdSet: HashSet<String>, unfinishedTaskMap: HashMap<String, YashaDbModal>) {

        logger.info("开始初始化MONGODB已完成任务库")
        var start = System.currentTimeMillis()
        var cursor = collection.find(Filters.eq("ready", false)).projection(Projections.fields(Projections.include("_id"))).iterator()

        cursor.forEach { doc ->
            finishedTaskIdSet.add(doc.getString("_id"))
        }

        cursor.close()
        logger.info("初始化MONGODB已完成任务库完成,耗时${System.currentTimeMillis() - start}ms 已完成:${finishedTaskIdSet.size}")

        logger.info("开始初始化MONGODB未完成任务库")
        start = System.currentTimeMillis()

        cursor = collection.find(Filters.eq("ready", true)).iterator()
        cursor.forEach { doc ->
            unfinishedTaskMap[doc.getString("_id")] = mongoDbObjectConverter.convertToYashaDbModal(doc)
        }
        cursor.close()

        logger.info("初始化MONGODB未完成任务库完成,耗时${System.currentTimeMillis() - start}ms 未完成:${finishedTaskIdSet.size}")


        val list = ArrayList(unfinishedTaskMap.values)
        list.sortBy { d -> d.createTime }

        for (yashaDbModal in list) {
            taskStack.push(yashaDbModal)
        }
    }

    override fun init() {
        val start = System.currentTimeMillis()
        logger.info("开始初始化MONGODB任务库")
        init(collection, finishedTaskIdSet, unfinishedTaskMap)
        logger.info("初始化MONGODB任务库完成,耗时${System.currentTimeMillis() - start}ms 已完成:${finishedTaskIdSet.size} 待完成:${unfinishedTaskMap.size}")
    }

    fun getById(id:String): YashaDbModal? {
        var yashaDbModal : YashaDbModal? = null
        collection.find(Filters.eq("_id", id)).limit(1).iterator().forEach { doc->
            yashaDbModal = mongoDbObjectConverter.convertToYashaDbModal(doc)
        }
        return yashaDbModal
    }

}