package cn.gongyinan.yasha

import cn.gongyinan.yasha.core.Engine
import cn.gongyinan.yasha.db.ITaskDb
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import java.net.URI

class Yasha(val yashaConfig: YashaConfig) {

    private val taskDb: ITaskDb = yashaConfig.taskDb

    private val logger = LogManager.getLogger(Yasha::class.java)

    private val engines = (1..yashaConfig.threadNum).map { i ->
        Engine(
            yashaConfig,
            "ENGINE$i"
        )
    }

    private val runningMap = HashMap<Engine, Boolean>()

    fun runningTask(): HashMap<String, YashaTask?> {
        val result = HashMap<String, YashaTask?>()
        for (key in runningMap.keys) {
            val name = key.toString()
            result[name] = key.runningTask
        }
        return result
    }

    init {
        yashaConfig.listener.yasha = this
        for (engine in engines) {
            runningMap[engine] = true
        }
    }

    fun pushTask(task: YashaTask, force: Boolean = false): Boolean {
        if (task.taskDepth > yashaConfig.maxDepth) {
            return false
        }
        return taskDb.addTask(task.toDbModal(), force)
    }

    fun pushGetTask(url: String, force: Boolean = false): Boolean {
        return taskDb.addTask(YashaGetTask(URI(url)).toDbModal(), force)
    }

    init {
        for (url in yashaConfig.initUrl) {
            pushTask(YashaGetTask(URI(url), 0))
        }
    }

    private fun startNextTask(engine: Engine, delay: Long = 0) {
        GlobalScope.launch {
            delay(delay)
            val yashaTask: YashaTask? = taskDb.getNextTask()
            if (yashaTask == null && !runningMap.values.any { running -> running }) {//当全部进入不运行状态
                logger.info("$engine 结束任务循环")
                return@launch
            }
            if (yashaTask != null) {
                runningMap[engine] = true
                startTask(engine, yashaTask)
                return@launch
            } else {
                runningMap[engine] = false
                startNextTask(engine, 1000)
            }
        }
    }

    private fun startTask(engine: Engine, task: YashaTask) {
        engine.fetchPage(task, onSuccess = { result ->
            for (subTask in result.subTasks) {
                pushTask(subTask)
            }
            val dbModal = task.toDbModal()
            dbModal.ready = false
            dbModal.success = true
            dbModal.updateTime = System.currentTimeMillis()
            dbModal.subTaskCommands = result.subTasks.filter { task -> !taskDb.containsTask(task.taskIdentifier) }
                .map { yashaTask -> yashaTask.taskCommand }.toTypedArray()
            dbModal.responseUrl = result.responseUri.toString()
            dbModal.responseHeaders =
                result.response.headers.names().map { name -> arrayOf(name, result.response.header(name)!!) }.toList()
            dbModal.responseCode = result.response.code
            dbModal.responseBody = result.rawData
            dbModal.contentType = result.contentType
            taskDb.updateTask(dbModal)
            yashaConfig.listener.onResponse(result)
        }, onFailure = { _, e ->
            val dbModal = task.toDbModal()
            dbModal.success = false
            dbModal.ready = false
            dbModal.updateTime = System.currentTimeMillis()
            taskDb.updateTask(dbModal)
            yashaConfig.listener.onError(task, e)
        }, onFinal = { _ ->
            startNextTask(engine, yashaConfig.intervalInMs)
        })
    }


    fun start() {
        for (engine in engines) {
            startNextTask(engine)
        }
    }

    fun waitFinish() {
        while (runningMap.values.any { running -> running }) {
            Thread.sleep(10)
        }
    }

}