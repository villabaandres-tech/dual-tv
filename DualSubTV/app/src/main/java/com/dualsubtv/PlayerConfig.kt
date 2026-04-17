package com.dualsubtv

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerConfig(
    val videoUri: String,

    // Subtitle 1 (top) - NONE | EMBEDDED | EXTERNAL
    val sub1Type: String = SUB_NONE,
    val sub1Source: String = "",      // path/url for EXTERNAL
    val sub1TrackIndex: Int = 0,      // track index for EMBEDDED

    // Subtitle 2 (bottom) - NONE | EMBEDDED | EXTERNAL
    val sub2Type: String = SUB_NONE,
    val sub2Source: String = "",
    val sub2TrackIndex: Int = 0
) : Parcelable {
    companion object {
        const val SUB_NONE     = "NONE"
        const val SUB_EMBEDDED = "EMBEDDED"
        const val SUB_EXTERNAL = "EXTERNAL"
        const val EXTRA_KEY    = "player_config"
    }
}
