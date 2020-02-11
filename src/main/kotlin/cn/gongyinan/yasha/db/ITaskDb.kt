package cn.gongyinan.yasha.db

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.YashaTask
import java.lang.RuntimeException

interface ITaskDb {

    fun size(): Int
    fun addTask(yashaDBModal: YashaDbModal, force: Boolean = false): Boolean
    fun updateTask(yashaDBModal: YashaDbModal)
    fun getNextTask(): YashaTask?
    fun containsTask(taskIdentifier: String): Boolean
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