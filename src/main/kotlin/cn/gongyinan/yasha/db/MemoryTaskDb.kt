package cn.gongyinan.yasha.db

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.YashaTask
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.collections.HashMap

class MemoryTaskDb : ITaskDb {

    private val logger = LogManager.getLogger(MemoryTaskDb::class.java)

    private val taskMap = HashMap<String, YashaDbModal>()
    private val taskStack = Stack<YashaDbModal>()

    override fun size(): Int {
        return taskStack.size
    }

    override fun addTask(yashaDBModal: YashaDbModal, force: Boolean): Boolean {
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

    override fun updateTask(yashaDBModal: YashaDbModal) {
        logger.info("更新任务数据${yashaDBModal.toYashaTask()}")
        taskMap[yashaDBModal.taskIdentifier] = yashaDBModal
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

    override fun containsTask(taskIdentifier: String): Boolean {
        return taskMap.containsKey(taskIdentifier)
    }

}