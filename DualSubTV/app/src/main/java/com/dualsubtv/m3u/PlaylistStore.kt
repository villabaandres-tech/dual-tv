package com.dualsubtv.m3u

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PlaylistStore {

    private const val PREFS = "dualsubtv_playlists"
    private const val KEY   = "playlists"

    fun savePlaylists(context: Context, playlists: List<SavedPlaylist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            arr.put(JSONObject().apply {
                put("name",   pl.name)
                put("source", pl.source)
                put("addedAt", pl.addedAt)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun loadPlaylists(context: Context): List<SavedPlaylist> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SavedPlaylist(
                    name    = obj.optString("name", "Lista"),
                    source  = obj.optString("source", ""),
                    addedAt = obj.optLong("addedAt", 0L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class SavedPlaylist(
    val name: String,
    val source: String,
    val addedAt: Long = System.currentTimeMillis()
)
