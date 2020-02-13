package cn.gongyinan.yasha.task.classifier

import cn.gongyinan.yasha.task.YashaTask

class RegexClassifier(val regex: Regex, private val tag: String) : ITaskClassifier {
    override fun classifier(task: YashaTask): String? {
        return if (regex.matches(task.uri.toString())) {
            tag
        } else {
            null
        }
    }
}