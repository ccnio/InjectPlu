package com.ccnio.plu

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.ccnio.utils.Log
import groovy.util.XmlSlurper
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by jianfeng.li on 21-2-9.
 */
private const val TAG = "DemoPlu"

class DemoPlu : Plugin<Project> {
    override fun apply(project: Project) {
        Log.d(TAG, "***********${project.name}************")

        /**
        AppPlugin: id 'com.android.application'
        LibraryPlugin: id 'com.android.library'
        AppExtension/LibraryExtension: build.gradle 里的 android {}
        Variant = ProductFlavor x BuildType
        variant.sourceSets[0].manifestFile
         */
        project.plugins.all { plugin ->
            when (plugin) {
                is AppPlugin -> {
                    val appExtension = project.extensions.getByType(AppExtension::class.java)
                    Log.d(TAG, "plugin: AppPlugin; appExtension = $appExtension")
                    configureR2Generation(project, appExtension.applicationVariants)
                }
                is LibraryPlugin -> {
                    val libraryExtension =
                        project.extensions.getByType(LibraryExtension::class.java)
                    Log.d(TAG, "plugin: LibraryPlugin; libExtension = $libraryExtension")
                    configureR2Generation(project, libraryExtension.libraryVariants)
                }
                else -> {
//                    Log.d(TAG, "plugin: other = $plugin")
                }
            }
        }
//        > Configure project :app
//        emoPlu d>>> start***********************
//        DemoPlu d>>> plugin: AppPlugin; appExtension = extension 'android'

//        只在app module使用时不会有如下library的打印
//        > Configure project :moduel-library
//        DemoPlu d>>> start***********************
//        DemoPlu d>>> plugin: LibraryPlugin; libExtension = extension 'android'
    }

    private fun configureR2Generation(
        project: Project,
        variants: DomainObjectSet<out BaseVariant>
    ) {
        variants.all { variant ->
            val outputDir = project.buildDir.resolve(
                "generated/source/r2/${variant.dirName}"
            )
//            > Configure project :app
//            DemoPlu d>>> configureR2Generation: outputDir = /home/lijf/code/InjectPlu/app/build/generated/source/r2/debug
//            DemoPlu d>>> configureR2Generation: outputDir = /home/lijf/code/InjectPlu/app/build/generated/source/r2/release
//
//            > Configure project :moduel-library
//            DemoPlu d>>> configureR2Generation: outputDir = /home/lijf/code/InjectPlu/moduel-library/build/generated/source/r2/debug
//            DemoPlu d>>> configureR2Generation: outputDir = /home/lijf/code/InjectPlu/moduel-library/build/generated/source/r2/release
            Log.d(TAG, "configureR2Generation: outputDir = $outputDir")
//
            val rPackage = getPackageName(variant)
//            val once = AtomicBoolean()
//            variant.outputs.all { output ->
//                // Though there might be multiple outputs, their R files are all the same. Thus, we only
//                // need to configure the task once with the R.java input and action.
//                if (once.compareAndSet(false, true)) {
//                    val processResources = output.processResourcesProvider.get() // TODO lazy
//
//                    // TODO: switch to better API once exists in AGP (https://issuetracker.google.com/118668005)
//                    val rFile =
//                        project.files(
//                            when (processResources) {
//                                is GenerateLibraryRFileTask -> processResources.textSymbolOutputFile
//                                is LinkApplicationAndroidResourcesTask -> processResources.textSymbolOutputFile
//                                else -> throw RuntimeException(
//                                    "Minimum supported Android Gradle Plugin is 3.3.0")
//                            })
//                            .builtBy(processResources)
//                    val generate = project.tasks.create("generate${variant.name.capitalize()}R2", R2Generator::class.java) {
//                        it.outputDir = outputDir
//                        it.rFile = rFile
//                        it.packageName = rPackage
//                        it.className = "R2"
//                    }
//                    variant.registerJavaGeneratingTask(generate, outputDir)
//                }
//            }
        }
    }

    // Parse the variant's main manifest file in order to get the package id which is used to create
    // R.java in the right place.
    private fun getPackageName(variant: BaseVariant): String {
        val slurper = XmlSlurper(false, false)
        val list = variant.sourceSets.map { it.manifestFile }
        Log.d(TAG, "getPackageName: mainfestFiles = $list")

        // According to the documentation, the earlier files in the list are meant to be overridden by the later ones.
        // So the first file in the sourceSets list should be main.
        val result = slurper.parse(list[0])
        return result.getProperty("@package").toString()
    }
}