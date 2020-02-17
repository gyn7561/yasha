package cn.gongyinan.yasha.task.db

import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.converter.DefaultDbDataConverter
import cn.gongyinan.yasha.task.db.modals.YashaDbModal
import org.apache.logging.log4j.LogManager
import java.util.*

class SimpleMemoryTaskDb : ITaskDb {

    override lateinit var yasha: Yasha
    private val logger = LogManager.getLogger(SimpleMemoryTaskDb::class.java)

    private val taskIdSet = HashSet<String>()
    private val taskStack = Stack<YashaDbModal>()

    override fun size(): Int {
        return taskStack.size
    }

    override fun pushTask(yashaTask: YashaTask, force: Boolean, pushToStackBottom: Boolean, beforePushFunc: (YashaDbModal.() -> Unit)?): ITaskDb.PushTaskResult {
        val yashaDBModal = defaultDbDataConverter.toYashaDbModal(yashaTask)
        beforePushFunc?.invoke(yashaDBModal)

        if (force) {
            taskIdSet.add(yashaDBModal.taskIdentifier)
            taskStack.push(yashaDBModal)
            logger.info("添加任务成功${yashaDBModal.toYashaTask()}")
            return ITaskDb.PushTaskResult(yashaDBModal,true)
        }
        if (!taskIdSet.contains(yashaDBModal.taskIdentifier)) {
            taskIdSet.add(yashaDBModal.taskIdentifier)
            taskStack.push(yashaDBModal)
            logger.info("添加任务成功${yashaDBModal.toYashaTask()}")
            return ITaskDb.PushTaskResult(yashaDBModal,true)
        }
        return ITaskDb.PushTaskResult(yashaDBModal,false)
    }

    private val defaultDbDataConverter = DefaultDbDataConverter()

    override fun updateTask(yashaTask: YashaTask, beforeUpdateFunc: YashaDbModal.() -> Unit): YashaDbModal {
        val yashaDBModal = defaultDbDataConverter.toYashaDbModal(yashaTask)
        beforeUpdateFunc(yashaDBModal)
        logger.info("更新任务数据${yashaDBModal.toYashaTask()}")
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
        return taskIdSet.contains(taskIdentifier)
    }

}