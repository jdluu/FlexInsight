package com.example.hevyinsight.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "api_key_preferences")

class ApiKeyManager(private val context: Context) {
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
    }
    
    /**
     * Get the stored API key as a Flow
     */
    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_KEY]
    }
    
    /**
     * Get the stored API key synchronously (suspend function)
     */
    suspend fun getApiKey(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[API_KEY]
        }.firstOrNull()
    }
    
    /**
     * Save the API key
     */
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }
    
    /**
     * Clear the stored API key
     */
    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }
    
    /**
     * Check if API key exists
     */
    suspend fun hasApiKey(): Boolean {
        return getApiKey() != null
    }
    
    /**
     * Validate API key format (basic validation)
     */
    fun isValidApiKeyFormat(apiKey: String): Boolean {
        // Basic validation - API key should not be empty
        // You can add more specific validation based on Hevy's API key format
        return apiKey.isNotBlank() && apiKey.length >= 10
    }
}

