package com.ccnio.utils

import org.gradle.api.Project

/**
 * Created by jianfeng.li on 21-2-9.
 */
object Log {
    private var enable: Boolean = true

    fun logEnable(enable: Boolean) {
        this.enable = enable
    }

    fun d(tag: String, info: String) {
        if (enable) {
            println("$tag d>>> $info")
        }
    }

}