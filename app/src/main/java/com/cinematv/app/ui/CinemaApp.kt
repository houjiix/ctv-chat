package com.cinematv.app.ui

import android.content.Context
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil3.compose.AsyncImage
import com.cinematv.app.CinemaApplication
import com.cinematv.app.R
import com.cinematv.app.data.CatalogRepository
import com.cinematv.app.data.LicensePolicy
import com.cinematv.app.data.toFavoriteEntity
import com.cinematv.app.data.toMediaTitle as favoriteToMediaTitle
import com.cinematv.app.model.EpisodeInfo
import com.cinematv.app.model.MediaKind
import com.cinematv.app.model.MediaTitle
import com.cinematv.app.playback.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MainTab { HOME, MOVIES, SERIES, FAVORITES, SETTINGS }

@Composable
fun CinemaApp() {
    val context = LocalContext.current
    val app = context.applicationContext as CinemaApplication
    val repository = remember { CatalogRepository() }
    val scope = rememberCoroutineScope()
    val preferences by app.settings.preferences.collectAsState(initial = com.cinematv.app.data.UserPreferences())
    val favorites by app.database.cinemaDao().observeFavorites().collectAsState(initial = emptyList())
    val watchProgress by app.database.cinemaDao().observeWatchProgress().collectAsState(initial = emptyList())

    var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var selectedTitle by remember { mutableStateOf<MediaTitle?>(null) }
    var series by remember { mutableStateOf<List<MediaTitle>>(emptyList()) }
    var archiveFilms by remember { mutableStateOf<List<MediaTitle>>(emptyList()) }
    var seriesLoading by remember { mutableStateOf(true) }
    var filmsLoading by remember { mutableStateOf(true) }
    var seriesError by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<MediaTitle>>(emptyList()) }
    var searchError by remember { mutableStateOf(false) }

    val playVerified: (MediaTitle) -> Unit = { item ->
        scope.launch {
            val hasAllowedLicence = item.kind == MediaKind.MOVIE && LicensePolicy.isAllowed(item.licenceUrl)
            val reachable = hasAllowedLicence && item.playbackUrl?.let { repository.verifyPlayableUrl(it) } == true
            if (reachable) {
                launchPlayback(context, item, preferences.rememberPosition)
            } else {
                Toast.makeText(context, context.getString(R.string.playback_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            seriesLoading = true
            seriesError = false
            try {
                series = repository.loadSeries()
                seriesError = series.isEmpty()
            } catch (_: Exception) {
                seriesError = true
            } finally {
                seriesLoading = false
            }
        }
        launch {
            filmsLoading = true
            try {
                archiveFilms = repository.searchLicensedArchiveFilms()
            } catch (_: Exception) {
                archiveFilms = emptyList()
            } finally {
                filmsLoading = false
            }
        }
    }

    LaunchedEffect(query, activeTab) {
        val shouldSearch = query.isNotBlank() && activeTab in setOf(MainTab.MOVIES, MainTab.SERIES)
        if (!shouldSearch) {
            searchResults = emptyList()
            searching = false
            searchError = false
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        searchError = false
        try {
            searchResults = if (activeTab == MainTab.SERIES) {
                repository.searchSeries(query)
            } else {
                repository.searchLicensedArchiveFilms(query)
            }
            searchError = false
        } catch (_: Exception) {
            searchResults = emptyList()
            searchError = true
        } finally {
            searching = false
        }
    }

    BackHandler(enabled = selectedTitle != null) { selectedTitle = null }

    CinemaTheme(preferences.theme, preferences.reducedAnimations) {
        if (selectedTitle != null) {
            DetailScreen(
                item = selectedTitle!!,
                isFavorite = favorites.any { it.id == selectedTitle!!.id },
                liquidGlass = preferences.liquidGlass,
                onBack = { selectedTitle = null },
                onFavorite = {
                    val item = selectedTitle!!
                    scope.launch {
                        val dao = app.database.cinemaDao()
                        if (dao.isFavorite(item.id)) dao.removeFavorite(item.id)
                        else dao.addFavorite(item.toFavoriteEntity())
                    }
                },
                onPlay = playVerified,
                repository = repository
            )
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    MainNavigationBar(activeTab = activeTab, onTabSelected = {
                        activeTab = it
                        query = ""
                    })
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppHeader(activeTab)
                    if (activeTab == MainTab.MOVIES || activeTab == MainTab.SERIES) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    }

                    when (activeTab) {
                        MainTab.HOME -> HomeScreen(
                            series = series,
                            films = archiveFilms,
                            progress = watchProgress.map { it.toMediaTitle() },
                            loadingSeries = seriesLoading,
                            loadingFilms = filmsLoading,
                            seriesError = seriesError,
                            liquidGlass = preferences.liquidGlass,
                            onRetrySeries = {
                                scope.launch {
                                    seriesLoading = true
                                    seriesError = false
                                    try {
                                        series = repository.loadSeries()
                                        seriesError = series.isEmpty()
                                    } catch (_: Exception) {
                                        seriesError = true
                                    } finally {
                                        seriesLoading = false
                                    }
                                }
                            },
                            onOpen = { selectedTitle = it },
                            onPlay = playVerified,
                            onRemoveProgress = { entry -> scope.launch { app.database.cinemaDao().removeProgress(entry.id) } },
                            onSeeMovies = { activeTab = MainTab.MOVIES },
                            onSeeSeries = { activeTab = MainTab.SERIES }
                        )
                        MainTab.MOVIES -> CatalogScreen(
                            items = if (query.isBlank()) archiveFilms else searchResults,
                            loading = if (query.isBlank()) filmsLoading else searching,
                            error = searchError,
                            emptyText = stringResource(R.string.no_movies),
                            query = query,
                            liquidGlass = preferences.liquidGlass,
                            onRetry = {
                                scope.launch {
                                    if (query.isBlank()) {
                                        filmsLoading = true
                                        try { archiveFilms = repository.searchLicensedArchiveFilms() }
                                        finally { filmsLoading = false }
                                    } else {
                                        searching = true
                                        searchError = false
                                        try { searchResults = repository.searchLicensedArchiveFilms(query) }
                                        catch (_: Exception) { searchError = true }
                                        finally { searching = false }
                                    }
                                }
                            },
                            onOpen = { selectedTitle = it }
                        )
                        MainTab.SERIES -> CatalogScreen(
                            items = if (query.isBlank()) series else searchResults,
                            loading = if (query.isBlank()) seriesLoading else searching,
                            error = searchError || (query.isBlank() && seriesError),
                            emptyText = stringResource(R.string.no_series),
                            query = query,
                            liquidGlass = preferences.liquidGlass,
                            onRetry = {
                                scope.launch {
                                    if (query.isBlank()) {
                                        seriesLoading = true
                                        try {
                                            series = repository.loadSeries()
                                            seriesError = series.isEmpty()
                                        } catch (_: Exception) { seriesError = true }
                                        finally { seriesLoading = false }
                                    } else {
                                        searching = true
                                        searchError = false
                                        try { searchResults = repository.searchSeries(query) }
                                        catch (_: Exception) { searchError = true }
                                        finally { searching = false }
                                    }
                                }
                            },
                            onOpen = { selectedTitle = it }
                        )
                        MainTab.FAVORITES -> FavoritesScreen(
                            items = favorites.map { it.favoriteToMediaTitle() },
                            liquidGlass = preferences.liquidGlass,
                            onOpen = { selectedTitle = it }
                        )
                        MainTab.SETTINGS -> SettingsScreen(
                            preferences = preferences,
                            onTheme = { value -> scope.launch { app.settings.setTheme(value) } },
                            onLanguage = { value ->
                                scope.launch {
                                    app.settings.setLanguage(value)
                                    val tags = when (value) {
                                        "en" -> "en"
                                        "fr" -> "fr"
                                        "ar" -> "ar"
                                        else -> ""
                                    }
                                    AppCompatDelegate.setApplicationLocales(
                                        androidx.core.os.LocaleListCompat.forLanguageTags(tags)
                                    )
                                }
                            },
                            onGlass = { value -> scope.launch { app.settings.setLiquidGlass(value) } },
                            onReducedMotion = { value -> scope.launch { app.settings.setReducedAnimations(value) } },
                            onRememberPosition = { value -> scope.launch { app.settings.setRememberPosition(value) } },
                            onClearCache = {
                                scope.launch(Dispatchers.IO) {
                                    context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, context.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun launchPlayback(context: Context, item: MediaTitle, rememberPosition: Boolean) {
    // The caller supplies only URLs that passed source, licence and reachability checks.
    val url = item.playbackUrl ?: return
    val intent = Intent(context, PlayerActivity::class.java).apply {
        putExtra(PlayerActivity.EXTRA_URL, url)
        putExtra(PlayerActivity.EXTRA_TITLE, item.title)
        putExtra(PlayerActivity.EXTRA_ID, item.id)
        putExtra(PlayerActivity.EXTRA_POSTER, item.posterUrl)
        putExtra(PlayerActivity.EXTRA_LICENCE_URL, item.licenceUrl)
        putExtra(PlayerActivity.EXTRA_LICENCE_LABEL, item.licenceLabel)
        putExtra(PlayerActivity.EXTRA_RESUME_MS, if (rememberPosition) item.resumePositionMs ?: 0L else 0L)
    }
    context.startActivity(intent)
}


@Composable
private fun AppHeader(tab: MainTab) {
    val title = when (tab) {
        MainTab.HOME -> stringResource(R.string.home)
        MainTab.MOVIES -> stringResource(R.string.movies)
        MainTab.SERIES -> stringResource(R.string.series)
        MainTab.FAVORITES -> stringResource(R.string.favorites)
        MainTab.SETTINGS -> stringResource(R.string.settings)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("CINEMA", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text("TV", fontWeight = FontWeight.Light, fontSize = 22.sp)
        Spacer(Modifier.weight(1f))
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun MainNavigationBar(activeTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    val tabs = listOf(
        Triple(MainTab.HOME, R.string.home, Icons.Outlined.Home),
        Triple(MainTab.MOVIES, R.string.movies, Icons.Outlined.Movie),
        Triple(MainTab.SERIES, R.string.series, Icons.Outlined.Tv),
        Triple(MainTab.FAVORITES, R.string.favorites, Icons.Outlined.Favorite),
        Triple(MainTab.SETTINGS, R.string.settings, Icons.Outlined.Settings)
    )
    NavigationBar {
        tabs.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = stringResource(label)) },
                label = { Text(stringResource(label), maxLines = 1) }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    series: List<MediaTitle>,
    films: List<MediaTitle>,
    progress: List<MediaTitle>,
    loadingSeries: Boolean,
    loadingFilms: Boolean,
    seriesError: Boolean,
    liquidGlass: Boolean,
    onRetrySeries: () -> Unit,
    onOpen: (MediaTitle) -> Unit,
    onPlay: (MediaTitle) -> Unit,
    onRemoveProgress: (MediaTitle) -> Unit,
    onSeeMovies: () -> Unit,
    onSeeSeries: () -> Unit
) {
    val featured = films.firstOrNull() ?: series.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (featured != null) {
            item {
                FeatureBanner(featured, onOpen = onOpen, onPlay = onPlay, liquidGlass = liquidGlass)
            }
        } else if (loadingSeries || loadingFilms) {
            item { LoadingBlock() }
        } else if (seriesError) {
            item { ErrorBlock(stringResource(R.string.no_series), onRetrySeries) }
        }

        if (progress.isNotEmpty()) {
            item {
                MediaSection(
                    title = stringResource(R.string.continue_watching),
                    items = progress,
                    liquidGlass = liquidGlass,
                    onItem = onPlay,
                    onRemove = onRemoveProgress
                )
            }
        }

        item {
            MediaSection(
                title = stringResource(R.string.series_catalog),
                items = series.take(18),
                loading = loadingSeries,
                liquidGlass = liquidGlass,
                onItem = onOpen,
                onSeeAll = onSeeSeries
            )
        }

        item {
            MediaSection(
                title = stringResource(R.string.free_films),
                items = films,
                loading = loadingFilms,
                emptyText = stringResource(R.string.no_movies),
                liquidGlass = liquidGlass,
                onItem = onOpen,
                onSeeAll = onSeeMovies
            )
        }
    }
}

@Composable
private fun FeatureBanner(
    item: MediaTitle,
    onOpen: (MediaTitle) -> Unit,
    onPlay: (MediaTitle) -> Unit,
    liquidGlass: Boolean
) {
    val canPlay = item.kind == MediaKind.MOVIE && item.playbackUrl != null && LicensePolicy.isAllowed(item.licenceUrl)
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(if (liquidGlass) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(24.dp)) else Modifier)
            .clickable { onOpen(item) }.focusable()
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color(0xF0090B10), Color(0xB0090B10), Color(0x22090B10))
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                if (item.kind == MediaKind.MOVIE) stringResource(R.string.free_films) else "SERIES TO EXPLORE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(item.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(item.year, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Button(onClick = { if (canPlay) onPlay(item) else onOpen(item) }) {
                Text(stringResource(if (canPlay) R.string.watch_now else R.string.details))
            }
        }
    }
}

@Composable
private fun CatalogScreen(
    items: List<MediaTitle>,
    loading: Boolean,
    error: Boolean,
    emptyText: String,
    query: String,
    liquidGlass: Boolean,
    onRetry: () -> Unit,
    onOpen: (MediaTitle) -> Unit
) {
    when {
        loading -> LoadingBlock()
        error -> ErrorBlock(stringResource(R.string.no_internet), onRetry)
        items.isEmpty() -> EmptyBlock(if (query.isNotBlank()) stringResource(R.string.search_empty) else emptyText)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MediaListItem(item, liquidGlass, onClick = { onOpen(item) })
            }
        }
    }
}

@Composable
private fun FavoritesScreen(items: List<MediaTitle>, liquidGlass: Boolean, onOpen: (MediaTitle) -> Unit) {
    if (items.isEmpty()) {
        EmptyBlock(stringResource(R.string.empty_favorites))
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.id }) { item -> MediaListItem(item, liquidGlass) { onOpen(item) } }
        }
    }
}

