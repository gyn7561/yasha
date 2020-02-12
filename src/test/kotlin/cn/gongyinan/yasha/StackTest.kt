package cn.gongyinan.yasha

import com.alibaba.fastjson.JSON
import org.junit.jupiter.api.Test
import java.util.*

object StackTest {
    @Test
    fun test() {
        val stack = Stack<String>()
        println(JSON.toJSONString(stack))
        stack.push("0")
        println(JSON.toJSONString(stack))
        stack.push("1")
        println(JSON.toJSONString(stack))
        stack.add(0, "2")
        println(JSON.toJSONString(stack))
        stack.push("3")
        println(JSON.toJSONString(stack))


        println(stack.pop())
        println(stack.peek())
        println(stack.pop())
        println(stack.peek())

    }
}