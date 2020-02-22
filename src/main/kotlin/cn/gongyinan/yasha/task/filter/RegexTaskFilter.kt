package cn.gongyinan.yasha.task.filter

import cn.gongyinan.yasha.task.YashaTask

class RegexTaskFilter(val regex: Regex) : ITaskFilter {

    override fun filter(task: YashaTask): Boolean {
        return regex.matches(task.uri.toString())
    }
}