@Composable
private fun MediaSection(
    title: String,
    items: List<MediaTitle>,
    loading: Boolean = false,
    emptyText: String? = null,
    liquidGlass: Boolean,
    onItem: (MediaTitle) -> Unit,
    onSeeAll: (() -> Unit)? = null,
    onRemove: ((MediaTitle) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (onSeeAll != null) TextButton(onClick = onSeeAll) { Text(stringResource(R.string.view_all)) }
        }
        when {
            loading -> LoadingRow()
            items.isEmpty() && emptyText != null -> Text(
                emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp),
                fontSize = 13.sp
            )
            items.isEmpty() -> Text(" ", modifier = Modifier.height(1.dp))
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Column(horizontalAlignment = Alignment.Start) {
                        MediaCard(item, liquidGlass) { onItem(item) }
                        if (onRemove != null) {
                            TextButton(onClick = { onRemove(item) }) { Text(stringResource(R.string.remove)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(item: MediaTitle, liquidGlass: Boolean, onClick: () -> Unit) {
    val motionModifier = if (LocalReducedMotion.current) Modifier else Modifier.animateContentSize()
    Column(
        modifier = Modifier.width(132.dp).then(motionModifier).clip(RoundedCornerShape(15.dp))
            .then(if (liquidGlass) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(15.dp)) else Modifier)
            .clickable(onClick = onClick).focusable()
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth().height(178.dp).clip(RoundedCornerShape(15.dp)),
            contentScale = ContentScale.Crop
        )
        Text(item.title, modifier = Modifier.padding(top = 7.dp, start = 3.dp, end = 3.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Text(item.year.ifBlank { if (item.kind == MediaKind.MOVIE) "Film" else "Series" }, modifier = Modifier.padding(start = 3.dp, top = 2.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        item.resumeProgress?.let { fraction ->
            androidx.compose.material3.LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun MediaListItem(item: MediaTitle, liquidGlass: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).focusable(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (liquidGlass) MaterialTheme.colorScheme.surface.copy(alpha = 0.78f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.width(88.dp).height(124.dp).clip(RoundedCornerShape(11.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(item.year, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.summary.ifBlank { item.genres.joinToString(" • ") }, maxLines = 3, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Box(Modifier.width(120.dp).height(165.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EmptyBlock(message: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailScreen(
    item: MediaTitle,
    isFavorite: Boolean,
    liquidGlass: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onPlay: (MediaTitle) -> Unit,
    repository: CatalogRepository
) {
    var episodes by remember(item.id) { mutableStateOf<List<EpisodeInfo>>(emptyList()) }
    var episodesLoading by remember(item.id) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(item.id) {
        if (item.kind == MediaKind.SERIES) {
            episodesLoading = true
            episodes = repository.loadEpisodes(item.id)
            episodesLoading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  ${stringResource(R.string.home)}") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onFavorite) {
                Icon(Icons.Outlined.Favorite, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (isFavorite) R.string.remove_favorite else R.string.save_favorite))
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(270.dp),
                    contentScale = ContentScale.Fit
                )
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(listOf(item.year, item.genres.joinToString(" · "), item.rating?.let { "★ $it" }).filterNotNull().filter(String::isNotBlank).joinToString("  • "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(item.summary.ifBlank { stringResource(R.string.movie_description) }, fontSize = 14.sp, lineHeight = 21.sp)
                    if (!item.licenceLabel.isNullOrBlank()) {
                        Text("${stringResource(R.string.licence)}: ${item.licenceLabel}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        item.licenceUrl?.let { licenceUrl ->
                            TextButton(onClick = { runCatching { uriHandler.openUri(licenceUrl) } }) {
                                Text("View licence terms", fontSize = 12.sp)
                            }
                        }
                    }
                    if (item.playbackUrl != null && LicensePolicy.isAllowed(item.licenceUrl)) {
                        Button(onClick = { onPlay(item) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text(stringResource(R.string.watch_now), fontSize = 16.sp)
                        }
                    } else {
                        Text(stringResource(R.string.not_available), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    if (item.creator != null) Text("Creator: ${item.creator}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.detailsUrl != null) {
                        TextButton(onClick = { runCatching { uriHandler.openUri(item.detailsUrl) } }) {
                            Text(if (item.kind == MediaKind.SERIES) "Open series information on TVmaze" else "Open Internet Archive record", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (item.kind == MediaKind.SERIES) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.episodes), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        if (episodesLoading) CircularProgressIndicator(Modifier.size(26.dp))
                    }
                }
                val groups = episodes.groupBy { it.season }.toSortedMap()
                groups.forEach { (seasonNumber, seasonEpisodes) ->
                    item(key = "season-$seasonNumber") {
                        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(stringResource(R.string.season, seasonNumber), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            seasonEpisodes.forEach { episode ->
                                Card(colors = CardDefaults.cardColors(containerColor = if (liquidGlass) MaterialTheme.colorScheme.surface.copy(alpha = .8f) else MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        AsyncImage(model = episode.imageUrl, contentDescription = null, modifier = Modifier.size(76.dp, 52.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                        Column(Modifier.weight(1f)) {
                                            Text("${episode.number}. ${episode.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(episode.airDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Info", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: com.cinematv.app.data.UserPreferences,
    onTheme: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onGlass: (Boolean) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onRememberPosition: (Boolean) -> Unit,
    onClearCache: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text(stringResource(R.string.settings_title), fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        item {
            SettingsCard(title = stringResource(R.string.appearance)) {
                ChoiceRow(
                    options = listOf("dark" to stringResource(R.string.dark), "black" to stringResource(R.string.black), "light" to stringResource(R.string.light)),
                    selected = preferences.theme,
                    onSelect = onTheme
                )
                PreferenceSwitch(stringResource(R.string.liquid_glass), preferences.liquidGlass, onGlass)
                PreferenceSwitch(stringResource(R.string.reduced_animations), preferences.reducedAnimations, onReducedMotion)
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.language)) {
                ChoiceRow(
                    options = listOf("auto" to stringResource(R.string.automatic), "en" to stringResource(R.string.english), "fr" to stringResource(R.string.french), "ar" to stringResource(R.string.arabic)),
                    selected = preferences.language,
                    onSelect = onLanguage
                )
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.playback)) {
                PreferenceSwitch(stringResource(R.string.remember_position), preferences.rememberPosition, onRememberPosition)
                Text(stringResource(R.string.provider_unavailable), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.storage)) {
                TextButton(onClick = onClearCache) { Text(stringResource(R.string.clear_cache)) }
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.about)) {
                Text(stringResource(R.string.about_description), fontSize = 13.sp, lineHeight = 19.sp)
                HorizontalDivider()
                Text(stringResource(R.string.tvmaze_credit), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.archive_credit), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("CINEMA TV 1.0.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            content()
        }
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (isSelected) 1.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(key) }.focusable().padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
