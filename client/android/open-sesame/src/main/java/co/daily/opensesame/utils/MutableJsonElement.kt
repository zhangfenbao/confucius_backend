package co.daily.opensesame.utils

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

@Serializable(with = MutableJsonElementSerializer::class)
sealed interface MutableJsonElement {

    @Serializable(with = MutableJsonElementNullSerializer::class)
    object Null : MutableJsonElement {
        override fun toImmutable() = JsonNull
    }

    @Serializable
    @JvmInline
    value class Str(val value: String) : MutableJsonElement {
        override fun toImmutable() = JsonPrimitive(value)
    }

    @Serializable
    @JvmInline
    value class Bool(val value: Boolean) : MutableJsonElement {
        override fun toImmutable() = JsonPrimitive(value)
    }

    @Serializable
    @JvmInline
    value class Number(val value: Double) : MutableJsonElement {
        override fun toImmutable() = JsonPrimitive(value)
    }

    @Serializable(with = MutableJsonElementArraySerializer::class)
    class Array(val value: MutableList<MutableJsonElement>) : MutableJsonElement {
        constructor(vararg values: MutableJsonElement) : this(value = values.toMutableList())

        override fun toImmutable() = JsonArray(value.map(MutableJsonElement::toImmutable))

        fun getElemWithTag(tagName: String, tagValue: String) =
            value.firstOrNull { it.getString(tagName) == tagValue } as? Object

        fun getOrAddElemWithTag(tagName: String, tagValue: String) =
            getElemWithTag(tagName, tagValue) ?: Object(
                mutableMapOf(tagName to Str(tagValue))
            ).also {
                value.add(it)
            }
    }

    @Serializable(with = MutableJsonElementObjectSerializer::class)
    class Object(val data: MutableMap<String, MutableJsonElement>) : MutableJsonElement {
        constructor(vararg values: Pair<String, MutableJsonElement>) : this(
            data = values.toMap(HashMap())
        )

        override fun toImmutable(): JsonObject {
            return JsonObject(data.mapValues { it.value.toImmutable() })
        }

        fun set(key: String, value: MutableJsonElement) {
            data[key] = value
        }

        fun set(key: String, value: String) = set(key, Str(value))

        fun set(key: String, value: Map<String, String>) {
            set(key, Object(value.mapValues { Str(it.value) }.toMutableMap()))
        }

        fun set(key: String, value: List<MutableJsonElement>) {
            set(key, Array(value.toMutableList()))
        }

        fun getOrAddObject(key: String) =
            getObject(key) ?: Object(mutableMapOf()).also { data[key] = it }

        fun getOrAddArray(key: String) =
            getArray(key) ?: Array(mutableListOf()).also { data[key] = it }
    }

    fun get(key: String) = (this as? Object)?.data?.get(key)

    fun getObject(key: String) = get(key) as? Object
    fun getArray(key: String) = get(key) as? Array
    fun getString(key: String) = (get(key) as? Str)?.value

    fun toImmutable(): JsonElement

    companion object {
        fun fromImmutable(element: JsonElement): MutableJsonElement {
            return when (element) {
                is JsonArray -> Array(element.map { fromImmutable(it) }.toMutableList())
                is JsonObject -> Object(element.mapValues { fromImmutable(it.value) }
                    .toMutableMap())

                is JsonPrimitive -> {
                    if (element.isString) {
                        return Str(element.contentOrNull ?: return Null)
                    }

                    element.booleanOrNull?.apply {
                        return Bool(this)
                    }

                    element.doubleOrNull?.apply {
                        return Number(this)
                    }

                    Str(element.contentOrNull ?: return Null)
                }

                JsonNull -> Null
            }
        }
    }
}

internal object MutableJsonElementSerializer :
    JsonContentPolymorphicSerializer<MutableJsonElement>(
        MutableJsonElement::class
    ) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MutableJsonElement> {

        return when (element) {
            is JsonArray -> MutableJsonElement.Array.serializer()
            is JsonObject -> MutableJsonElement.Object.serializer()
            is JsonPrimitive -> {

                if (element is JsonNull) {
                    return MutableJsonElement.Null.serializer()
                }

                if (element.isString) {
                    return MutableJsonElement.Str.serializer()
                }

                element.doubleOrNull?.apply {
                    return MutableJsonElement.Number.serializer()
                }

                element.booleanOrNull?.apply {
                    return MutableJsonElement.Bool.serializer()
                }

                throw RuntimeException("Unknown type: $element")
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal object MutableJsonElementNullSerializer : KSerializer<MutableJsonElement.Null> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("MutableJsonElement.Null", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MutableJsonElement.Null {

        if (decoder.decodeNotNullMark()) {
            throw RuntimeException("Expecting 'null'")
        }

        decoder.decodeNull()

        return MutableJsonElement.Null
    }

    override fun serialize(encoder: Encoder, value: MutableJsonElement.Null) {
        encoder.encodeNull()
    }
}

internal object MutableJsonElementArraySerializer : KSerializer<MutableJsonElement.Array> {

    private val innerSerializer = ListSerializer(MutableJsonElement.serializer())

    override val descriptor: SerialDescriptor
        get() = innerSerializer.descriptor

    override fun deserialize(decoder: Decoder): MutableJsonElement.Array {
        return MutableJsonElement.Array(innerSerializer.deserialize(decoder).toMutableList())
    }

    override fun serialize(encoder: Encoder, value: MutableJsonElement.Array) {
        innerSerializer.serialize(encoder, value.value)
    }
}

internal object MutableJsonElementObjectSerializer : KSerializer<MutableJsonElement.Object> {

    private val innerSerializer =
        MapSerializer(MutableJsonElementObjectKeySerializer, MutableJsonElement.serializer())

    override val descriptor: SerialDescriptor
        get() = innerSerializer.descriptor

    override fun deserialize(decoder: Decoder): MutableJsonElement.Object {
        return MutableJsonElement.Object(innerSerializer.deserialize(decoder).toMutableMap())
    }

    override fun serialize(encoder: Encoder, value: MutableJsonElement.Object) {
        innerSerializer.serialize(encoder, value.data)
    }
}

internal object MutableJsonElementObjectKeySerializer : KSerializer<String> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("MutableJsonElement.Object.Key", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return decoder.decodeString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}