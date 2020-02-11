package cn.gongyinan.yasha.event

annotation class OnCheckResponse(val value: Array<String> = ["[\\w\\W]*"], val order:Int = 1)