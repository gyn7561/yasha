package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.converter.DefaultDbDataConverter
import cn.gongyinan.yasha.utils.SpeedRecorder
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnel
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.exitProcess
@Deprecated("待重写")
class BloomFilterFileTaskDb(private val filePath: String, val expectedInsertions: Int, val fpp: Double) :
        ITaskDb {

    override lateinit var yasha: Yasha

    lateinit var bloomFilter: BloomFilter<String>

    private var savedFinishedTaskCount = AtomicInteger(0)

    private val speedRecorder = SpeedRecorder()

    private val logger = LogManager.getLogger(BloomFilterFileTaskDb::class.java)
    private val unfinishedTaskMap = Collections.synchronizedMap(HashMap<String, YashaDbModal>())
    private val taskStack = Stack<YashaDbModal>()

    private val bloomFilterFilePath = "$filePath/finishedTask_${DigestUtils.md5Hex("$expectedInsertions$fpp")}.bloomfilter"
    private val bloomFilterCountFilePath = "$filePath/finishedTask_${DigestUtils.md5Hex("$expectedInsertions$fpp")}.bloomfilter.count"
    private val unfinishedTaskListJsonPath = "$filePath/unfinishedTaskList.json"

    private val writeSuccessFilePath = "$filePath/write.success"

    override val unfinishedTaskCount: Int
        get() = unfinishedTaskMap.size

    override val finishedTaskCount: Int
        get() = savedFinishedTaskCount.get()

    init {

        if ((File(bloomFilterFilePath).exists() || File(unfinishedTaskListJsonPath).exists() || File(bloomFilterCountFilePath).exists()) && !File(writeSuccessFilePath).exists()) {
            throw RuntimeException("读取文件出错，有文件无锁")
        }

        if (File(bloomFilterFilePath).exists()) {
            val start = System.currentTimeMillis()
            val fs = FileInputStream(File(bloomFilterFilePath))
            bloomFilter = BloomFilter.readFrom(fs, Funnel<String> { from, into ->
                into.putString(from, Charsets.UTF_8)
            })
            fs.close()

            if (File(bloomFilterCountFilePath).exists()) {
                savedFinishedTaskCount.addAndGet(FileUtils.readFileToString(File(bloomFilterCountFilePath), "utf-8").toInt())
            }
            val end = System.currentTimeMillis()
            logger.info("读取已完成任务，已完成${savedFinishedTaskCount}任务，读取耗时:${end - start}ms")
        } else {
            bloomFilter = BloomFilter.create(Funnel<String> { from, into ->
                into.putString(from, Charsets.UTF_8)
            }, expectedInsertions, fpp)
        }

        if (File(unfinishedTaskListJsonPath).exists()) {
            val start = System.currentTimeMillis()
            val unfinishedTask = Gson().fromJson<Array<YashaDbModal>>(FileUtils.readFileToString(File(unfinishedTaskListJsonPath), "utf-8"), Array<YashaDbModal>::class.java)
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

    override fun pushTask(yashaTask: YashaTask, force: Boolean, pushToStackBottom: Boolean, beforePushFunc: (YashaDbModal.() -> Unit)?): Boolean {
        val yashaDBModal = defaultDbDataConverter.toYashaDbModal(yashaTask)
        beforePushFunc?.invoke(yashaDBModal)
        if (force || (!bloomFilter.mightContain(yashaDBModal.taskIdentifier) && !unfinishedTaskMap.contains(yashaDBModal.taskIdentifier))) {
            if (pushToStackBottom) {
                taskStack.add(0, yashaDBModal)
            } else {
                taskStack.push(yashaDBModal)
            }

            unfinishedTaskMap[yashaDBModal.taskIdentifier] = yashaDBModal
            if (force && bloomFilter.mightContain(yashaDBModal.taskIdentifier)) {
                savedFinishedTaskCount.addAndGet(-1)
            }
            return true
        }
        return false
    }

    private val defaultDbDataConverter = DefaultDbDataConverter()

    private val downloadSpeedRecorder = SpeedRecorder()
    override fun downloadSpeed(): Double {
        return downloadSpeedRecorder.lastOneMinCount().toDouble() / 60
    }

    override fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit): YashaDbModal {

        val yashaDBModal = defaultDbDataConverter.toYashaDbModal(yashaTask)
        beforeUpdateFunc(yashaDBModal)

        if (yashaDBModal.success) {
            speedRecorder.add(1)
            downloadSpeedRecorder.add(yashaDBModal.responseBody?.size ?: 0)
            savedFinishedTaskCount.addAndGet(1)
            bloomFilter.put(yashaDBModal.taskIdentifier)
            unfinishedTaskMap.remove(yashaDBModal.taskIdentifier)
        }
        return yashaDBModal

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

    override fun isTaskFinished(taskIdentifier: String): Boolean {
        return unfinishedTaskMap.containsKey(taskIdentifier) || bloomFilter.mightContain(taskIdentifier)
    }

    @Synchronized
    fun writeData() {

        try {
            logger.info("开始保存数据")
            if (File("$unfinishedTaskListJsonPath.bk").exists()) {
                File("$unfinishedTaskListJsonPath.bk").delete()
            }
            if (File("$bloomFilterCountFilePath.bk").exists()) {
                File("$bloomFilterCountFilePath.bk").delete()
            }
            if (File("$bloomFilterFilePath.bk").exists()) {
                File("$bloomFilterFilePath.bk").delete()
            }

            if (File(bloomFilterFilePath).exists()) {
                File(bloomFilterFilePath).renameTo(File("$bloomFilterFilePath.bk"))
            }
            if (File(bloomFilterCountFilePath).exists()) {
                File(bloomFilterCountFilePath).renameTo(File("$bloomFilterCountFilePath.bk"))
            }
            if (File(unfinishedTaskListJsonPath).exists()) {
                File(unfinishedTaskListJsonPath).renameTo(File("$unfinishedTaskListJsonPath.bk"))
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
                    val bs = ByteArrayOutputStream()
                    bloomFilter.writeTo(bs)
                    FileUtils.writeByteArrayToFile(File(bloomFilterFilePath), bs.toByteArray())
                    bs.close()

                    break
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            while (true) {
                try {
                    FileUtils.writeStringToFile(File(bloomFilterCountFilePath), savedFinishedTaskCount.toString(), "utf-8")
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
