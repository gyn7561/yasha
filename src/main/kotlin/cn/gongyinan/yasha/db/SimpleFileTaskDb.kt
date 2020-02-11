package cn.gongyinan.yasha.db

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.YashaTask
import cn.gongyinan.yasha.utils.SpeedRecorder
import com.alibaba.fastjson.JSON
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.system.exitProcess

class SimpleFileTaskDb(private val filePath: String) : ITaskDb {

    private val speedRecorder = SpeedRecorder()

    private val logger = LogManager.getLogger(SimpleFileTaskDb::class.java)
    private val finishedTaskIdSet = Collections.synchronizedSet(HashSet<String>())
    private val unfinishedTaskMap = Collections.synchronizedMap(HashMap<String, YashaDbModal>())
    private val taskStack = Stack<YashaDbModal>()

    private val finishedTaskIdJsonPath = "$filePath/finishedTaskIdSet.json"
    private val writeSuccessFilePath = "$filePath/write.success"
    private val unfinishedTaskListJsonPath = "$filePath/unfinishedTaskList.json"

    override val unfinishedTaskCount: Int
        get() = unfinishedTaskMap.size

    override val finishedTaskCount: Int
        get() = finishedTaskIdSet.size

    init {

        if ((File(finishedTaskIdJsonPath).exists() || File(unfinishedTaskListJsonPath).exists()) && !File(
                writeSuccessFilePath
            ).exists()
        ) {
            throw RuntimeException("读取文件出错，有文件无锁")
        }

        if (File(finishedTaskIdJsonPath).exists()) {
            val start = System.currentTimeMillis()
            val array = Gson().fromJson(
                FileUtils.readFileToString(File(finishedTaskIdJsonPath), "utf-8"),
                Array<String>::class.java
            )
            finishedTaskIdSet.addAll(array)
            val end = System.currentTimeMillis()
            logger.info("读取已完成任务，已完成${finishedTaskIdSet.size}任务，读取耗时:${end - start}ms")
        }
        if (File(unfinishedTaskListJsonPath).exists()) {
            val start = System.currentTimeMillis()
            val unfinishedTask = Gson().fromJson<Array<YashaDbModal>>(
                FileUtils.readFileToString(
                    File(unfinishedTaskListJsonPath),
                    "utf-8"
                ), Array<YashaDbModal>::class.java
            )
            for (yashaDbModal in unfinishedTask) {
                unfinishedTaskMap[yashaDbModal.taskIdentifier] = yashaDbModal
            }
            taskStack.addAll(unfinishedTask)
            val end = System.currentTimeMillis()
            logger.info("读取未完成任务，未完成${unfinishedTaskMap.size}任务，读取耗时:${end - start}ms")
        }
        GlobalScope.launch {
            while (true) {
                writeData()
                delay(60 * 1000)
            }
        }
    }

    override fun size(): Int {
        return taskStack.size
    }

    override fun addTask(yashaDBModal: YashaDbModal, force: Boolean): Boolean {
        if (force || (!finishedTaskIdSet.contains(yashaDBModal.taskIdentifier) && !unfinishedTaskMap.contains(
                yashaDBModal.taskIdentifier
            ))
        ) {
            taskStack.push(yashaDBModal)
            unfinishedTaskMap[yashaDBModal.taskIdentifier] = yashaDBModal
            if (force) {
                finishedTaskIdSet.remove(yashaDBModal.taskIdentifier)
            }
            return true
        }
        return false
    }

    override fun updateTask(yashaDBModal: YashaDbModal) {
        if (yashaDBModal.success) {
            speedRecorder.add(1)
            finishedTaskIdSet.add(yashaDBModal.taskIdentifier)
            unfinishedTaskMap.remove(yashaDBModal.taskIdentifier)
        }
    }

    override val lastOneMinSpeed: Double
        get() = speedRecorder.lastOneMinSpeed()

    override val totalSpeed: Double
        get() = speedRecorder.speed()

    @Synchronized
    override fun getNextTask(): YashaTask? {
        return if (!taskStack.empty()) {
            taskStack.pop().toYashaTask()
        } else {
            null
        }
    }

    override fun containsTask(taskIdentifier: String): Boolean {
        return unfinishedTaskMap.containsKey(taskIdentifier) || finishedTaskIdSet.contains(taskIdentifier)
    }

    @Synchronized
    fun writeData() {
        try {
            logger.info("开始保存数据")
            if (File("$unfinishedTaskListJsonPath.bk").exists()) {
                File("$unfinishedTaskListJsonPath.bk").delete()
            }
            if (File("$finishedTaskIdJsonPath.bk").exists()) {
                File("$finishedTaskIdJsonPath.bk").delete()
            }
            if (File(unfinishedTaskListJsonPath).exists()) {
                File(unfinishedTaskListJsonPath).renameTo(File("$unfinishedTaskListJsonPath.bk"))
            }
            if (File(finishedTaskIdJsonPath).exists()) {
                File(finishedTaskIdJsonPath).renameTo(File("$finishedTaskIdJsonPath.bk"))
            }
            File(writeSuccessFilePath).delete()
            while (true) {
                try {
                    val list = ArrayList(unfinishedTaskMap.values)
                    FileUtils.writeStringToFile(File(unfinishedTaskListJsonPath), Gson().toJson(list), "utf-8")
                    break
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            while (true) {
                try {
                    val set = HashSet(finishedTaskIdSet)
                    FileUtils.writeStringToFile(File(finishedTaskIdJsonPath), Gson().toJson(set), "utf-8")
                    break
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            File(writeSuccessFilePath).createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(0)
        }
    }
}

