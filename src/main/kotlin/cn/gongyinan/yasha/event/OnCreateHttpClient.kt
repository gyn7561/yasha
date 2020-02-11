package cn.gongyinan.yasha.event

annotation class OnCreateHttpClient(val value: Array<String> = ["[\\w\\W]*"], val order:Int = 1)