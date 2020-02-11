package cn.gongyinan.yasha.event

annotation class OnRequest(val value: Array<String> = ["[\\w\\W]*"], val order:Int = 1)