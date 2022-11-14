package cn.devin.jiaguplugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by devin on 2021/12/31.
 */

public class JiaguPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        //获取配置中的参数（对应app下build.gradle中的 jiagu{}）
//        System.out.println("1234")
        val jiaguParams = project.extensions.create("jiagu",JiaguParams::class.java)
        //读取完配置后的监听
        project.afterEvaluate {
            //获取android{}中的配置
            val appExtension = project.extensions.getByType(AppExtension::class.java)
            //读取applicationVariants
            appExtension.applicationVariants.all { applicationVariant->
                applicationVariant.outputs.all { output->
                    //动态创建output名字相关的任务（比如这里默认有debug和release，如果有多渠道包的话还会有更多），最后的两个参数是JiaguTask的构造函数需要的
                    println(output.outputFile.absolutePath)
                    println(output.name)
                    project.tasks.create("jiagu" + output.name,JiaguTask::class.java,output.outputFile, jiaguParams)
                }
            }
        }
    }
}