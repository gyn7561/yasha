package cn.gongyinan.yasha.task.classifier

import cn.gongyinan.yasha.task.YashaTask

class UrlPrefixClassifier(private val prefix: String, private val tag: String) : ITaskClassifier {
    override fun classifier(task: YashaTask): String? {
        return if (task.uri.toString().startsWith(prefix)) {
            tag
        } else {
            null
        }
    }
}