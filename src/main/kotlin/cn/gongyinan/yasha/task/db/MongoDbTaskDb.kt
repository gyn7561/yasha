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
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings


class MongoDbTaskDb(private val mongoDatabase: MongoDatabase, private val collectionName: String, private val converter: IDbDataConverter = DefaultDbDataConverter()) : AbstractMemoryTaskDb(converter) {

    open fun upsertDocument(collection: MongoCollection<Document>, yashaDbModal: YashaDbModal) {
        val updateOptions = UpdateOptions()
        updateOptions.upsert(true)
        val doc = BsonDocument.parse(JSON.toJSONString(yashaDbModal))
        doc["_id"] = BsonString(yashaDbModal.taskIdentifier)
        collection.updateOne(Filters.eq("_id", yashaDbModal.taskIdentifier), Document("\$set", doc), updateOptions)
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

    var relaxed: JsonWriterSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()

    init {
        var cursor = collection.find(Filters.eq("ready", false)).projection(Projections.fields(Projections.include("_id"))).iterator()
        cursor.forEach { doc ->
            finishedTaskIdSet.add(doc.getString("_id"))
        }

        cursor = collection.find(Filters.eq("ready", true)).iterator()
        cursor.forEach { doc ->
            val obj = Gson().fromJson<YashaDbModal>(doc.toJson(relaxed), YashaDbModal::class.java)
            unfinishedTaskMap[doc.getString("_id")] = obj
        }

        val list = ArrayList(unfinishedTaskMap.values)
        list.sortBy { d -> d.createTime }

        for (yashaDbModal in list) {
            taskStack.push(yashaDbModal)
        }

    }
}