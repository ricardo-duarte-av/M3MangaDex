package pt.aguiarvieira.m3mangadex.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Everything needed to keep a MangaDex session alive. Never includes the password. */
@Serializable
data class StoredSession(
    val clientId: String,
    val clientSecret: String,
    val username: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val accessExpiresAt: Long = 0,
    val refreshExpiresAt: Long = 0,
    /** MangaDex stopped accepting the refresh token: ask for the password again. */
    val expired: Boolean = false,
)

interface SessionStore {
    fun read(): StoredSession?

    fun write(session: StoredSession?)
}

/**
 * The session on disk, encrypted with an AES-GCM key that lives in the Android Keystore (it never
 * leaves the secure hardware, and isn't in backups). The file is in `noBackupFilesDir` too, so a
 * restored device starts logged out rather than with an undecryptable blob.
 */
internal class KeystoreSessionStore(
    context: Context,
    private val json: Json,
) : SessionStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))

    override fun read(): StoredSession? =
        try {
            val bytes = file.readFully()
            if (bytes.size <= IV_BYTES) return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            val plain = cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
            json.decodeFromString(StoredSession.serializer(), plain.decodeToString())
        } catch (_: IOException) {
            null
        } catch (_: GeneralSecurityException) {
            // Key gone (app data cleared on some devices, or a restore): start over, logged out.
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    override fun write(session: StoredSession?) {
        if (session == null) {
            file.delete()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted =
            cipher.iv + cipher.doFinal(json.encodeToString(StoredSession.serializer(), session).encodeToByteArray())
        val stream = file.startWrite()
        try {
            stream.write(encrypted)
            file.finishWrite(stream)
        } catch (e: IOException) {
            file.failWrite(stream)
            throw e
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "mangadex-session.bin"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "m3mangadex.session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
