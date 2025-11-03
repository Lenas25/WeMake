package com.utp.wemake.services

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object GeminiHelper {
    /**
     * Método sincrónico que puede ser llamado desde Java
     * usando CompletableFuture.supplyAsync
     */
    @Throws(Exception::class)
    fun generateContentSync(model: GenerativeModel, prompt: String): String {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val response = model.generateContent(prompt)
                response.text ?: throw Exception("Respuesta vacía de Gemini")
            }
        }
    }
}