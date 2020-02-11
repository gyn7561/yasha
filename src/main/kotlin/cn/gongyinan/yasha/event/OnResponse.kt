package cn.gongyinan.yasha.event

annotation class OnResponse(val value: Array<String> = ["[\\w\\W]*"], val order:Int = 1)