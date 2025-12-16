package dev.aurakai.auraframefx.api.client.infrastructure

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object Serializer {
    @Deprecated(
        "Use Serializer.kotlinxSerializationAdapters instead",
        replaceWith = ReplaceWith("Serializer.kotlinxSerializationAdapters"),
        level = DeprecationLevel.ERROR
    )
    @JvmStatic
    val kotlinSerializationAdapters: SerializersModule
        get() {
            return kotlinxSerializationAdapters
        }

    private var isAdaptersInitialized = false

    @JvmStatic
    val kotlinxSerializationAdapters: SerializersModule by lazy {
        isAdaptersInitialized = true
        SerializersModule {
            contextual(BigDecimal::class, BigDecimalAdapter)
            contextual(BigInteger::class, BigIntegerAdapter)
            contextual(LocalDate::class, LocalDateAdapter)
            contextual(LocalDateTime::class, LocalDateTimeAdapter)
            contextual(OffsetDateTime::class, OffsetDateTimeAdapter)
            contextual(UUID::class, UUIDAdapter)
            contextual(AtomicInteger::class, AtomicIntegerAdapter)
            contextual(AtomicLong::class, AtomicLongAdapter)
            contextual(AtomicBoolean::class, AtomicBooleanAdapter)
            contextual(URI::class, URIAdapter)
            contextual(URL::class, URLAdapter)
            contextual(StringBuilder::class, StringBuilderAdapter)

            apply(kotlinxSerializationAdaptersConfiguration)
        }
    }

    var kotlinxSerializationAdaptersConfiguration: SerializersModuleBuilder.() -> Unit = {}
        set(value) {
            check(!isAdaptersInitialized) {
                "Cannot configure kotlinxSerializationAdaptersConfiguration after kotlinxSerializationAdapters has been initialized."
            }
            field = value
        }

    @Deprecated(
        "Use Serializer.kotlinxSerializationJson instead",
        replaceWith = ReplaceWith("Serializer.kotlinxSerializationJson"),
        level = DeprecationLevel.ERROR
    )
    @JvmStatic
    val jvmJson: Json
        get() {
            return kotlinxSerializationJson
        }

    private var isJsonInitialized = false

    @JvmStatic
    val kotlinxSerializationJson: Json by lazy {
        isJsonInitialized = true
        Json {
            serializersModule = kotlinxSerializationAdapters
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true

            apply(kotlinxSerializationJsonConfiguration)
        }
    }

    var kotlinxSerializationJsonConfiguration: JsonBuilder.() -> Unit = {}
        set(value) {
            check(!isJsonInitialized) {
                "Cannot configure kotlinxSerializationJsonConfiguration after kotlinxSerializationJson has been initialized."
            }
            field = value
        }
}

object BigDecimalAdapter {
    fun serialize(value: BigDecimal): String = value.toString()
    fun deserialize(value: String): BigDecimal = BigDecimal(value)
}

object BigIntegerAdapter {
    fun serialize(value: BigInteger): String = value.toString()
    fun deserialize(value: String): BigInteger = BigInteger(value)
}

object LocalDateAdapter {
    fun serialize(value: LocalDate): String = value.toString()
    fun deserialize(value: String): LocalDate = LocalDate.parse(value)
}

object LocalDateTimeAdapter {
    fun serialize(value: LocalDateTime): String = value.toString()
    fun deserialize(value: String): LocalDateTime = LocalDateTime.parse(value)
}

object OffsetDateTimeAdapter {
    fun serialize(value: OffsetDateTime): String = value.toString()
    fun deserialize(value: String): OffsetDateTime = OffsetDateTime.parse(value)
}

object UUIDAdapter {
    fun serialize(value: UUID): String = value.toString()
    fun deserialize(value: String): UUID = UUID.fromString(value)
}

object AtomicIntegerAdapter {
    fun serialize(value: AtomicInteger): String = value.toString()
    fun deserialize(value: String): AtomicInteger = AtomicInteger(value.toInt())
}

object AtomicLongAdapter {
    fun serialize(value: AtomicLong): String = value.toString()
    fun deserialize(value: String): AtomicLong = AtomicLong(value.toLong())
}

object AtomicBooleanAdapter {
    fun serialize(value: AtomicBoolean): String = value.toString()
    fun deserialize(value: String): AtomicBoolean = AtomicBoolean(value.toBoolean())
}

object URIAdapter {
    fun serialize(value: URI): String = value.toString()
    fun deserialize(value: String): URI = URI.create(value)
}

object URLAdapter {
    fun serialize(value: URL): String = value.toString()
    fun deserialize(value: String): URL = URL(value)
}

object StringBuilderAdapter {
    fun serialize(value: StringBuilder): String = value.toString()
    fun deserialize(value: String): StringBuilder = StringBuilder(value)
}
