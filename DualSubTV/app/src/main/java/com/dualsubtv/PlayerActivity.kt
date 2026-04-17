package com.dualsubtv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.dualsubtv.databinding.ActivityPlayerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var config: PlayerConfig

    // External subtitle tracks
    private var sub1Entries: List<SubtitleEntry> = emptyList()
    private var sub2Entries: List<SubtitleEntry> = emptyList()

    // Subtitle update handler (runs every 100ms)
    private val handler = Handler(Looper.getMainLooper())
    private val subtitleRunnable = object : Runnable {
        override fun run() {
            updateSubtitles()
            handler.postDelayed(this, 100)
        }
    }

    // Controls auto-hide
    private val hideControlsRunnable = Runnable {
        binding.playerControls.visibility = View.GONE
    }

    // Seek bar update
    private val seekBarRunnable = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = intent.getParcelableExtra(PlayerConfig.EXTRA_KEY) ?: run {
            finish()
            return
        }

        setupPlayer()
        setupControls()
        loadExternalSubtitles()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // Disable ExoPlayer's built-in subtitle rendering — we handle it ourselves
        binding.playerView.subtitleView?.visibility = View.GONE

        val mediaItem = MediaItem.fromUri(config.videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        binding.progressBar.visibility = View.GONE
                        // Once tracks are available, set up embedded subtitle selection
                        setupEmbeddedSubtitles()
                    }
                    Player.STATE_BUFFERING -> binding.progressBar.visibility = View.VISIBLE
                    Player.STATE_ENDED -> finish()
                    else -> {}
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                setupEmbeddedSubtitles()
            }
        })
    }

    private fun setupEmbeddedSubtitles() {
        // ExoPlayer handles rendering of embedded subs for sub1 if set to EMBEDDED
        // We use track selection to pick the right embedded track for sub1
        // For dual embedded subs, we render sub1 via ExoPlayer's CueGroup callback
        // and sub2 we intercept manually — but since ExoPlayer only renders one sub
        // track at a time natively, we use our own overlay for BOTH to have full control.

        val tracks = player.currentTracks
        val subGroups = tracks.groups.filter { group ->
            group.type == C.TRACK_TYPE_TEXT
        }

        // For sub1 EMBEDDED: listen to cue updates from ExoPlayer
        if (config.sub1Type == PlayerConfig.SUB_EMBEDDED) {
            player.addListener(object : Player.Listener {
                override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                    val text = cueGroup.cues.joinToString("\n") { it.text?.toString() ?: "" }.trim()
                    binding.tvSubtitle1.text = text
                    binding.tvSubtitle1.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                }
            })
        }

        // For sub2 EMBEDDED: ExoPlayer can only natively render one text track.
        // We select a second track index and manually read its cues by temporarily
        // switching tracks — this is a limitation. A full solution requires FFmpeg.
        // For now, we inform the user this is a single-pass limitation.
        if (config.sub2Type == PlayerConfig.SUB_EMBEDDED && subGroups.size > 1) {
            // Note: showing second embedded sub track requires advanced track muxing
            binding.tvSubtitle2.text = "⚠ Sub 2 incrustado: usa archivo .srt externo para mejor compatibilidad"
            binding.tvSubtitle2.visibility = View.VISIBLE
        }
    }

    private fun loadExternalSubtitles() {
        val scope = CoroutineScope(Dispatchers.Main)

        if (config.sub1Type == PlayerConfig.SUB_EXTERNAL && config.sub1Source.isNotEmpty()) {
            scope.launch {
                sub1Entries = SubtitleLoader.load(this@PlayerActivity, config.sub1Source)
            }
        }

        if (config.sub2Type == PlayerConfig.SUB_EXTERNAL && config.sub2Source.isNotEmpty()) {
            scope.launch {
                sub2Entries = SubtitleLoader.load(this@PlayerActivity, config.sub2Source)
            }
        }
    }

    private fun updateSubtitles() {
        if (!::player.isInitialized) return
        val posMs = player.currentPosition

        // Sub 1 external
        if (config.sub1Type == PlayerConfig.SUB_EXTERNAL) {
            val entry = SubtitleLoader.findActiveEntry(sub1Entries, posMs)
            val text = entry?.text ?: ""
            binding.tvSubtitle1.text = text
            binding.tvSubtitle1.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Sub 2 external
        if (config.sub2Type == PlayerConfig.SUB_EXTERNAL) {
            val entry = SubtitleLoader.findActiveEntry(sub2Entries, posMs)
            val text = entry?.text ?: ""
            binding.tvSubtitle2.text = text
            binding.tvSubtitle2.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateSeekBar() {
        if (!::player.isInitialized) return
        val duration = player.duration.coerceAtLeast(1L)
        val position = player.currentPosition
        binding.seekBar.max = duration.toInt()
        binding.seekBar.progress = position.toInt()
        binding.tvTime.text = "${formatTime(position)} / ${formatTime(duration)}"
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player.play()
                binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        binding.btnRewind.setOnClickListener {
            player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
        }

        binding.btnFastForward.setOnClickListener {
            player.seekTo(player.currentPosition + 10_000)
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) player.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })
    }

    private fun showControls() {
        binding.playerControls.visibility = View.VISIBLE
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 4000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (binding.playerControls.visibility == View.VISIBLE) {
                    if (player.isPlaying) player.pause() else player.play()
                } else {
                    showControls()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT  -> { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); showControls(); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { player.seekTo(player.currentPosition + 10_000); showControls(); true }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_SPACE      -> { if (player.isPlaying) player.pause() else player.play(); true }
            KeyEvent.KEYCODE_BACK       -> { finish(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(subtitleRunnable)
        handler.post(seekBarRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(subtitleRunnable)
        handler.removeCallbacks(seekBarRunnable)
        handler.removeCallbacks(hideControlsRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) {
            player.release()
        }
    }
}
