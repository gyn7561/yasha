package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.converter.IDbDataConverter
import cn.gongyinan.yasha.task.db.modals.YashaDbModal
import cn.gongyinan.yasha.utils.SpeedRecorder
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

abstract class AbstractMemoryTaskDb(private val converter: IDbDataConverter) : ITaskDb {

    private val logger = LogManager.getLogger(AbstractMemoryTaskDb::class.java)

    open val unfinishedTaskMap = HashMap<String, YashaDbModal>()
    open val taskStack = Stack<YashaDbModal>()

    override val unfinishedTaskCount: Int
        get() = unfinishedTaskMap.size

    override lateinit var yasha: Yasha

    private val speedRecorder = SpeedRecorder()

    private var lastSuccessTaskInner: YashaDbModal? = null

    private val taggedTaskMap = HashMap<String, SpeedRecorder>()

    override val lastOneMinSpeed: Double
        get() = speedRecorder.lastOneMinSpeed()

    override val totalSpeed: Double
        get() = speedRecorder.speed()

    override fun lastSuccessTask(): YashaDbModal? {
        return lastSuccessTaskInner
    }

    @Synchronized
    override fun getNextTask(): YashaTask? {
        return if (!taskStack.empty()) {
            taskStack.pop().toYashaTask()
        } else {
            null
        }
    }

    override fun pushTask(yashaTask: YashaTask, force: Boolean, pushToStackBottom: Boolean, beforePushFunc: (YashaDbModal.() -> Unit)?): ITaskDb.PushTaskResult {
        val dbModal = converter.toYashaDbModal(yashaTask)
        beforePushFunc?.invoke(dbModal)
        if (force || canPush(dbModal)) {
            if (!pushToStackBottom) {
                taskStack.push(dbModal)
            } else {
                taskStack.add(0, dbModal)
            }
            unfinishedTaskMap[dbModal.taskIdentifier] = dbModal
            return ITaskDb.PushTaskResult(dbModal, true)
        }
        return ITaskDb.PushTaskResult(dbModal, false)
    }

    override fun finishedTaggedTaskSpeedMap(): Map<String, Double> {
        val result = HashMap<String, Double>()
        try {
            for (key in taggedTaskMap.keys) {
                result[key] = taggedTaskMap[key]!!.speed()
            }
            return result
        } catch (e: Exception) {
            return finishedTaggedTaskSpeedMap()
        }
    }

    override fun finishedTaggedTaskCountMap(): Map<String, Int> {
        val result = HashMap<String, Int>()
        try {
            for (key in taggedTaskMap.keys) {
                result[key] = taggedTaskMap[key]!!.total()
            }
            return result
        } catch (e: Exception) {
            return finishedTaggedTaskCountMap()
        }
    }

    override fun finishedTaggedTaskLastOneMinSpeedMap(): Map<String, Double> {
        val result = HashMap<String, Double>()
        try {
            for (key in taggedTaskMap.keys) {
                result[key] = taggedTaskMap[key]!!.lastOneMinSpeed()
            }
            return result
        } catch (e: Exception) {
            return finishedTaggedTaskLastOneMinSpeedMap()
        }
    }


    override fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit): YashaDbModal {
        val dbModal = converter.toYashaDbModal(yashaTask)
        beforeUpdateFunc.invoke(dbModal)
        if (dbModal.success) {
            lastSuccessTaskInner = dbModal
            speedRecorder.add(1)
            val tag = yasha.yashaConfig.listener.onTaskClassifier(yashaTask) ?: "YASHA__UNTAGGED__"
            if (!taggedTaskMap.containsKey(tag)) {
                taggedTaskMap[tag] = SpeedRecorder()
            }
            taggedTaskMap[tag]!!.add(1)
            unfinishedTaskMap.remove(dbModal.taskIdentifier)
            downloadSpeedRecorder.add(dbModal.responseBody?.size ?: 0)
        }
        return dbModal
    }

    private val downloadSpeedRecorder = SpeedRecorder()
    override fun downloadSpeed(): Double {
        return downloadSpeedRecorder.lastOneMinCount().toDouble() / 60
    }

}