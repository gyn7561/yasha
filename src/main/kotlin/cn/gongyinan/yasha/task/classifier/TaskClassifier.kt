package cn.gongyinan.yasha.task.classifier

import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.filter.ITaskFilter
import cn.gongyinan.yasha.task.filter.TaskFilter

class TaskClassifier(private val taskFilter: ITaskFilter, private val tag: String) : ITaskClassifier {

    override fun classifier(task: YashaTask): String? {
        return if (taskFilter.filter(task)) {
            tag
        } else {
            null
        }
    }

}