package com.example.moamap.feature.footprint.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moamap.core.walksession.WalkSessionJson
import com.example.moamap.feature.footprint.data.WalkSessionFileStore
import com.example.moamap.feature.footprint.domain.model.ReceivedWalkSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface WearDebugUiState {
    data object Loading : WearDebugUiState
    data class Success(val sessions: List<ReceivedWalkSession>) : WearDebugUiState
}

@HiltViewModel
class WearDebugViewModel @Inject constructor(
    private val fileStore: WalkSessionFileStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WearDebugUiState>(WearDebugUiState.Loading)
    val uiState: StateFlow<WearDebugUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = WearDebugUiState.Loading
        viewModelScope.launch {
            val sessions = withContext(Dispatchers.IO) { fileStore.loadAll() }
            _uiState.value = WearDebugUiState.Success(sessions)
        }
    }

    /**
     * 공유용으로 사람이 읽을 수 있게 pretty-print 한 JSON 을 캐시 디렉터리에 써서 돌려준다.
     * 저장소에 쌓인 원본(compact) 파일은 건드리지 않는다.
     */
    suspend fun exportForShare(session: ReceivedWalkSession): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "walk-session-exports").apply { mkdirs() }
        val exportFile = File(exportDir, session.fileName)
        exportFile.writeText(WalkSessionJson.encodeToPrettyString(session.payload))
        exportFile
    }
}
