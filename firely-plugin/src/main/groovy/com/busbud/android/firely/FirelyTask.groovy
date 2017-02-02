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

import com.sun.codemodel.*
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import static com.sun.codemodel.JExpr._this
import static com.sun.codemodel.JExpr.lit
import static com.sun.codemodel.JMod.*

/**
 * Task that generates the FirelyConfig.java file in the outputDir based on the inputFile
 */
class FirelyTask extends DefaultTask {

    @InputFile
    File inputFile

    @OutputDirectory
    File outputDir

    @TaskAction
    def createFirelyConfig() {
        def json = new JsonSlurper().parseText(inputFile.text)

        println("Parsing ${inputFile.path}")

        def codeModel = new JCodeModel()

        // General classes and dependencies
        def packageDef = codeModel._package("com.busbud.android.firely")
        def firelyItem = codeModel.directClass("com.busbud.android.firely.IFirelyItem")
        def firelyConfig = codeModel.directClass("com.busbud.android.firely.IFirelyConfig")

        // Main class
        def configClass = packageDef._class(PUBLIC | FINAL, "FirelyConfig", ClassType.CLASS)
        configClass.javadoc().add("Generated class - Do not edit")
        configClass._implements(firelyConfig)

        // allValues methods
        def allValuesArray = JExpr.newArray(firelyItem)

        // Create enum
        for (String key : json.keySet()) {
            if(name && !name.isAllWhitespace()) {
                addEnumToJClass(configClass, firelyItem, convertUnderscoreToJava(key), json.get(key), allValuesArray)
            }
        }

        // Add allValues methods
        def allValuesMethod = configClass.method(PUBLIC, firelyItem.array(), "allValues")
        allValuesMethod.annotate(Override.class)
        allValuesMethod.body()._return(allValuesArray)

        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException('Could not create directory: ' + outputDir)
        }
        println("Writing class in ${outputDir.path} :")
        codeModel.build(outputDir)
    }

    private static def convertUnderscoreToJava(String name) {
        if (name.size() > 0) {
            name = name[0].toUpperCase() + (name.size() > 1 ? name.substring(1) : '')
        }
        return name.replaceAll(/_\w/){ it[1].toUpperCase() }
    }

    private static def addEnumToJClass(JClass configClass, JClass firelyItem,
                        def enumName, Object jsonArray, JArray allValues) {
        if (jsonArray.size() == 0) {
            // Do nothing
            return;
        }
        def enumClass = configClass._class(PUBLIC, enumName, ClassType.ENUM)
        enumClass._implements(firelyItem)

        def name = enumClass.field(PRIVATE | FINAL, String.class, "mName")
        def defaultValue = enumClass.field(PRIVATE | FINAL, java.lang.Object.class, "mDefault")

        def constructor = enumClass.constructor(PRIVATE)
        def param1 = constructor.param(String.class, "name")
        def param2 = constructor.param(java.lang.Object.class, "defaultValue")

        def body = constructor.body()
        body.assign(_this().ref(name), param1)
        body.assign(_this().ref(defaultValue), param2)

        def getNameMethod = enumClass.method(PUBLIC, String.class, "getName")
        getNameMethod.annotate(Override.class)
        getNameMethod.body()._return(name)

        def getDefaultMethod = enumClass.method(PUBLIC, java.lang.Object.class, "getDefault")
        getDefaultMethod.annotate(Override.class)
        getDefaultMethod.body()._return(defaultValue)

        jsonArray.each {
            def enumEntry = enumClass.enumConstant(it.key.toString().toUpperCase())
            enumEntry.arg(lit(it.key))
            enumEntry.arg(lit(it.default))
            allValues.add(enumEntry)
        }
    }
}
