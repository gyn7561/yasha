package cn.gongyinan.yasha.task.filter

import cn.gongyinan.yasha.task.YashaTask
import java.util.logging.Filter

class CompositeTaskFilter(private val filters: Array<ITaskFilter>) : ITaskFilter {
    override fun filter(task: YashaTask): Boolean {
        return filters.all { f -> f.filter(task) }
    }
}