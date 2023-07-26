/*
 * Copyright 2023 Ben Buzard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.benbuzard.apilib.openai

import com.benbuzard.apilib.jsonschema.JsonSchemaNode
import com.benbuzard.apilib.jsonschema.WhyDuplicateClassName.createJsonSchemaFromFunctionArguments
import com.benbuzard.apilib.jsonschema.createJsonSchemaFromFunction
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction

sealed class OpenAIApi(val apiKey: String) {
    class Chat(apiKey: String) : OpenAIApi(apiKey) {
        suspend fun completions(
            model: String,
            messages: List<Message>,
            functions: List<Function>? = null,
            functionCall: String? = null,
//            temperature: Double = 1.0,
//            topP: Double = 1.0,
//            n: Int = 1,
//            maxTokens: Int? = null, // TODO: implement more complete version of this
        ): ChatCompletionResponse {
            val client = HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        encodeDefaults = false
                        classDiscriminator = "class_type"
                    })
                }
            }

            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                functions = functions,
                functionCall = functionCall,
            )

            val res = client.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer ${this@Chat.apiKey}")
                header("Content-Type", "application/json")
                setBody(request)
            }

            println(res.body<String>())

            return res.body<ChatCompletionResponse>()
        }
    }
}

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Int,
    val model: String,
    val choices: List<ChatCompletionResponseChoice>,
    val usage: ChatCompletionResponseUsage,
)

@Serializable
data class ChatCompletionResponseChoice(
    val index: Int,
    val message: Message,
    @SerialName("finish_reason") val finishReason: String,
)

@Serializable
data class ChatCompletionResponseUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val functions: List<Function>? = null,
    @SerialName("function_call") val functionCall: String? = null,
)

@Serializable
data class Message(
    val role: Role,
    val content: String?,
    val name: String? = null,
    @SerialName("function_call") val functionCall: FunctionCall? = null,
) {
    @Serializable
    enum class Role {
        @SerialName("system") System,
        @SerialName("user") User,
        @SerialName("assistant") Assistant,
        @SerialName("function") Function,
        ;
    }

    @Serializable
    data class FunctionCall(
        val name: String,
        @SerialName("arguments")
        private val _arguments: String,
    ) {
        @Transient lateinit var arguments: Map<String, Any?>
            private set

        init {
            println(_arguments)
            @Suppress("JSON_FORMAT_REDUNDANT")
            arguments = Json {
                encodeDefaults = false
                classDiscriminator = "class_type"
            }.decodeFromString<Map<String, JsonElement>>(_arguments)
                .mapValues { (_, value) ->
                    value.jsonPrimitive.contentOrNull
                }
        }

        fun call(): Any? {
            val function = Function(name = name, parameters = createJsonSchemaFromFunctionArguments(name, arguments)).toKFunction()
            return function.callBy(arguments)
        }
    }
}

private fun <R> KFunction<R>.callBy(args: Map<String, Any?>): R {
    val params: Map<KParameter, Any?> = parameters.map { parameter ->
        parameter to args[parameter.name]
    }.toMap()
    return callBy(params)
}

@Serializable
data class Function(
    val name: String,
    val description: String? = null,
    val parameters: JsonSchemaNode
) {
    companion object {
        fun fromKFunction(function: KFunction<*>, description: String? = null): Function {
            val jsonSchema = createJsonSchemaFromFunction(function, description) as JsonSchemaNode.MyObject
            return Function(
                name = jsonSchema.title!!,
                description = jsonSchema.description,
                parameters = jsonSchema.let {
                    return@let JsonSchemaNode.MyObject(
                        properties = it.properties,
                        required = it.required,
                    )
                }
            )
        }
    }

    fun toKFunction(): KFunction<*> {
        val rawName = name.replace("-", ".")
        val className = rawName.split(".").dropLast(1).joinToString(".")
        var functionName = rawName.split(".").last()

//        if (functionName.startsWith("get") || functionName.startsWith("set")) {
//            val actualFunctionName = functionName.removePrefix("get").removePrefix("set").replaceFirstChar { it.lowercase() }
//            functionName = "<${functionName.take(3)}-$actualFunctionName>"
//        }

        val clazz = Class.forName(className)
        val function = clazz.methods.find { // TODO: be mindful of overloads, use parameters to determine which function to use
            println("Comparing ${it.name} to $functionName")
            it.name == functionName
        } ?: throw IllegalArgumentException("Could not find function $name")

        return function.kotlinFunction ?: throw IllegalArgumentException("Could not find function $name")
    }
}