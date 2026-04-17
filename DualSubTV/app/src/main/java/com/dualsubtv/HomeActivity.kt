package com.dualsubtv

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dualsubtv.databinding.ActivityHomeBinding
import com.dualsubtv.databinding.FragmentAddPlaylistBinding
import com.dualsubtv.databinding.FragmentContentBrowserBinding
import com.dualsubtv.databinding.FragmentPlaylistsBinding
import com.dualsubtv.m3u.*
import com.dualsubtv.ui.PlaylistAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        showPlaylistsPanel()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun setupNavigation() {
        binding.navPlaylists.setOnClickListener   { showPlaylistsPanel() }
        binding.navDirectPlay.setOnClickListener  { showDirectPlayPanel() }
        binding.navAddPlaylist.setOnClickListener { showAddPlaylistPanel() }
    }

    private fun setActiveNav(activeId: Int) {
        listOf(binding.navPlaylists, binding.navDirectPlay).forEach { btn ->
            btn.setTextColor(getColor(R.color.text_secondary))
        }
        when (activeId) {
            R.id.navPlaylists  -> binding.navPlaylists.setTextColor(getColor(R.color.text_primary))
            R.id.navDirectPlay -> binding.navDirectPlay.setTextColor(getColor(R.color.text_primary))
        }
    }

    // ── Playlists Panel ───────────────────────────────────────────────────────

    private fun showPlaylistsPanel() {
        setActiveNav(R.id.navPlaylists)
        val panelBinding = FragmentPlaylistsBinding.inflate(layoutInflater)
        binding.contentArea.removeAllViews()
        binding.contentArea.addView(panelBinding.root)

        val saved = PlaylistStore.loadPlaylists(this).toMutableList()

        if (saved.isEmpty()) {
            panelBinding.tvEmpty.visibility   = View.VISIBLE
            panelBinding.rvPlaylists.visibility = View.GONE
            return
        }

        panelBinding.tvEmpty.visibility   = View.GONE
        panelBinding.rvPlaylists.visibility = View.VISIBLE
        panelBinding.rvPlaylists.layoutManager = LinearLayoutManager(this)

        val adapter = PlaylistAdapter(
            saved,
            onOpen   = { pl -> openPlaylist(pl) },
            onDelete = { pl, idx ->
                val current = PlaylistStore.loadPlaylists(this).toMutableList()
                current.removeAll { it.source == pl.source && it.name == pl.name }
                PlaylistStore.savePlaylists(this, current)
                Toast.makeText(this, "Lista eliminada", Toast.LENGTH_SHORT).show()
                showPlaylistsPanel()
            }
        )
        panelBinding.rvPlaylists.adapter = adapter
    }

    // ── Add Playlist Panel ────────────────────────────────────────────────────

    private fun showAddPlaylistPanel() {
        val panelBinding = FragmentAddPlaylistBinding.inflate(layoutInflater)
        binding.contentArea.removeAllViews()
        binding.contentArea.addView(panelBinding.root)

        var selectedFileUri = ""

        panelBinding.rgSourceType.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.rbUrl -> {
                    panelBinding.etPlaylistUrl.visibility  = View.VISIBLE
                    panelBinding.btnPickFile.visibility    = View.GONE
                    panelBinding.tvSelectedFile.visibility = View.GONE
                }
                R.id.rbLocalFile -> {
                    panelBinding.etPlaylistUrl.visibility  = View.GONE
                    panelBinding.btnPickFile.visibility    = View.VISIBLE
                }
            }
        }

        panelBinding.btnPickFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/x-mpegurl", "application/x-mpegurl", "*/*"))
            }
            startActivityForResult(intent, REQUEST_PICK_M3U)
            // Store reference for result callback
            currentAddBinding = panelBinding
        }

        panelBinding.btnSavePlaylist.setOnClickListener {
            val name = panelBinding.etPlaylistName.text.toString().trim()
                .ifEmpty { "Lista ${System.currentTimeMillis() % 1000}" }

            val source = when (panelBinding.rgSourceType.checkedRadioButtonId) {
                R.id.rbUrl       -> panelBinding.etPlaylistUrl.text.toString().trim()
                R.id.rbLocalFile -> selectedFileUri
                else             -> ""
            }

            if (source.isEmpty()) {
                Toast.makeText(this, "Ingresa una URL o selecciona un archivo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            panelBinding.progressSave.visibility = View.VISIBLE
            panelBinding.tvSaveStatus.visibility = View.VISIBLE
            panelBinding.tvSaveStatus.text = "Verificando lista..."

            scope.launch {
                val entries = withContext(Dispatchers.IO) {
                    if (source.startsWith("http")) M3uParser.loadFromUrl(source)
                    else M3uParser.loadFromFile(this@HomeActivity, source)
                }

                panelBinding.progressSave.visibility = View.GONE

                if (entries.isEmpty()) {
                    panelBinding.tvSaveStatus.text = "⚠ No se encontraron entradas. Verifica la URL."
                    return@launch
                }

                val existing = PlaylistStore.loadPlaylists(this@HomeActivity).toMutableList()
                existing.add(SavedPlaylist(name, source))
                PlaylistStore.savePlaylists(this@HomeActivity, existing)

                panelBinding.tvSaveStatus.text = "✅ Lista guardada con ${entries.size} canales/videos"
                Toast.makeText(this@HomeActivity, "Lista guardada: ${entries.size} entradas", Toast.LENGTH_LONG).show()

                // Refresh playlists after short delay
                kotlinx.coroutines.delay(1200)
                showPlaylistsPanel()
            }
        }
    }

    // ── Open & Browse Playlist ────────────────────────────────────────────────

    private fun openPlaylist(playlist: SavedPlaylist) {
        val panelBinding = FragmentContentBrowserBinding.inflate(layoutInflater)
        binding.contentArea.removeAllViews()
        binding.contentArea.addView(panelBinding.root)

        panelBinding.tvPlaylistTitle.text = playlist.name
        panelBinding.layoutLoading.visibility = View.VISIBLE
        panelBinding.contentLayout.visibility = View.GONE

        scope.launch {
            val entries = withContext(Dispatchers.IO) {
                if (playlist.source.startsWith("http")) M3uParser.loadFromUrl(playlist.source)
                else M3uParser.loadFromFile(this@HomeActivity, playlist.source)
            }

            panelBinding.layoutLoading.visibility = View.GONE
            panelBinding.contentLayout.visibility = View.VISIBLE

            setupContentBrowser(panelBinding, entries)
        }
    }

    private fun setupContentBrowser(
        panelBinding: FragmentContentBrowserBinding,
        allEntries: List<M3uEntry>
    ) {
        var currentFilter = "ALL"
        var currentGroup  = ""

        fun filteredEntries(): List<M3uEntry> {
            var list = when (currentFilter) {
                "LIVE" -> allEntries.filter { it.contentType == ContentType.LIVE }
                "VOD"  -> allEntries.filter { it.contentType == ContentType.VOD }
                else   -> allEntries
            }
            if (currentGroup.isNotEmpty()) list = list.filter { it.group == currentGroup }
            val query = panelBinding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) list = list.filter { it.title.contains(query, ignoreCase = true) }
            return list
        }

        fun buildGroups(entries: List<M3uEntry>): List<M3uGroup> {
            val all = M3uGroup("Todos (${entries.size})", entries)
            val grouped = entries.groupBy { it.group.ifEmpty { "Sin grupo" } }
                .map { (name, items) -> M3uGroup(name, items) }
                .sortedBy { it.name }
            return listOf(all) + grouped
        }

        // Entry adapter
        val entryAdapter = com.dualsubtv.ui.EntryAdapter(filteredEntries()) { entry ->
            showPlayOptionsDialog(entry)
        }
        panelBinding.rvEntries.layoutManager = LinearLayoutManager(this)
        panelBinding.rvEntries.adapter = entryAdapter

        // Group adapter
        val groups = buildGroups(filteredEntries())
        val groupAdapter = com.dualsubtv.ui.GroupAdapter(groups) { group, _ ->
            currentGroup = if (group.name.startsWith("Todos")) "" else group.name
            entryAdapter.updateEntries(filteredEntries())
        }
        panelBinding.rvGroups.layoutManager = LinearLayoutManager(this)
        panelBinding.rvGroups.adapter = groupAdapter

        // Tabs
        fun setTab(tab: String) {
            currentFilter = tab
            currentGroup  = ""
            entryAdapter.updateEntries(filteredEntries())
            listOf(panelBinding.tabAll, panelBinding.tabLive, panelBinding.tabVod).forEach {
                it.setTextColor(getColor(R.color.text_secondary))
            }
            when (tab) {
                "ALL"  -> panelBinding.tabAll.setTextColor(getColor(R.color.text_primary))
                "LIVE" -> panelBinding.tabLive.setTextColor(getColor(R.color.text_primary))
                "VOD"  -> panelBinding.tabVod.setTextColor(getColor(R.color.text_primary))
            }
        }
        panelBinding.tabAll.setOnClickListener  { setTab("ALL") }
        panelBinding.tabLive.setOnClickListener { setTab("LIVE") }
        panelBinding.tabVod.setOnClickListener  { setTab("VOD") }
        setTab("ALL")

        // Search
        panelBinding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                entryAdapter.updateEntries(filteredEntries())
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    // ── Play Options Dialog ───────────────────────────────────────────────────

    private fun showPlayOptionsDialog(entry: M3uEntry) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_play_options, null)
        val db = com.dualsubtv.databinding.DialogPlayOptionsBinding.bind(dialogView)

        db.tvEntryName.text = entry.title

        val dialog = AlertDialog.Builder(this, R.style.Theme_DualSubTV_Player)
            .setView(dialogView)
            .create()

        // Show/hide sub1 external field
        db.rgDSub1.setOnCheckedChangeListener { _, id ->
            db.etDSub1.visibility = if (id == R.id.rbD1External) View.VISIBLE else View.GONE
        }
        db.rgDSub2.setOnCheckedChangeListener { _, id ->
            db.etDSub2.visibility = if (id == R.id.rbD2External) View.VISIBLE else View.GONE
        }

        db.btnDialogCancel.setOnClickListener { dialog.dismiss() }

        db.btnDialogPlay.setOnClickListener {
            val sub1Type = when (db.rgDSub1.checkedRadioButtonId) {
                R.id.rbD1Embedded -> PlayerConfig.SUB_EMBEDDED
                R.id.rbD1External -> PlayerConfig.SUB_EXTERNAL
                else              -> PlayerConfig.SUB_NONE
            }
            val sub2Type = when (db.rgDSub2.checkedRadioButtonId) {
                R.id.rbD2Embedded -> PlayerConfig.SUB_EMBEDDED
                R.id.rbD2External -> PlayerConfig.SUB_EXTERNAL
                else              -> PlayerConfig.SUB_NONE
            }

            val config = PlayerConfig(
                videoUri   = entry.url,
                sub1Type   = sub1Type,
                sub1Source = db.etDSub1.text.toString().trim(),
                sub2Type   = sub2Type,
                sub2Source = db.etDSub2.text.toString().trim()
            )

            dialog.dismiss()
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerConfig.EXTRA_KEY, config)
            })
        }

        dialog.show()
    }

    // ── Direct Play Panel ─────────────────────────────────────────────────────

    private fun showDirectPlayPanel() {
        setActiveNav(R.id.navDirectPlay)
        // Reuse the original MainActivity layout logic inline
        val mainBinding = com.dualsubtv.databinding.ActivityMainBinding.inflate(layoutInflater)
        binding.contentArea.removeAllViews()
        binding.contentArea.addView(mainBinding.root)
        // Wire up its own play button
        setupDirectPlayBindings(mainBinding)
    }

    private fun setupDirectPlayBindings(b: com.dualsubtv.databinding.ActivityMainBinding) {
        b.rgSub1Type.setOnCheckedChangeListener { _, id ->
            b.etSub1.visibility = if (id == R.id.rbSub1External) View.VISIBLE else View.GONE
            b.layoutSub1EmbeddedSelector.visibility = if (id == R.id.rbSub1Embedded) View.VISIBLE else View.GONE
        }
        b.rgSub2Type.setOnCheckedChangeListener { _, id ->
            b.etSub2.visibility = if (id == R.id.rbSub2External) View.VISIBLE else View.GONE
            b.layoutSub2EmbeddedSelector.visibility = if (id == R.id.rbSub2Embedded) View.VISIBLE else View.GONE
        }
        b.btnPlay.setOnClickListener {
            val url = b.etVideoUrl.text.toString().trim()
            if (url.isEmpty()) { b.etVideoUrl.error = "Ingresa una URL"; return@setOnClickListener }

            val sub1Type = when (b.rgSub1Type.checkedRadioButtonId) {
                R.id.rbSub1Embedded -> PlayerConfig.SUB_EMBEDDED
                R.id.rbSub1External -> PlayerConfig.SUB_EXTERNAL
                else -> PlayerConfig.SUB_NONE
            }
            val sub2Type = when (b.rgSub2Type.checkedRadioButtonId) {
                R.id.rbSub2Embedded -> PlayerConfig.SUB_EMBEDDED
                R.id.rbSub2External -> PlayerConfig.SUB_EXTERNAL
                else -> PlayerConfig.SUB_NONE
            }
            val config = PlayerConfig(
                videoUri = url,
                sub1Type = sub1Type, sub1Source = b.etSub1.text.toString().trim(),
                sub1TrackIndex = b.spinnerSub1Track.selectedItemPosition,
                sub2Type = sub2Type, sub2Source = b.etSub2.text.toString().trim(),
                sub2TrackIndex = b.spinnerSub2Track.selectedItemPosition
            )
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerConfig.EXTRA_KEY, config)
            })
        }
    }

    // ── File picker result ────────────────────────────────────────────────────

    private var currentAddBinding: FragmentAddPlaylistBinding? = null

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_M3U && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentAddBinding?.let { pb ->
                pb.tvSelectedFile.text = uri.lastPathSegment ?: uri.toString()
                pb.tvSelectedFile.visibility = View.VISIBLE
                // Store the URI string so it can be used when saving
                pb.btnSavePlaylist.tag = uri.toString()
            }
        }
    }

    companion object {
        const val REQUEST_PICK_M3U = 1001
    }
}
