package cn.gongyinan.yasha.task.classifier

import cn.gongyinan.yasha.task.YashaTask

class UrlPrefixAndSuffixClassifier(private val prefix: String, private val suffix: String, private val tag: String) : ITaskClassifier {
    override fun classifier(task: YashaTask): String? {
        return if (task.uri.toString().startsWith(prefix) && task.uri.toString().endsWith(suffix)) {
            tag
        } else {
            null
        }
    }
}