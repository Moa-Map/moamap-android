package com.moamap.app.feature.mapdetail.presentation.info

import com.moamap.app.feature.collection.domain.model.CreatedMap
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.collection.domain.model.NewMap
import com.moamap.app.feature.collection.domain.repository.MapRepository
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapInfoEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val repository = FakeMapInfoRepository()

    private val viewModel = MapInfoEditViewModel(repository)

    private val state get() = viewModel.uiState.value

    private val map = MapDetail(
        id = 7L,
        title = "숭실대 주변 맛집",
        description = "학교 주변 맛집",
        imageUrl = "https://cdn/old.jpg",
        ownerName = "모아",
        type = MapType.Community,
        role = MapRole.Owner,
        tags = listOf("맛집", "점심"),
        memberCount = 3,
        placeCount = 12,
        joined = true,
        personal = false,
        inviteCode = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `열면 지금 지도 값이 채워져 있고 아직 저장할 수 없다`() {
        viewModel.open(map)

        val form = checkNotNull(state.form)
        assertEquals("숭실대 주변 맛집", form.name)
        assertEquals("학교 주변 맛집", form.description)
        assertEquals(listOf("맛집", "점심"), form.tags)
        assertEquals("https://cdn/old.jpg", form.photo)
        assertFalse(state.canSave)
    }

    @Test
    fun `하나라도 바꾸면 저장할 수 있고 되돌리면 다시 막힌다`() {
        viewModel.open(map)

        viewModel.updateName("숭실대 맛집")
        assertTrue(state.canSave)

        viewModel.updateName("숭실대 주변 맛집")
        assertFalse(state.canSave)

        viewModel.removeTag("점심")
        assertTrue(state.canSave)
    }

    @Test
    fun `이름을 비우면 저장할 수 없다`() {
        viewModel.open(map)

        viewModel.updateName("  ")

        assertFalse(state.canSave)
    }

    @Test
    fun `스페이스로는 태그를 확정하지 않고 엔터로 확정한다`() {
        viewModel.open(map)

        viewModel.updateTagInput("학교 앞")
        assertEquals(listOf("맛집", "점심"), state.form?.tags)

        viewModel.commitTag()
        assertEquals(listOf("맛집", "점심", "학교 앞"), state.form?.tags)
        assertEquals("", state.form?.tagInput)
    }

    @Test
    fun `고치지 않은 값도 처음 값 그대로 함께 보낸다`() = runTest(dispatcher) {
        viewModel.open(map)
        viewModel.updateDescription("새 설명")

        viewModel.save()
        advanceUntilIdle()

        // 서버가 통째로 덮어써서 빠진 값은 지워진다.
        assertEquals(
            listOf(UpdateCall(7L, "숭실대 주변 맛집", "새 설명", "https://cdn/old.jpg", listOf("맛집", "점심"))),
            repository.updates,
        )
        assertTrue(repository.uploadedUris.isEmpty())
    }

    @Test
    fun `새 사진은 올린 뒤 받은 주소로 저장한다`() = runTest(dispatcher) {
        viewModel.open(map)
        viewModel.selectPhoto("content://new")
        assertEquals("content://new", state.form?.photo)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("content://new"), repository.uploadedUris)
        assertEquals("https://cdn/new.jpg", repository.updates.single().imageUrl)
    }

    @Test
    fun `엔터 없이 입력만 한 태그도 저장할 때 확정해 보낸다`() = runTest(dispatcher) {
        viewModel.open(map)
        viewModel.updateTagInput("데이트")
        assertTrue(state.canSave)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("맛집", "점심", "데이트"), repository.updates.single().tags)
    }

    @Test
    fun `저장하면 화면을 닫고 저장 횟수를 올린다`() = runTest(dispatcher) {
        viewModel.open(map)
        viewModel.updateName("새 이름")

        viewModel.save()
        advanceUntilIdle()

        assertNull(state.form)
        assertEquals(1, state.savedCount)
    }

    @Test
    fun `저장이 실패하면 입력을 그대로 두고 안내한다`() = runTest(dispatcher) {
        repository.updateFailure = IllegalStateException("[403] MAP_008")
        viewModel.open(map)
        viewModel.updateName("새 이름")

        viewModel.save()
        advanceUntilIdle()

        assertEquals("새 이름", state.form?.name)
        assertFalse(state.saving)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.savedCount)
    }

    @Test
    fun `저장만 실패해 다시 누르면 같은 사진을 또 올리지 않는다`() = runTest(dispatcher) {
        repository.updateFailure = IllegalStateException("서버 오류")
        viewModel.open(map)
        viewModel.selectPhoto("content://new")
        viewModel.save()
        advanceUntilIdle()

        repository.updateFailure = null
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("content://new"), repository.uploadedUris)
        assertEquals("https://cdn/new.jpg", repository.updates.last().imageUrl)
    }

    @Test
    fun `저장 중에는 닫지 않는다`() = runTest(dispatcher) {
        viewModel.open(map)
        viewModel.updateName("새 이름")

        viewModel.save()
        viewModel.close()

        assertNotNull(state.form)
        advanceUntilIdle()
        assertNull(state.form)
    }
}

private data class UpdateCall(
    val mapId: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val tags: List<String>,
)

private class FakeMapInfoRepository : MapRepository {

    val updates = mutableListOf<UpdateCall>()
    val uploadedUris = mutableListOf<String>()
    var updateFailure: Throwable? = null

    override suspend fun uploadCoverImage(imageUri: String): String {
        uploadedUris += imageUri
        return "https://cdn/new.jpg"
    }

    override suspend fun updateMap(
        mapId: Long,
        name: String,
        description: String?,
        imageUrl: String?,
        tags: List<String>,
    ) {
        updates += UpdateCall(mapId, name, description, imageUrl, tags)
        updateFailure?.let { throw it }
    }

    override suspend fun getMyMaps(type: MapType) = TODO("사용하지 않음")
    override suspend fun updateMyMapOrder(type: MapType, mapIds: List<Long>) = TODO("사용하지 않음")
    override suspend fun createMap(newMap: NewMap): CreatedMap = TODO("사용하지 않음")
    override suspend fun joinByInviteCode(inviteCode: String) = TODO("사용하지 않음")
    override suspend fun getLeaveOutcome(mapId: Long) = TODO("사용하지 않음")
    override suspend fun leaveMap(mapId: Long) = TODO("사용하지 않음")
}
