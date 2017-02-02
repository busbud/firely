/*
 * Copyright 2017 Busbud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.busbud.android.firely

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Firely plugin that plugs a JavaGeneratingTask in the variants of the application and generate the FirelyConfig.java file
 */
class FirelyPlugin implements Plugin<Project> {

    private static final String TASK_NAME = "firely"
    private static final String FILE_NAME = "firely-config.json"

    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        // Add generated source
        android.applicationVariants.all { BaseVariant variant ->
            File sourceFolder = new File(project.buildDir, "/generated/source/firely/${variant.dirName}")
            def configFile = new File(FILE_NAME)
            if (!configFile.exists()) {
                configFile = new File("./${project.name}/${FILE_NAME}")
            }
            if (!configFile.exists()) {
                println("Unable to find ${FILE_NAME} in your project")
                return
            }
            def firelyTask = project.task(type: FirelyTask, "${TASK_NAME}${variant.name.capitalize()}") {
                inputFile = configFile
                outputDir = sourceFolder
            }
            firelyTask.outputs.upToDateWhen { false }
            variant.registerJavaGeneratingTask(firelyTask, sourceFolder)
        }
    }
}
