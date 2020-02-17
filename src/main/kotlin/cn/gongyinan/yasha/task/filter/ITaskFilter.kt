package cn.gongyinan.yasha.task.filter

import cn.gongyinan.yasha.task.YashaTask

interface ITaskFilter {

    fun filter(task: YashaTask): Boolean

}