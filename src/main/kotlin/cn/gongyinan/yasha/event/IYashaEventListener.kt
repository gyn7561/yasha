package cn.gongyinan.yasha.event

import cn.gongyinan.yasha.FetchResult
import cn.gongyinan.yasha.Yasha
import cn.gongyinan.yasha.YashaConfig
import cn.gongyinan.yasha.YashaTask
import okhttp3.OkHttpClient

interface IYashaEventListener {

    var yashaConfig: YashaConfig

    var yasha: Yasha

    fun onResponse(fetchResult: FetchResult)

    fun onRequest(yashaTask: YashaTask)

    fun onCreateHttpClient(yashaTask: YashaTask): OkHttpClient

    fun onCheckResponse(fetchResult: FetchResult): Boolean

    fun onTaskFinder(fetchResult: FetchResult): List<YashaTask>

    fun onError(yashaTask: YashaTask, e: Throwable)
}