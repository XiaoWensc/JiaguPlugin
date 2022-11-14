package cn.devin.jiaguplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @Author devin
 * @Date 2022/11/14
 * @Description
 */

class TestPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        println("start apply")
        project.task("testDemo").doLast {
            println("start task")
        }
        println("end")
    }
}