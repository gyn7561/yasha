package cn.gongyinan.yasha.db

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.YashaTask
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.collections.HashMap

class SimpleMemoryTaskDb : ITaskDb {

    private val logger = LogManager.getLogger(SimpleMemoryTaskDb::class.java)

    private val taskIdSet = HashSet<String>()
    private val taskStack = Stack<YashaDbModal>()

    override fun size(): Int {
        return taskStack.size
    }

    override fun addTask(yashaDBModal: YashaDbModal, force: Boolean): Boolean {
        if (force) {
            taskIdSet.add(yashaDBModal.taskIdentifier)
            taskStack.push(yashaDBModal)
            logger.info("添加任务成功${yashaDBModal.toYashaTask()}")
            return true
        }
        if (!taskIdSet.contains(yashaDBModal.taskIdentifier)) {
            taskIdSet.add(yashaDBModal.taskIdentifier)
            taskStack.push(yashaDBModal)
            logger.info("添加任务成功${yashaDBModal.toYashaTask()}")
            return true
        }
        return false
    }

    override fun updateTask(yashaDBModal: YashaDbModal) {
        logger.info("更新任务数据${yashaDBModal.toYashaTask()}")
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
        return taskIdSet.contains(taskIdentifier)
    }

}