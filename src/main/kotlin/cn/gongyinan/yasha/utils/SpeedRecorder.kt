package cn.gongyinan.yasha.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger


class SpeedRecorder {


    private var recordHs = HashMap<Long, Int>()

    private var startTs = System.currentTimeMillis()

    private val counter = AtomicInteger(0)
    fun reset() {
        startTs = System.currentTimeMillis()
        recordHs = HashMap()
    }

    @Synchronized
    fun add(num: Int) {
        val sec = System.currentTimeMillis() / 1000
        recordHs[sec] = (recordHs[sec] ?: 0) + num
        counter.addAndGet(num)
    }

    fun total(): Int {
        return counter.get()
    }

    fun speed(): Double {
        val ms = (System.currentTimeMillis() - startTs).toDouble() + 1
        return counter.get() / ms * 1000
    }

    fun lastOneMinCount(): Int {
        val current = System.currentTimeMillis() / 1000
        var total = 0
        for (i in 1..60) {
            total += (recordHs[current - i] ?: 0)
        }
        return total
    }

    fun lastOneMinSpeed(): Double {
        return lastOneMinCount().toDouble() / 60
    }

    fun speedString(): String {
        val sp = speed()
        val speed = (sp * 10000).toInt()
        return "${speed / 10000.toDouble()}/S"
    }

    init {
        GlobalScope.launch {
            delay(60000)
            try {
                val current = System.currentTimeMillis() / 1000
                val removeKeys = recordHs.keys.filter { k -> k < (current - 100) }
                for (removeKey in removeKeys) {
                    recordHs.remove(removeKey)
                }
            } catch (e: Throwable) {

            }
        }
    }
}