package com.aicodingcli.conversation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Serializer for Instant objects
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochSecond)
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.ofEpochSecond(decoder.decodeLong())
    }
}

/**
 * Serializer for java.time.Duration objects
 */
object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.toMillis())
    }

    override fun deserialize(decoder: Decoder): Duration {
        return Duration.ofMillis(decoder.decodeLong())
    }
}

/**
 * Serializer for kotlin.time.Duration objects
 */
object KotlinDurationSerializer : KSerializer<kotlin.time.Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KotlinDuration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: kotlin.time.Duration) {
        encoder.encodeLong(value.inWholeMilliseconds)
    }

    override fun deserialize(decoder: Decoder): kotlin.time.Duration {
        return decoder.decodeLong().milliseconds
    }
}
