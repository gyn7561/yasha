package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.converter.DefaultDbDataConverter
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.collections.HashMap

class MemoryTaskDb : ITaskDb {

    override lateinit var yasha: Yasha
    private val logger = LogManager.getLogger(MemoryTaskDb::class.java)

    private val taskMap = HashMap<String, YashaDbModal>()
    private val taskStack = Stack<YashaDbModal>()


    override fun pushTask(yashaTask: YashaTask, force: Boolean, pushToStackBottom: Boolean, beforePushFunc: (YashaDbModal.() -> Unit)?): Boolean {
        val yashaDBModal = defaultDbDataConverter.toYashaDbModal(yashaTask)
        beforePushFunc?.invoke(yashaDBModal)
        if (force) {
            taskMap[yashaDBModal.taskIdentifier] = yashaDBModal
            taskStack.push(yashaDBModal)
            logger.info("添加任务成功${yashaDBModal.toYashaTask()}")
            return true
        }
        if (!taskMap.contains(yashaDBModal.taskIdentifier)) {
            taskMap[yashaDBModal.taskIdentifier] = yashaDBModal
            taskStack.push(yashaDBModal)
            logger.info("添加任务成功${yashaDBModal.toYashaTask()}")
            return true
        }
        return false
    }

    private val defaultDbDataConverter = DefaultDbDataConverter()

    override fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit) :YashaDbModal{
        val yashaDBModal = defaultDbDataConverter.toYashaDbModal(yashaTask)
        beforeUpdateFunc(yashaDBModal)
        logger.info("更新任务数据${yashaDBModal.toYashaTask()}")
        taskMap[yashaDBModal.taskIdentifier] = yashaDBModal
        return yashaDBModal
    }

    @Synchronized
    override fun getNextTask(): YashaTask? {
        val task = if (!taskStack.empty()) {
            taskStack.pop().toYashaTask()
        } else {
            null
        }
        logger.info("获取任务 -> $task")
        return task
    }

    override fun isTaskFinished(taskIdentifier: String): Boolean {
        return taskMap.containsKey(taskIdentifier)
    }

}