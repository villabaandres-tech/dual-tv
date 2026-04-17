package com.dualsubtv.m3u

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ContentType { LIVE, VOD, UNKNOWN }

@Parcelize
data class M3uEntry(
    val title: String,
    val url: String,
    val group: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val contentType: ContentType = ContentType.UNKNOWN
) : Parcelable

data class M3uPlaylist(
    val name: String,
    val source: String,          // URL or local path
    val entries: List<M3uEntry> = emptyList(),
    val loadedAt: Long = 0L
)

data class M3uGroup(
    val name: String,
    val entries: List<M3uEntry>
)
