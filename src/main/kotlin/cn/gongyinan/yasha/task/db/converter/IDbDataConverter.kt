package cn.gongyinan.yasha.task.db.converter

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.task.YashaTask

interface IDbDataConverter {

    fun toYashaDbModal(yashaTask: YashaTask): YashaDbModal

    fun toYashaTask(yashaDbModal: YashaDbModal): YashaTask


}