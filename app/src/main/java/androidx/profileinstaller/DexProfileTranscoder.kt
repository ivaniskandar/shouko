package androidx.profileinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object DexProfileTranscoder {
    private const val PROFILE_SOURCE_LOCATION = "assets/dexopt/baseline.prof"
    private const val PROFILE_META_LOCATION = "assets/dexopt/baseline.profm"

    private const val PROFILE_SOURCE_FILE = "primary.prof"
    private const val PROFILE_META_FILE = "primary.profm"

    /**
     * Creates a dex metadata file for the given APK file if it contains a baseline profile.
     *
     * @return the dex metadata file if successfully created, null otherwise.
     */
    suspend fun run(
        context: Context,
        apkFile: File,
    ): File? {
        if (apkFile.extension != "apk") return null
        val desiredVersion = desiredProfileVersion() ?: return null
        val dmFile = File(context.cacheDir, "${apkFile.nameWithoutExtension}.dm")
        var success = false
        withContext(Dispatchers.IO) {
            dmFile.delete()
            ZipFile(apkFile).use { zf ->
                val profSource = zf.getEntry(PROFILE_SOURCE_LOCATION) ?: return@use
                val profMeta = zf.getEntry(PROFILE_META_LOCATION)
                var profile =
                    zf.getInputStream(profSource).use { profileStr ->
                        try {
                            val baselineVersion = ProfileTranscoder.readHeader(profileStr, ProfileTranscoder.MAGIC_PROF)
                            ProfileTranscoder.readProfile(profileStr, baselineVersion, apkFile.name)
                        } catch (_: Exception) {
                            null
                        }
                    } ?: return@use
                if (requiresProfileMetadata()) {
                    if (profMeta == null) return@use
                    profile = zf.getInputStream(profMeta).use { metaStr ->
                        try {
                            val metaVersion = ProfileTranscoder.readHeader(metaStr, ProfileTranscoder.MAGIC_PROFM)
                            ProfileTranscoder.readMeta(metaStr, metaVersion, desiredVersion, profile)
                        } catch (_: Exception) {
                            null
                        }
                    } ?: return@use
                }
                ByteArrayOutputStream().use { os ->
                    success =
                        try {
                            ProfileTranscoder.writeHeader(os, desiredVersion)
                            ProfileTranscoder.transcodeAndWriteBody(os, desiredVersion, profile)
                        } catch (_: Exception) {
                            false
                        }
                    if (success) {
                        ZipOutputStream(FileOutputStream(dmFile)).use { zipOut ->
                            try {
                                zipOut.putNextEntry(ZipEntry(PROFILE_SOURCE_FILE))
                                os.writeTo(zipOut)
                                zipOut.closeEntry()
                            } catch (_: Exception) {
                                success = false
                            }

                            if (success) {
                                zf.getInputStream(profMeta)?.use { metaStr ->
                                    try {
                                        zipOut.putNextEntry(ZipEntry(PROFILE_META_FILE))
                                        metaStr.copyTo(zipOut)
                                        zipOut.closeEntry()
                                    } catch (_: Exception) {
                                        success = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return dmFile.takeIf { success && it.exists() }
    }

    /**
     * From DeviceProfileWriter.desiredVersion
     *
     * @return the desired profile version for the current device.
     */
    @SuppressLint("ObsoleteSdkInt", "RestrictedApi")
    private fun desiredProfileVersion(): ByteArray? {
        // If SDK is pre or post supported version, we don't want to do anything, so return null.
        if (Build.VERSION.SDK_INT < ProfileVersion.MIN_SUPPORTED_SDK) {
            return null
        }

        // Profile version V015_S is forward compatible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ProfileVersion.V015_S
        }

        return when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.N,
            Build.VERSION_CODES.N_MR1,
            -> ProfileVersion.V001_N

            Build.VERSION_CODES.O -> ProfileVersion.V005_O

            Build.VERSION_CODES.O_MR1 -> ProfileVersion.V009_O_MR1

            Build.VERSION_CODES.P,
            Build.VERSION_CODES.Q,
            Build.VERSION_CODES.R,
            -> ProfileVersion.V010_P

            Build.VERSION_CODES.S,
            Build.VERSION_CODES.S_V2,
            Build.VERSION_CODES.TIRAMISU,
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            -> ProfileVersion.V015_S

            else -> null
        }
    }

    /**
     * From DeviceProfileWriter.requiresMetadata
     */
    @SuppressLint("ObsoleteSdkInt", "RestrictedApi")
    private fun requiresProfileMetadata(): Boolean {
        // If SDK is pre-N, we don't want to do anything, so return null.
        if (Build.VERSION.SDK_INT < ProfileVersion.MIN_SUPPORTED_SDK) {
            return false
        }

        // The profiles for S require a typeIdCount. Therefore metadata is required.
        // Profile version V015_S is forward compatible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return true
        }

        return when (Build.VERSION.SDK_INT) {
            // The profiles for N and N_MR1 used class ids to identify classes instead of type
            // ids, which is what the V0.1.0 profile encodes, so a metadata file is required in
            // order to transcode to this profile.
            Build.VERSION_CODES.N,
            Build.VERSION_CODES.N_MR1,
            -> true

            // for all of these versions, the data encoded in the V0.1.0 profile is enough to
            // losslessly transcode into these other formats.
            Build.VERSION_CODES.O,
            Build.VERSION_CODES.O_MR1,
            Build.VERSION_CODES.P,
            Build.VERSION_CODES.Q,
            Build.VERSION_CODES.R,
            -> false

            else -> false
        }
    }
}
