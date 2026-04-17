package com.dualsubtv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.dualsubtv.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSubtitleTypeListeners()
        setupPlayButton()
    }

    private fun setupSubtitleTypeListeners() {
        // Subtitle 1 radio buttons
        binding.rgSub1Type.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSub1None -> {
                    binding.etSub1.visibility = View.GONE
                    binding.layoutSub1EmbeddedSelector.visibility = View.GONE
                }
                R.id.rbSub1Embedded -> {
                    binding.etSub1.visibility = View.GONE
                    binding.layoutSub1EmbeddedSelector.visibility = View.VISIBLE
                }
                R.id.rbSub1External -> {
                    binding.etSub1.visibility = View.VISIBLE
                    binding.layoutSub1EmbeddedSelector.visibility = View.GONE
                }
            }
        }

        // Subtitle 2 radio buttons
        binding.rgSub2Type.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSub2None -> {
                    binding.etSub2.visibility = View.GONE
                    binding.layoutSub2EmbeddedSelector.visibility = View.GONE
                }
                R.id.rbSub2Embedded -> {
                    binding.etSub2.visibility = View.GONE
                    binding.layoutSub2EmbeddedSelector.visibility = View.VISIBLE
                }
                R.id.rbSub2External -> {
                    binding.etSub2.visibility = View.VISIBLE
                    binding.layoutSub2EmbeddedSelector.visibility = View.GONE
                }
            }
        }
    }

    private fun setupPlayButton() {
        binding.btnPlay.setOnClickListener {
            val videoUrl = binding.etVideoUrl.text.toString().trim()
            if (videoUrl.isEmpty()) {
                binding.etVideoUrl.error = "Ingresa una URL o ruta de video"
                return@setOnClickListener
            }

            val sub1Type = when (binding.rgSub1Type.checkedRadioButtonId) {
                R.id.rbSub1Embedded -> PlayerConfig.SUB_EMBEDDED
                R.id.rbSub1External -> PlayerConfig.SUB_EXTERNAL
                else -> PlayerConfig.SUB_NONE
            }

            val sub2Type = when (binding.rgSub2Type.checkedRadioButtonId) {
                R.id.rbSub2Embedded -> PlayerConfig.SUB_EMBEDDED
                R.id.rbSub2External -> PlayerConfig.SUB_EXTERNAL
                else -> PlayerConfig.SUB_NONE
            }

            val config = PlayerConfig(
                videoUri      = videoUrl,
                sub1Type      = sub1Type,
                sub1Source    = binding.etSub1.text.toString().trim(),
                sub1TrackIndex = binding.spinnerSub1Track.selectedItemPosition,
                sub2Type      = sub2Type,
                sub2Source    = binding.etSub2.text.toString().trim(),
                sub2TrackIndex = binding.spinnerSub2Track.selectedItemPosition
            )

            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerConfig.EXTRA_KEY, config)
            }
            startActivity(intent)
        }
    }
}
