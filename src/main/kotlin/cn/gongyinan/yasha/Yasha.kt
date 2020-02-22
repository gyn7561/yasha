package cn.gongyinan.yasha

import cn.gongyinan.yasha.core.Engine
import cn.gongyinan.yasha.task.db.ITaskDb
import cn.gongyinan.yasha.task.YashaTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import java.net.URI
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class Yasha(val yashaConfig: YashaConfig) {

    private val taskDb: ITaskDb by lazy {
        yashaConfig.taskDb.init()
        yashaConfig.taskDb
    }

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
        taskDb.yasha = this
        yashaConfig.listener.yasha = this
        for (engine in engines) {
            runningMap[engine] = true
        }
    }

    fun pushTask(task: YashaTask, force: Boolean = false, pushToStackBottom: Boolean = false): ITaskDb.PushTaskResult {
        return taskDb.pushTask(task, force, pushToStackBottom)
    }

    fun pushGetTask(url: String, force: Boolean = false): ITaskDb.PushTaskResult {
        val getTask = this.yashaConfig.listener.onCreateDefaultGetTask(URI(url), 0, null)
        return taskDb.pushTask(getTask, force)
    }


    init {
        for (url in yashaConfig.initUrl) {
            pushGetTask(url)
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
            result.subTasks = result.subTasks.map { subTask ->
                yashaConfig.listener.beforePushTask(subTask)
            }
            for (subTask in result.subTasks) {
                pushTask(subTask)
            }
            yashaConfig.listener.onResponse(result)
            taskDb.updateTask(task) {
                ready = false
                success = true
                updateTime = System.currentTimeMillis()
                subTaskCommands = result.subTasks.map { yashaTask -> yashaTask.taskIdentifier }.toTypedArray()
                responseUrl = result.responseUri.toString()
                responseHeaders = result.responseHeaders.toList()
                responseCode = result.responseCode
                responseBody = result.zippedData ?: result.rawData
                contentType = result.contentType
            }
        }, onFailure = { _, e ->
            taskDb.updateTask(task) {
                success = false
                ready = true // 下次再来
                updateTime = System.currentTimeMillis()
            }
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