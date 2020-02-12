package cn.gongyinan.yasha.task.classifier

import cn.gongyinan.yasha.task.YashaTask

class UrlSuffixClassifier(private val suffix: String, private val tag: String) : ITaskClassifier {
    override fun classifier(task: YashaTask): String? {
        return if (task.uri.toString().endsWith(suffix)) {
            tag
        } else {
            null
        }
    }
}