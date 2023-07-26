package com.benbuzard.apilib.jsonschema

import com.benbuzard.apilib.serializers.RegexAsString
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonElement
import kotlin.Boolean
import kotlin.String
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

import kotlin.String as KString
import kotlin.Boolean as KBoolean

// TODO: some of these fields are not entirely correct (some should have array options, some should have boolean options_
// TODO: much of this needs to be verified against the spec
// TODO: extract this into a separate library (jsonschema-kt?)

//@Polymorphic
@Serializable
sealed class JsonSchemaNode(val type: JsonSchemaType? = null) {
    abstract val title: KString?
    abstract val description: KString?
    abstract val default: JsonElement?
    abstract val examples: List<JsonElement>?
    abstract val deprecated: KBoolean?
    abstract val readOnly: KBoolean?
    abstract val writeOnly: KBoolean?
    @SerialName("\$comment") abstract val comment: KString?
    abstract val enum: List<JsonElement>?
    abstract val const: JsonElement?

    @Serializable
    class String(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
        val minLength: Int? = null,
        val maxLength: Int? = null,
        val pattern: RegexAsString? = null,
        val format: KString? = null,

        val contentMediaType: KString? = null,
        val contentEncoding: KString? = null,
    ) : JsonSchemaNode(
        type = JsonSchemaType.String,
    )

    @Serializable
    class Number(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
        val multipleOf: Double? = null,
        val minimum: Double? = null,
        val exclusiveMinimum: Double? = null,
        val maximum: Double? = null,
        val exclusiveMaximum: Double? = null,
    ) : JsonSchemaNode(
        type = JsonSchemaType.Number
    )

    @Serializable
    class Integer(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
        val multipleOf: Double? = null,
        val minimum: Double? = null,
        val exclusiveMinimum: Double? = null,
        val maximum: Double? = null,
        val exclusiveMaximum: Double? = null,
    ) : JsonSchemaNode(
        type = JsonSchemaType.Integer
    )

    @Serializable
    class MyObject(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
        val properties: Map<KString, JsonSchemaNode> = emptyMap(),
        val patternProperties: Map<RegexAsString, JsonSchemaNode> = emptyMap(),
        val required: Set<KString> = emptySet(),
        val propertyNames: JsonSchemaNode? = null,
        val minProperties: Int? = null,
        val maxProperties: Int? = null,
        // TODO: additionalProperties, unevaluatedProperties
    ) : JsonSchemaNode(
        type = JsonSchemaType.Object
    )

    @Serializable
    class Array(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
        val items: JsonSchemaNode? = null,
        val prefixItems: List<JsonSchemaNode> = emptyList(),
        val contains: JsonSchemaNode? = null,
        val minContains: Int? = null,
        val maxContains: Int? = null,
        val minItems: Int? = null,
        val maxItems: Int? = null,
        val uniqueItems: KBoolean? = null, // this makes it a set instead of a list
        // TODO: additionalItems, unevaluatedItems
    ) : JsonSchemaNode(
        type = JsonSchemaType.Array
    )

    @Serializable
    class Boolean(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
    ) : JsonSchemaNode(
        type = JsonSchemaType.Boolean
    )

    @Serializable
    class Null(
        override val title: KString? = null,
        override val description: KString? = null,
        override val default: JsonElement? = null,
        override val examples: List<JsonElement>? = null,
        override val deprecated: KBoolean? = null,
        override val readOnly: KBoolean? = null,
        override val writeOnly: KBoolean? = null,
        override val comment: KString? = null,
        override val enum: List<JsonElement>? = null,
        override val const: JsonElement? = null,
    ) : JsonSchemaNode(
        type = JsonSchemaType.Null
    )
}

@Serializable
enum class JsonSchemaType {
    @SerialName("string") String,
    @SerialName("number") Number,
    @SerialName("integer") Integer,
    @SerialName("object") Object,
    @SerialName("array") Array,
    @SerialName("boolean") Boolean,
    @SerialName("null") Null,
    ;
}

expect fun createJsonSchemaFromClass(clazz: KClass<*>, description: KString? = null): JsonSchemaNode
expect fun createJsonSchemaFromFunction(function: KFunction<*>, description: KString? = null): JsonSchemaNode // TODO: argument descriptions

object WhyDuplicateClassName {
    fun typeIsSubclass(clazz: KClass<*>, type: KType): Boolean = type.isSubtypeOf(clazz.starProjectedType)

    fun KType.isSubclassOf(clazz: KClass<*>): Boolean = typeIsSubclass(clazz, this)

    fun schemaNodeFromType(type: KType): JsonSchemaNode = when {
        type.isSubclassOf(String::class) -> JsonSchemaNode.String()
        type.isSubclassOf(Int::class) -> JsonSchemaNode.Integer()
        type.isSubclassOf(Short::class) -> JsonSchemaNode.Integer()
        type.isSubclassOf(Long::class) -> JsonSchemaNode.Integer()
        type.isSubclassOf(Float::class) -> JsonSchemaNode.Number()
        type.isSubclassOf(Double::class) -> JsonSchemaNode.Number()
        type.isSubclassOf(Boolean::class) -> JsonSchemaNode.Boolean()
        type.isSubclassOf(Array::class) -> JsonSchemaNode.Array(
            items = schemaNodeFromType(type.arguments.first().type ?: throw IllegalArgumentException("Array must have a type"))
        )
        type.isSubclassOf(List::class) -> JsonSchemaNode.Array(
            items = schemaNodeFromType(type.arguments.first().type ?: throw IllegalArgumentException("List must have a type"))
        )
        type.isSubclassOf(Set::class) -> JsonSchemaNode.Array(
            items = schemaNodeFromType(type.arguments.first().type ?: throw IllegalArgumentException("Set must have a type"))
        )
        type.isSubclassOf(Map::class) -> JsonSchemaNode.Array(
            items = JsonSchemaNode.MyObject(
                properties = mapOf(
                    "key" to schemaNodeFromType(type.arguments.first().type ?: throw IllegalArgumentException("Map must have a key type")),
                    "value" to schemaNodeFromType(type.arguments[1].type ?: throw IllegalArgumentException("Map must have a value type")),
                )
            )
        )

        else -> throw IllegalArgumentException("Unsupported type: $type")
    }

    fun createJsonSchemaFromFunctionArguments(name: String, arguments: Map<String, Any?>): JsonSchemaNode {
        val parameters = mutableMapOf<String, JsonSchemaNode>()

        arguments.forEach { (name, value) ->
            if (value == null) {
                parameters[name] = JsonSchemaNode.Null()
            }
            parameters[name] = schemaNodeFromType(value!!::class.starProjectedType)
        }

        return JsonSchemaNode.MyObject(
            title = name,
            properties = parameters.toMap(),
        )
    }
}