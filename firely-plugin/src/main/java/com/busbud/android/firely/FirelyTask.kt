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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException

/**
 * Task that generates the FirelyConfig.kt file in the outputDir based on the inputFile
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

        // Main class
        val configClassBuilder =
            TypeSpec.classBuilder(ClassName("com.busbud.android.firely", "FirelyConfig"))
                .addSuperinterface(ClassName("com.busbud.android.firely", "IFirelyConfig"))

        // allValues method
        val array = ClassName("kotlin", "Array")
        val firelyItem = ClassName("com.busbud.android.firely", "IFirelyItem")
        val allValuesMethodBuilder = FunSpec.builder("allValues").addModifiers(KModifier.OVERRIDE)
            .returns(array.parameterizedBy(firelyItem))

        // Create Enums
        val createdEnumsList = mutableListOf<String>()
        for (key in json.jsonObject.keys) {
            if (!name.isNullOrBlank() && json[key]!!.jsonArray.size != 0) {

                val enum = buildEnum(
                    key.convertUnderscoreToJava(),
                    json[key]!!.jsonArray,
                    createdEnumsList
                )

                // Add to class
                configClassBuilder.addType(enum)
            }
        }

        // Add allValues method to class
        allValuesMethodBuilder.addStatement(
            "return arrayOf(%L)",
            createdEnumsList.joinToString(",")
        )
        configClassBuilder.addFunction(allValuesMethodBuilder.build())

        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw IOException("Could not create directory: $outputDir")
        }
        println("Writing class in ${outputDir.path} :")
        val fileSpec = FileSpec.builder("com.busbud.android.firely", "FirelyConfig")
            .addType(
                configClassBuilder
                    .addKdoc("Generated class - Do not edit")
                    .build()
            )
            .build()
        fileSpec.writeTo(outputDir)
    }


    private fun String.convertUnderscoreToJava(): String {
        var convertedName: String = this
        if (convertedName.isNotBlank()) {
            convertedName = convertedName[0].uppercase() + (if (convertedName.length > 1) {
                this.substring(1)
            } else {
                ""
            })
        }
        return convertedName.replace("_\\w".toRegex()) {
            it.value[1].uppercase()
        }
    }


    private fun buildEnum(
        enumName: String,
        jsonArray: JsonArray,
        enumsList: MutableList<String>
    ): TypeSpec {


        return TypeSpec.enumBuilder(enumName)
            .addSuperinterface(ClassName("com.busbud.android.firely", "IFirelyItem"))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("key", String::class)
                    .addParameter("default", Any::class)
                    .build()
            ).addProperty(
                PropertySpec.builder("key", String::class, KModifier.OVERRIDE)
                    .initializer("key")
                    .build()
            ).addProperty(
                PropertySpec.builder("default", Any::class, KModifier.OVERRIDE)
                    .initializer("default")
                    .build()
            )

            .apply {
                jsonArray.forEach {
                    it as JsonObject

                    val key = it["key"].toString().replace("\"", "")
                    val default = it["default"]!!.jsonPrimitive.toString()

                    addEnumConstant(
                        key.uppercase(),
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%S", key)
                            .addSuperclassConstructorParameter("%L", default)
                            .build()
                    )

                    enumsList.add("$enumName.${key.uppercase()}")
                }
            }
            .build()
    }

}
