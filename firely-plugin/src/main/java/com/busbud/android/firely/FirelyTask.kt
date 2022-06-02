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
import com.sun.codemodel.JExpr._this
import com.sun.codemodel.JExpr.lit
import com.sun.codemodel.JMod.*
import kotlinx.serialization.json.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException

/**
 * Task that generates the FirelyConfig.java file in the outputDir based on the inputFile
 */
abstract class FirelyTask : DefaultTask() {

    @get:InputFile
    abstract var inputFile: File

    @get:OutputDirectory
    abstract var outputDir: File

    @TaskAction
    fun createFirelyConfig() {
        val json = Json.parseToJsonElement(inputFile.readText()).jsonObject

        println("Parsing ${inputFile.path}")

        val codeModel = JCodeModel()

        // General classes and dependencies
        val packageDef = codeModel._package("com.busbud.android.firely")
        val firelyItem = codeModel.directClass("com.busbud.android.firely.IFirelyItem")
        val firelyConfig = codeModel.directClass("com.busbud.android.firely.IFirelyConfig")

        // Main class
        val configClass = packageDef._class(PUBLIC or FINAL, "FirelyConfig", ClassType.CLASS)
        configClass.javadoc().add("Generated class - Do not edit")
        configClass._implements(firelyConfig)

        // allValues methods
        val allValuesArray = JExpr.newArray(firelyItem)

        // Create enum
        for (key in json.jsonObject.keys) {
            if (!name.isNullOrBlank()) {
                addEnumToJClass(
                    configClass,
                    firelyItem,
                    convertUnderscoreToJava(key),
                    json.get(key)!!.jsonArray,
                    allValuesArray
                )
            }
        }

        // Add allValues methods
        val allValuesMethod = configClass.method(PUBLIC, firelyItem.array(), "allValues")
        allValuesMethod.annotate(Override::class.java)
        allValuesMethod.body()._return(allValuesArray)

        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw IOException("Could not create directory: $outputDir")
        }
        println("Writing class in ${outputDir.path} :")
        codeModel.build(outputDir)
    }


    private fun convertUnderscoreToJava(name: String): String {
        var convertedName: String = name
        if (convertedName.isNotBlank()) {
            convertedName = convertedName[0].uppercase() + (if (convertedName.length > 1) {
                name.substring(1)
            } else {
                ""
            })
        }
        return convertedName.replace("_\\w".toRegex()) {
            it.value[1].uppercase()
        }
    }


    private fun addEnumToJClass(
        configClass: JDefinedClass,
        firelyItem: JClass,
        enumName: String,
        jsonArray: JsonArray,
        allValues: JArray
    ) {
        if (jsonArray.size == 0) {
            // Do nothing
            return
        }

        val enumClass = configClass._class(PUBLIC, enumName, ClassType.ENUM)
        enumClass._implements(firelyItem)

        val name = enumClass.field(PRIVATE or FINAL, String::class.java, "mName")
        val defaultValue = enumClass.field(PRIVATE or FINAL, Object::class.java, "mDefault")

        val constructor = enumClass.constructor(PRIVATE)
        val param1 = constructor.param(String::class.java, "name")
        val param2 = constructor.param(Object::class.java, "defaultValue")
        val body = constructor.body()
        body.assign(_this().ref(name), param1)
        body.assign(_this().ref(defaultValue), param2)

        val getNameMethod = enumClass.method(PUBLIC, String::class.java, "getName")
        getNameMethod.annotate(Override::class.java)
        getNameMethod.body()._return(name)

        val getDefaultMethod = enumClass.method(PUBLIC, Object::class.java, "getDefault")
        getDefaultMethod.annotate(Override::class.java)
        getDefaultMethod.body()._return(defaultValue)

        jsonArray.forEach {
            it as JsonObject

            val key = it["key"].toString().replace("\"", "")
            val default = it["default"]!!.jsonPrimitive.toString()

            val enumEntry = enumClass.enumConstant(key.uppercase())
            enumEntry.arg(lit(key))
            enumEntry.arg(lit(default))
            allValues.add(enumEntry)
        }
    }
}
