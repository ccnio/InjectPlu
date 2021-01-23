package com.ccnio.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger

class Log {
    private static Logger logger

    static void make(Project project) {
        logger = project.getLogger()
    }

    static void d(String info) {
        if (null != info && null != logger) {
            logger.debug("InjectPlu >>> " + info)
            println("InjectPlu d>>> " + info)
        }
    }

    static void i(String info) {
        if (null != info && null != logger) {
            logger.info("InjectPlu >>> " + info)
            println("InjectPlu i>>> " + info)
        }
    }

    static void e(String error) {
        if (null != error && null != logger) {
            logger.error("InjectPlu >>> " + error)
            println("InjectPlu e>>> " + error)
        }
    }

    static void w(String warning) {
        if (null != warning && null != logger) {
            logger.warn("InjectPlu w>>> " + warning)
            println("InjectPlu w>>> " + warning)
        }
    }
}