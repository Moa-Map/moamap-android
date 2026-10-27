package com.moamap.app.feature.collection.presentation.placeimport

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.common.NETWORK_ERROR_MESSAGE
import com.moamap.app.core.common.actionableMessage
import com.moamap.app.core.common.placeAddedToast
import com.moamap.app.core.common.toUserMessage
import com.moamap.app.core.common.toggle
import com.moamap.app.core.common.upload.ImageUploadException
import com.moamap.app.core.navigation.MoaMapRoute
import com.moamap.app.core.network.ApiException
import com.moamap.app.core.network.ConnectionException
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.MyMap
import com.moamap.app.feature.collection.domain.model.PlaceEdit
import com.moamap.app.feature.collection.domain.model.PlaceExtractionException
import com.moamap.app.feature.collection.domain.model.PlaceImportSource
import com.moamap.app.feature.collection.domain.model.PlaceSaveResult
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.collection.domain.repository.PlaceImportRepository
import com.moamap.app.feature.collection.presentation.MyMapsState
import com.moamap.app.feature.collection.share.SharedLink
import com.moamap.app.feature.collection.share.SharedLinkParser
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.domain.model.PlaceCandidate
import com.moamap.app.feature.mapdetail.domain.repository.PlaceSearchRepository
import com.moamap.app.feature.mapdetail.presentation.addplace.MAX_PLACE_PHOTOS
import com.moamap.app.feature.mapdetail.presentation.addplace.PlaceSearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlaceImportViewModel"
/** 시안 「상세지도/장소추가 - 에러」와 같은 문구다. 링크 입력 화면 안내 자리에 [다시 시도]와 함께 뜬다. */
private const val DEFAULT_EXTRACTION_ERROR = "검색 결과를 불러오지 못했어요"
private const val SEARCH_FAILED_MESSAGE = "검색 결과를 불러오지 못했어요"
private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val MAPS_LOAD_FAILED_MESSAGE = "지도 목록을 불러오지 못했어요"
private const val DEFAULT_SAVE_ERROR = "장소를 저장하지 못했어요"

/**
 * 장소 가져오기 4단계가 공유하는 ViewModel.
 *
 * 중첩 그래프의 back stack entry 에 스코프해서 4개 화면이 같은 인스턴스를 본다.
 * 흐름을 벗어나면 함께 정리되므로 다음에 다시 들어와도 이전 입력이 남지 않는다.
 */

