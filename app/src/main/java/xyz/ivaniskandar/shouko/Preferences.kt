package xyz.ivaniskandar.shouko

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Preferences(
    @ProtoNumber(1) val assistButtonEnabled: Boolean = true,
    @ProtoNumber(2) val assistButtonAction: String = "",
    @ProtoNumber(3) val hideAssistantCue: Boolean = false,
    @ProtoNumber(4) val preventPocketTouchEnabled: Boolean = false,
    @ProtoNumber(5) val flipToShushEnabled: Boolean = false,
    @ProtoNumber(6) val coffeeBoardingDone: Boolean = false,
    @ProtoNumber(7) val teaBoardingDone: Boolean = false,
    @ProtoNumber(8) val lockscreenLeftAction: String = "",
    @ProtoNumber(9) val lockscreenRightAction: String = "",
)
