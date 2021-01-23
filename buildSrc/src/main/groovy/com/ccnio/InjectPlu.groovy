package com.ccnio

import com.ccnio.utils.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppExtension


class InjectPlu implements Plugin<Project> {
    @Override
    void apply(Project project) {
        Log.make(project)
        Log.d("apply project:${project.name}")
        def app = project.extensions.getByType(AppExtension)
        def transformImpl = new PluTransform(project)
        app.registerTransform(transformImpl)
    }
}