@HiltViewModel
internal class PlaceImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeImportRepository: PlaceImportRepository,
    private val mapRepository: MapRepository,
    private val placeSearchRepository: PlaceSearchRepository,
) : ViewModel() {

    private val source: PlaceImportSource = PlaceImportSource.valueOf(
        requireNotNull(savedStateHandle[MoaMapRoute.PlaceImport.ARG_SOURCE]),
    )

    /** 다른 앱에서 공유로 들어온 링크. 모음 탭으로 들어오면 비어 있다. */
    private val sharedUrl: String = savedStateHandle[MoaMapRoute.PlaceImport.ARG_URL] ?: ""

    private val _uiState = MutableStateFlow(PlaceImportUiState(source = source, url = sharedUrl))
    val uiState: StateFlow<PlaceImportUiState> = _uiState.asStateFlow()

    private var extractionJob: Job? = null

    private var mapsJob: Job? = null

    private var searchJob: Job? = null

    init {
        loadTargetMaps()
    }

    /**
     * 재시도를 취소했을 때 되돌아갈 직전 결과.
     *
     * 재시도는 이미 목록을 보고 있는 상태에서 시작하므로, 취소하면 보던 목록으로 돌아가야 한다.
     * 그냥 비워버리면 취소한 사용자가 결과를 잃고 URL 입력부터 다시 해야 한다.
     */
    private var previousResult: Pair<ExtractionState.Success, Set<String>>? = null

    /** 한 줄 칸이라 붙여넣은 줄바꿈은 띄어쓰기로 바꾼다. 다시 입력하면 링크 오류 표시는 지운다. */
    fun updateUrl(url: String) {
        // 불러오기 오류도 입력칸 아래 안내 자리에 남아 있다. 다시 입력하면 함께 지운다.
        _uiState.update { state ->
            state.copy(url = url.replace('\n', ' '), linkInvalid = false, errorMessage = null)
        }
    }

    /**
     * 링크 입력 화면의 키보드 엔터.
     *
     * 이 흐름이 받는 링크 형식이면(인스타그램 게시물 / 네이버·카카오·구글 지도) 앞뒤 글을 떼고 링크만 남겨
     * 추출을 시작하고 true 를 준다. 아니면 입력칸에 오류를 띄우고 false 다(시안 「양식 오류」).
     */
    fun submitUrl(): Boolean {
        val current = _uiState.value
        if (current.url.isBlank()) return false

        val link = importLinkOf(current.url, source)
        if (link == null) {
            _uiState.update { state -> state.copy(linkInvalid = true) }
            return false
        }
        _uiState.update { state -> state.copy(url = link) }
        startExtraction()
        return true
    }

    /** 검색하기와 재시도가 함께 쓴다. */
    fun startExtraction() {
        val current = _uiState.value
        if (!current.canSearch) return

        extractionJob?.cancel()
        previousResult = (current.extraction as? ExtractionState.Success)
            ?.let { success -> success to current.selectedPlaceIds }

        _uiState.update { state ->
            state.copy(
                extraction = ExtractionState.Loading,
                selectedPlaceIds = emptySet(),
                errorMessage = null,
                linkInvalid = false,
            )
        }

        extractionJob = viewModelScope.launch {
            val result = try {
                when (source) {
                    PlaceImportSource.Instagram ->
                        ExtractionState.Success(placeImportRepository.extractPlaces(current.url))

                    PlaceImportSource.MapShare ->
                        placeImportRepository.extractMapSharePlaces(current.url).let { imported ->
                            ExtractionState.Success(imported.places, imported.unmatchedNames)
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "장소 추출 실패", throwable)
                failExtraction(throwable)
                return@launch
            }

            // 새 결과가 나왔으니 되돌릴 대상도 사라진다.
            previousResult = null
            _uiState.update { state ->
                state.copy(
                    extraction = result,
                    selectedPlaceIds = initialSelection(result.places),
                )
            }
        }
    }

    /**
     * 로딩 중 뒤로가기. 진행 중이던 작업을 취소하고 직전 상태로 되돌린다.
     *
     * 진행 중이 아닐 때는 아무것도 하지 않는다. 이미 나온 결과를 지워버리면 안 된다.
     */
    fun cancelExtraction() {
        if (_uiState.value.extraction !is ExtractionState.Loading) return

        extractionJob?.cancel()
        extractionJob = null

        val restored = previousResult
        previousResult = null

        _uiState.update { state ->
            if (restored == null) {
                state.copy(extraction = ExtractionState.Idle)
            } else {
                state.copy(extraction = restored.first, selectedPlaceIds = restored.second)
            }
        }
    }

    /**
     * 추출에 실패해도 보고 있던 목록은 유지하고 안내만 띄운다.
     *
     * 결과를 지워버리면 재시도가 한 번 실패했다는 이유로 사용자가 처음부터 다시 해야 한다.
     *
     * 링크 탓이면 아래 안내 대신 입력칸에 오류를 띄운다. 네트워크·서버 탓은 링크를 고쳐도 소용없어 안내로 둔다.
     */
    private fun failExtraction(throwable: Throwable) {
        val restored = previousResult
        previousResult = null
        val linkProblem = throwable.isLinkProblem()

        _uiState.update { state ->
            state.copy(
                extraction = restored?.first ?: ExtractionState.Idle,
                selectedPlaceIds = restored?.second.orEmpty(),
                errorMessage = if (linkProblem) null else throwable.toUserMessage(),
                linkInvalid = linkProblem,
            )
        }
    }

    /**
     * 목록이 막 나왔을 때의 선택 상태.
     *
     * 외부 지도는 리스트를 통째로 가져오는 것이라 전부 고른 채로 시작하고 뺄 것만 해제하게 한다.
     * 인스타그램은 영상에서 찾은 후보라 사용자가 맞는 곳을 직접 고른다.
     */
    private fun initialSelection(places: List<ImportedPlace>): Set<String> = when (source) {
        PlaceImportSource.Instagram -> emptySet()

        PlaceImportSource.MapShare -> places
            .filter { place -> place.savable }
            .mapTo(mutableSetOf()) { place -> place.id }
    }

    /** 안내를 보여준 뒤 호출한다. 같은 메시지가 다시 뜨지 않게 한다. */
    fun consumeError() {
        _uiState.update { state -> state.copy(errorMessage = null) }
    }

    /** 한 링크에서 나온 장소를 여러 개 가져갈 수 있으므로 장소도 토글이다. */
    fun togglePlace(placeId: String) {
        _uiState.update { state ->
            // 등록 키가 없는 후보는 골라도 서버가 거절한다. 아예 선택되지 않게 둔다.
            if (state.places.none { place -> place.id == placeId && place.savable }) {
                return@update state
            }

            state.copy(selectedPlaceIds = state.selectedPlaceIds.toggle(placeId))
        }
    }

    fun updateEditTags(placeId: String, tags: List<String>) {
        updateEdit(placeId) { edit -> edit.copy(tags = tags) }
    }

    fun updateEditMemo(placeId: String, memo: String) {
        updateEdit(placeId) { edit -> edit.copy(memo = memo) }
    }

    /** 서버가 한 장소에 [MAX_PLACE_PHOTOS] 장까지 받는다. 같은 사진을 두 번 넣지도 않는다. */
    fun addEditPhoto(placeId: String, uri: Uri) {
        updateEdit(placeId) { edit ->
            if (edit.photos.size >= MAX_PLACE_PHOTOS || uri in edit.photos) {
                edit
            } else {
                edit.copy(photos = edit.photos + uri)
            }
        }
    }

    fun removeEditPhoto(placeId: String, uri: Uri) {
        updateEdit(placeId) { edit -> edit.copy(photos = edit.photos - uri) }
    }

    /**
     * 사진 목록이 바뀌면 올려 둔 주소를 버린다.
     *
     * 남겨두면 저장에 한 번 실패한 뒤 사진을 바꿔도 예전 주소가 그대로 등록된다.
     */
    private fun PlaceImportUiState.discardUploadsIfPhotosChanged(
        before: PlaceEdit,
        after: PlaceEdit,
    ): PlaceImportUiState =
        if (before.photos == after.photos) this else copy(uploadedPhotoUrls = emptyMap())

    /**
     * 편집값을 고친다.
     *
     * 아직 편집한 적 없는 장소는 [PlaceImportUiState.editOf] 가 만든 기본값에서 시작한다.
     * 외부 지도로 가져온 장소의 기존 메모가 그 기본값에 들어 있어, 사진만 붙여도 메모가
     * 사라지지 않는다.
     */
    private fun updateEdit(placeId: String, transform: (PlaceEdit) -> PlaceEdit) {
        _uiState.update { state ->
            val place = state.places.firstOrNull { place -> place.id == placeId }
                ?: return@update state
            val before = state.editOf(place)
            val after = transform(before)
            state.copy(edits = state.edits + (placeId to after))
                .discardUploadsIfPhotosChanged(before, after)
        }
    }

    /** 지도 목록을 읽지 못했을 때 다시 읽는다. */
    fun retryLoadMaps() = loadTargetMaps()

    // ---------- 장소를 못 찾았을 때 직접 검색 ----------

    /** 장소 추가와 같은 카카오 장소 검색. 입력이 멈추면 찾는다. */
    fun updateSearchQuery(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { state -> state.copy(search = PlaceSearchState.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            _uiState.update { state -> state.copy(search = PlaceSearchState.Loading) }
            search(query)
        }
    }

    fun retrySearch() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return
        searchJob?.cancel()
        _uiState.update { state -> state.copy(search = PlaceSearchState.Loading) }
        searchJob = viewModelScope.launch { search(query) }
    }

    private suspend fun search(query: String) {
        val next = try {
            PlaceSearchState.Success(placeSearchRepository.search(query))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "장소 검색 실패", e)
            PlaceSearchState.Error(e.toUserMessage(SEARCH_FAILED_MESSAGE))
        }
        _uiState.update { state -> state.copy(search = next) }
    }

    /** 검색에서 고른 장소를 영상에서 찾아 고른 것처럼 둔다. 이어서 편집 → 지도 고르기로 간다. */
    fun selectSearchedPlace(candidate: PlaceCandidate) {
        val place = candidate.toImportedPlace()
        _uiState.update { state ->
            state.copy(
                extraction = ExtractionState.Success(listOf(place)),
                selectedPlaceIds = setOf(place.id),
                edits = emptyMap(),
                uploadedPhotoUrls = emptyMap(),
            )
        }
    }

    /**
     * 장소를 넣을 지도. 시안대로 나만의 지도 → 프라이빗 지도 → 내가 방장인 커뮤니티 지도 순이다
     * (커뮤니티는 방장만 넣는다, 이슈 #386·#390). 인스타그램과 외부 지도가 같다.
     */
    private fun loadTargetMaps() {
        mapsJob?.cancel()
        _uiState.update { state -> state.copy(targetMaps = MyMapsState.Loading) }

        mapsJob = viewModelScope.launch {
            val maps = try {
                coroutineScope {
                    val community = async { mapRepository.getMyMaps(MapType.Community) }
                    val private = mapRepository.getMyMaps(MapType.Private)
                    importTargetMaps(private, community.await())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.w(TAG, "저장할 지도 목록 조회 실패", throwable)
                _uiState.update { state ->
                    state.copy(targetMaps = MyMapsState.Error(MAPS_LOAD_FAILED_MESSAGE))
                }
                return@launch
            }

            _uiState.update { state -> state.copy(targetMaps = MyMapsState.Success(maps)) }
        }
    }

    /**
     * 고른 장소를 고른 지도에 등록한다.
     *
     * 성공하면 [PlaceImportUiState.saveResult] 가 채워지고, 그 값을 보고 흐름을 빠져나간다.
     * 실패하면 고른 것을 그대로 둔 채 안내만 띄워 같은 자리에서 다시 누를 수 있게 한다.
     */
    fun savePlaces() {
        val current = _uiState.value
        if (!current.canSave || current.selectedPlaces.isEmpty()) return
        val entries = current.selectedEntries

        // 발급 권한만 확인하는 값이라 고른 지도 중 아무거나면 된다. 받은 주소는 모든 지도에 쓴다.
        val photoMapId = current.selectedMapIds.first()

        _uiState.update { state -> state.copy(saving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = try {
                val photoUrls = current.uploadedPhotoUrls.ifEmpty {
                    placeImportRepository.uploadPhotos(photoMapId, entries).also { uploaded ->
                        _uiState.update { state -> state.copy(uploadedPhotoUrls = uploaded) }
                    }
                }
                placeImportRepository.savePlaces(current.selectedMapIds, entries, photoUrls)
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                Log.e(TAG, "장소 저장 실패", throwable)
                _uiState.update { state ->
                    state.copy(saving = false, errorMessage = throwable.toSaveMessage())
                }
                return@launch
            }

            _uiState.update { state ->
                // 한 곳도 들어가지 않았는데 저장된 것처럼 흐름을 닫으면 안 된다.
                if (result.created == 0) {
                    state.copy(saving = false, errorMessage = result.toEmptyMessage())
                } else {
                    // 프라이빗 지도와 내가 방장인 커뮤니티 지도만 고를 수 있어 승인 없이 바로 들어간다 - [importTargetMaps].
                    state.copy(saving = false, saveResult = result, doneToast = placeAddedToast(needsApproval = false))
                }
            }
        }
    }

    /** 한 장소를 여러 지도에 넣을 수 있으므로 지도는 토글이다. */
    fun toggleMap(mapId: Long) {
        _uiState.update { state ->
            state.copy(selectedMapIds = state.selectedMapIds.toggle(mapId))
        }
    }
}

/** 등록된 것이 하나도 없을 때의 안내. 전부 중복인 경우와 진짜 실패를 구분한다. */
private fun PlaceSaveResult.toEmptyMessage(): String =
    if (failed == 0) "이미 저장되어 있는 장소예요" else DEFAULT_SAVE_ERROR

private fun Throwable.toSaveMessage(): String = when (this) {
    // 사진 형식·크기는 사용자가 사진을 바꿔야 하는 일이라 이유를 그대로 보여준다.
    is ImageUploadException -> message ?: DEFAULT_SAVE_ERROR
    // 권한이나 정원 같은 등록 거절 사유는 사용자가 조치할 수 있어 그대로 노출한다. 서버 내부 오류 문구는 숨긴다.
    is ApiException -> actionableMessage() ?: DEFAULT_SAVE_ERROR
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> DEFAULT_SAVE_ERROR
}

/** 서버 `UNSUPPORTED_SHARE_LINK`. 지도 공유 링크가 아니다. */
private const val UNSUPPORTED_SHARE_LINK_CODE = "PLACE_015"

/** 링크를 고쳐야 풀리는 실패인지. 인스타그램 게시물을 못 읽었거나 서버가 지원하지 않는 공유 링크라고 답했다. */
private fun Throwable.isLinkProblem(): Boolean =
    this is PlaceExtractionException.CaptionUnavailable ||
        (this is ApiException && code == UNSUPPORTED_SHARE_LINK_CODE)

/**
 * 입력한 글에서 이 흐름이 받는 링크를 뽑는다. 다른 앱 공유와 같은 판정이다 - [SharedLinkParser].
 * 인스타그램 흐름에 지도 링크를 넣는 것처럼 흐름이 다르면 null 이다.
 */
internal fun importLinkOf(text: String, source: PlaceImportSource): String? =
    (SharedLinkParser.parse(text) as? SharedLink.Supported)?.takeIf { link -> link.source == source }?.url

/**
 * 가져온 장소를 넣을 수 있는 지도. 나만의 지도 → 프라이빗 지도 → 내가 방장인 커뮤니티 지도 순이고, 묶음 안은
 * 모음 탭 순서 그대로다. 커뮤니티 탭에 함께 오는 공식지도는 뺀다.
 */
internal fun importTargetMaps(private: List<MyMap>, community: List<MyMap>): List<MyMap> =
    private.sortedBy { map -> !map.personal } +
        community.filter { map -> !map.official && map.role == MapRole.Owner }

/** 직접 검색해 고른 장소의 출처. 장소 추가와 같다. */
internal const val SEARCHED_PLACE_SOURCE_TYPE = "KAKAO_SEARCH"

/** 카카오 검색 결과를 링크에서 찾은 장소와 같은 모양으로. 장소 추가와 같은 출처로 등록한다. */
internal fun PlaceCandidate.toImportedPlace() = ImportedPlace(
    id = kakaoPlaceId,
    name = name,
    address = address,
    roadAddress = roadAddress,
    lat = latitude,
    lng = longitude,
    category = category,
    kakaoPlaceId = kakaoPlaceId,
    sourceType = SEARCHED_PLACE_SOURCE_TYPE,
    sourceUrl = placeUrl,
)

private fun Throwable.toUserMessage(): String = when (this) {
    // 캡션을 못 읽은 이유는 사용자가 조치할 수 있는 내용이라 그대로 노출한다.
    is PlaceExtractionException -> message ?: DEFAULT_EXTRACTION_ERROR
    is ApiException -> actionableMessage() ?: DEFAULT_EXTRACTION_ERROR
    is ConnectionException -> NETWORK_ERROR_MESSAGE
    else -> DEFAULT_EXTRACTION_ERROR
}
