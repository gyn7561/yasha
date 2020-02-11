package cn.gongyinan.yasha.db

import cn.gongyinan.yasha.YashaDbModal
import cn.gongyinan.yasha.YashaTask

class HttpBasedDb : ITaskDb {



    override fun size(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addTask(yashaDBModal: YashaDbModal, force: Boolean): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateTask(yashaDBModal: YashaDbModal) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNextTask(): YashaTask? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsTask(taskIdentifier: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}