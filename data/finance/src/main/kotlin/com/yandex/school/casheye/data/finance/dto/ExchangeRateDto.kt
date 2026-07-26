package com.yandex.school.casheye.data.finance.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class ExchangeRateDto(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val base: String,
    val quote: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal,
)

internal object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) {
        encoder.encodeString(value.toString())
    }
}

internal object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal =
        requireNotNull(decoder as? JsonDecoder) {
            "BigDecimalSerializer supports JSON only"
        }.decodeJsonElement()
            .jsonPrimitive
            .content
            .toBigDecimal()

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        requireNotNull(encoder as? JsonEncoder) {
            "BigDecimalSerializer supports JSON only"
        }.encodeJsonElement(JsonPrimitive(value))
    }
}
