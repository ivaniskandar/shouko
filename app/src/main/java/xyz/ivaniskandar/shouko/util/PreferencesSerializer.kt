package xyz.ivaniskandar.shouko.util

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import xyz.ivaniskandar.shouko.Preferences
import java.io.InputStream
import java.io.OutputStream

class PreferencesSerializer : Serializer<Preferences> {
    override val defaultValue: Preferences =
        Preferences
            .newBuilder()
            .setAssistButtonEnabled(true)
            .setAssistButtonAction("")
            .setHideAssistantCue(false)
            .setPreventPocketTouchEnabled(false)
            .setFlipToShushEnabled(false)
            .setCoffeeBoardingDone(false)
            .setTeaBoardingDone(false)
            .build()

    override suspend fun readFrom(input: InputStream): Preferences {
        try {
            return Preferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Preferences,
        output: OutputStream,
    ) = t.writeTo(output)
}
