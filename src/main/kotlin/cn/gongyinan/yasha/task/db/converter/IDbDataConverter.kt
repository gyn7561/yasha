package cn.gongyinan.yasha.task.db.converter

import cn.gongyinan.yasha.task.YashaTask
import cn.gongyinan.yasha.task.db.modals.YashaDbModal

interface IDbDataConverter {

    fun toYashaDbModal(yashaTask: YashaTask): YashaDbModal

    fun toYashaTask(yashaDbModal: YashaDbModal): YashaTask


}