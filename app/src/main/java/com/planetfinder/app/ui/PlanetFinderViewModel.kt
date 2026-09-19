package com.planetfinder.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planetfinder.app.data.AstronomyRepository
import com.planetfinder.app.data.FavoritesStore
import com.planetfinder.app.data.LocationStore
import com.planetfinder.app.data.ObserverLocation
import com.planetfinder.app.data.SkyPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class PlanetFinderUiState(
    val location: ObserverLocation,
    val positions: List<SkyPosition> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val observationTimeMillis: Long = System.currentTimeMillis(),
    val isLive: Boolean = true,
    val isCalculating: Boolean = true,
)

class PlanetFinderViewModel(application: Application) : AndroidViewModel(application) {
    private val favoritesStore = FavoritesStore(application)
    private val locationStore = LocationStore(application)
    private val _uiState = MutableStateFlow(
        PlanetFinderUiState(
            location = locationStore.load(),
            favorites = favoritesStore.load(),
        )
    )
    val uiState: StateFlow<PlanetFinderUiState> = _uiState.asStateFlow()
    private var calculationJob: Job? = null

    init {
        refreshPositions()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (_uiState.value.isLive) {
                    _uiState.update { it.copy(observationTimeMillis = System.currentTimeMillis()) }
                    refreshPositions()
                }
            }
        }
    }

    fun setLocation(location: ObserverLocation) {
        locationStore.save(location)
        _uiState.update { it.copy(location = location) }
        refreshPositions()
    }

    fun toggleFavorite(bodyId: String) {
        val favorites = _uiState.value.favorites.let { if (bodyId in it) it - bodyId else it + bodyId }
        favoritesStore.save(favorites)
        _uiState.update { it.copy(favorites = favorites) }
    }

    fun useLiveTime() {
        _uiState.update {
            it.copy(observationTimeMillis = System.currentTimeMillis(), isLive = true)
        }
        refreshPositions()
    }

    fun shiftObservationTime(hours: Int) {
        val state = _uiState.value
        val base = if (state.isLive) System.currentTimeMillis() else state.observationTimeMillis
        _uiState.update {
            it.copy(observationTimeMillis = base + hours * 60 * 60 * 1000L, isLive = false)
        }
        refreshPositions()
    }

    fun setObservationTime(millis: Long) {
        _uiState.update { it.copy(observationTimeMillis = millis, isLive = false) }
        refreshPositions()
    }

    fun useTonight() {
        val now = ZonedDateTime.now()
        val tonight = now.toLocalDate().atTime(20, 0).atZone(now.zone)
        _uiState.update {
            it.copy(observationTimeMillis = tonight.toInstant().toEpochMilli(), isLive = false)
        }
        refreshPositions()
    }

    private fun refreshPositions() {
        calculationJob?.cancel()
        val location = _uiState.value.location
        val time = _uiState.value.observationTimeMillis
        _uiState.update { it.copy(isCalculating = true) }
        calculationJob = viewModelScope.launch(Dispatchers.Default) {
            val positions = AstronomyRepository.positions(location, time)
            val current = _uiState.value
            if (current.location == location && current.observationTimeMillis == time) {
                _uiState.update { it.copy(positions = positions, isCalculating = false) }
            }
        }
    }
}
