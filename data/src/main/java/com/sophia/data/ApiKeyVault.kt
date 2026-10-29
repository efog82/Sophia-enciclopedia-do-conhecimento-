package com.sophia.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only AES-GCM ciphertext enters preferences. The non-exportable key belongs to Android Keystore. */
class ApiKeyVault(context:Context) {
    private val prefs=context.getSharedPreferences("sophia_api_credentials",Context.MODE_PRIVATE)
    @Synchronized private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey("sophia-api-v1",null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder("sophia-api-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())}.generateKey()
    }
    fun save(provider:String,value:String){
        if(value.isBlank()){prefs.edit().remove(provider).apply();return}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val encrypted=cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        prefs.edit().putString(provider,Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply()
    }
    fun read(provider:String):String? {
        val value=prefs.getString(provider,null)?:return null
        return try{val parts=value.split(':');val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)).toString(Charsets.UTF_8)}catch(e:Exception){throw IllegalStateException("Não foi possível abrir a chave guardada. Configure-a novamente.")}
    }
    fun has(provider:String)=prefs.contains(provider)
    var externalConsent:Boolean
        get()=prefs.getBoolean("external-consent-v1",false)
        set(value){prefs.edit().putBoolean("external-consent-v1",value).apply()}
}
