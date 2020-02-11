package cn.gongyinan.yasha.event

annotation class OnCreateHttpHeader(val value: Array<String> = ["[\\w\\W]*"], val order:Int = 1)