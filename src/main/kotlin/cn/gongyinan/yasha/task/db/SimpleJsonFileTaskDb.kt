package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.converter.DefaultDbDataConverter
import cn.gongyinan.yasha.task.db.converter.IDbDataConverter
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.system.exitProcess

class SimpleJsonFileTaskDb(private val filePath: String, converter: IDbDataConverter = DefaultDbDataConverter()) : AbstractMemoryTaskDb(converter) {

    private val logger = LogManager.getLogger(SimpleJsonFileTaskDb::class.java)
    private val finishedTaskIdSet = Collections.synchronizedSet(HashSet<String>())

    private val finishedTaskIdJsonPath = "$filePath/finishedTaskIdSet.json"
    private val writeSuccessFilePath = "$filePath/write.success"
    private val unfinishedTaskListJsonPath = "$filePath/unfinishedTaskList.json"

    override val unfinishedTaskCount: Int
        get() = unfinishedTaskMap.size

    override val finishedTaskCount: Int
        get() = finishedTaskIdSet.size

    init {

        if ((File(finishedTaskIdJsonPath).exists() || File(unfinishedTaskListJsonPath).exists() ||
                        File("$finishedTaskIdJsonPath.bk").exists() || File("$unfinishedTaskListJsonPath.bk").exists()) && !File(
                        writeSuccessFilePath
                ).exists()
        ) {
            throw RuntimeException("上次写入出错，请手工恢复")
        }

        File(writeSuccessFilePath).delete()

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
                taskStack.push(yashaDbModal)
            }
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


    override fun isTaskFinished(taskIdentifier: String): Boolean {
        return finishedTaskIdSet.contains(taskIdentifier)
    }

    override fun canPush(yashaDbModal: YashaDbModal): Boolean {
        return !finishedTaskIdSet.contains(yashaDbModal.taskIdentifier) && !unfinishedTaskMap.containsKey(yashaDbModal.taskIdentifier)
    }


    override fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit): YashaDbModal {
        val dbModal = super.updateTask(yashaTask, beforeUpdateFunc)
        if (dbModal.success) {
            finishedTaskIdSet.add(dbModal.taskIdentifier)
        }
        return dbModal
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