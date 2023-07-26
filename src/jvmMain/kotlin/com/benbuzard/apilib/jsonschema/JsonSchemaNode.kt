package com.benbuzard.apilib.jsonschema

import com.benbuzard.apilib.jsonschema.WhyDuplicateClassName.schemaNodeFromType
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

// TODO: implement this as a kotlinx.serialization plugin
actual fun createJsonSchemaFromClass(clazz: KClass<*>, description: String?): JsonSchemaNode {
    TODO()
}

actual fun createJsonSchemaFromFunction(function: KFunction<*>, description: String?): JsonSchemaNode {
    var name = function.name

    // check if the name is for get/set with regex (getters have the name "<get-foo>" and setters have the name "<set-foo>")
    val regex = Regex("<(get|set)-(.*)>")
    val match = regex.matchEntire(name)
    if (match != null) {
        val (getOrSet, propertyName) = match.destructured
        name = "$getOrSet${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
    }

    // prepend package path and class name to name
    val packageName = function.javaMethod?.declaringClass?.`package`?.name
    val className = function.javaMethod?.declaringClass?.simpleName
    if (packageName != null && className != null) {
        name = "$packageName.$className.$name"
    }

    name = name.replace(".", "-")

    val parameters = mutableMapOf<String, JsonSchemaNode>()
    val requiredParameters = mutableSetOf<String>()

    function.parameters.forEach { parameter ->
        val name = parameter.name ?: throw IllegalArgumentException("Function parameters must have names")
        val optional = parameter.isOptional
        if (!optional) {
            requiredParameters.add(name)
        }

        val type = parameter.type
        val jsonSchemaNode = schemaNodeFromType(type)

        parameters[name] = jsonSchemaNode
    }

    return JsonSchemaNode.MyObject(
        title = name,
        description = description,
        properties = parameters.toMap(),
        required = requiredParameters,
    )
}