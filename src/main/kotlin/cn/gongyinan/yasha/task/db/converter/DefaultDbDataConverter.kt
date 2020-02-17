package cn.gongyinan.yasha.task.db.converter

import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.modals.YashaDbModal
import java.net.URI

class DefaultDbDataConverter : IDbDataConverter {

    override fun toYashaDbModal(yashaTask: YashaTask): YashaDbModal {
        val dbModal = YashaDbModal(
                yashaTask.taskIdentifier,
                taskDepth = yashaTask.taskDepth,
                requestUrl = yashaTask.uri.toString(),
                taskCommand = yashaTask.taskCommand,
                requestBody = yashaTask.requestBody,
                ready = true,
                requestMethod = yashaTask.method,
                parentTaskIdentifier = yashaTask.parentTaskIdentifier,
                extraData = yashaTask.extraData,
                createTime = yashaTask.createTime,
                taskClass = yashaTask.javaClass.name
        )
        dbModal.requestHeaders = yashaTask.headers
        return dbModal
    }

    override fun toYashaTask(yashaDbModal: YashaDbModal): YashaTask {
        return YashaTask(
                URI(yashaDbModal.requestUrl),
                yashaDbModal.taskDepth,
                yashaDbModal.requestBody,
                yashaDbModal.requestMethod,
                yashaDbModal.requestHeaders,
                yashaDbModal.extraData,
                yashaDbModal.parentTaskIdentifier,
                yashaDbModal.taskIdentifier,
                yashaDbModal.taskCommand,
                yashaDbModal.createTime
        )
    }

}