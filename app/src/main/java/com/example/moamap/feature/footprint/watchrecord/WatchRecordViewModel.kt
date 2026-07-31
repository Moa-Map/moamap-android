package com.example.moamap.feature.footprint.watchrecord

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface WatchRecordUiState {
    data object Loading : WatchRecordUiState
    data class Success(val sessions: List<ReceivedWalkSession>) : WatchRecordUiState
}

@HiltViewModel
class WatchRecordViewModel @Inject constructor(
    private val fileStore: WalkSessionFileStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchRecordUiState>(WatchRecordUiState.Loading)
    val uiState: StateFlow<WatchRecordUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = WatchRecordUiState.Loading
        viewModelScope.launch {
            val sessions = withContext(Dispatchers.IO) { fileStore.loadAll() }
            _uiState.value = WatchRecordUiState.Success(sessions)
        }
    }

    /**
     * 기록 하나를 폰에서 지운다.
     *
     * 되돌릴 수 없다. 백엔드 업로드가 아직 없어 이 파일이 그 기록의 유일한 원본이다.
     *
     * 파일을 지운 뒤 목록을 다시 읽지 않고 화면에서 바로 덜어낸다. 다시 읽으면 목록이
     * Loading 을 거쳐 통째로 깜빡이는데, 지운 카드 하나가 빠지는 것뿐이라 그럴 이유가 없다.
     */
    fun deleteSession(session: ReceivedWalkSession) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileStore.delete(session.fileName) }

            _uiState.update { state ->
                if (state !is WatchRecordUiState.Success) return@update state
                state.copy(
                    sessions = state.sessions.filterNot { it.fileName == session.fileName },
                )
            }
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
