package com.benbuzard.apilib.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias RegexAsString = @Serializable(with = RegexAsStringSerializer::class) Regex

object RegexAsStringSerializer : KSerializer<Regex> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.text.Regex", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Regex {
        val string = decoder.decodeString()
        return Regex(string)
    }

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }

}