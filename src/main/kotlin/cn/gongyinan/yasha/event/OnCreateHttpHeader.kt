package cn.gongyinan.yasha.event
@Deprecated("弃用")
annotation class OnCreateHttpHeader(val value: Array<String> = ["[\\w\\W]*"], val order:Int = 1)