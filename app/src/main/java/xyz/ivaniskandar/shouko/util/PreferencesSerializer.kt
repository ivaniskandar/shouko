package xyz.ivaniskandar.shouko.util

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import xyz.ivaniskandar.shouko.Preferences
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
class PreferencesSerializer : Serializer<Preferences> {
    override val defaultValue: Preferences = Preferences()

    override suspend fun readFrom(input: InputStream): Preferences {
        try {
            return withContext(Dispatchers.IO) {
                ProtoBuf.decodeFromByteArray(input.readBytes())
            }
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: Preferences,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            output.write(ProtoBuf.encodeToByteArray(t))
        }
    }
}
