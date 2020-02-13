package cn.gongyinan.yasha.task.classifier

import cn.gongyinan.yasha.task.YashaTask

interface ITaskClassifier {
    fun classifier(task: YashaTask): String?
}