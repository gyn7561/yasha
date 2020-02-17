package cn.gongyinan.yasha.task.filter

import cn.gongyinan.yasha.task.YashaTask

class TaskFilter(private val func: (YashaTask.() -> Boolean)) : ITaskFilter {

    override fun filter(task: YashaTask): Boolean {
        return func.invoke(task)
    }

}