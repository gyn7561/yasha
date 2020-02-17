package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.modals.YashaDbModal
import java.util.concurrent.atomic.AtomicInteger

interface ITaskDb {

    var yasha: Yasha

    fun size(): Int {
        return -1
    }

    data class PushTaskResult(val yashaDbModal: YashaDbModal, val success: Boolean)

    fun pushTask(yashaTask: YashaTask, force: Boolean = false, pushToStackBottom: Boolean = false, beforePushFunc: (YashaDbModal.() -> Unit)? = null): PushTaskResult

    fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit): YashaDbModal
    fun getNextTask(): YashaTask?
    fun isTaskFinished(taskIdentifier: String): Boolean

    fun lastSuccessTask(): YashaDbModal? {
        return null
    }

    fun finishedTaggedTaskSpeedMap(): Map<String, Double> {
        return mapOf()
    }


    fun finishedTaggedTaskCountMap(): Map<String, Int> {
        return mapOf()
    }

    fun finishedTaggedTaskLastOneMinSpeedMap(): Map<String, Double> {
        return mapOf()
    }

    fun canPush(yashaDbModal: YashaDbModal): Boolean {
        return yashaDbModal.taskDepth <= yasha.yashaConfig.maxDepth
    }

    fun downloadSpeed(): Double {
        return -1.0
    }

    val finishedTaskCount: Int
        get() {
            return -1
        }

    val unfinishedTaskCount: Int
        get() {
            return -1
        }

    val totalSpeed: Double
        get() {
            return (-1).toDouble()
        }

    val lastOneMinSpeed: Double
        get() {
            return (-1).toDouble()
        }

}