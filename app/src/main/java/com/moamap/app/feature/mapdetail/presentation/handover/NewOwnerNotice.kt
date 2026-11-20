package com.moamap.app.feature.mapdetail.presentation.handover

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moamap.app.core.auth.CurrentUserStore
import com.moamap.app.core.designsystem.component.MoaMapConfirmDialog
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.navigation.requireMapId
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지도마다 내가 지난번에 본 역할. 서버는 「방금 방장이 됐다」를 알려 주지 않아 앱이 기억해 두고 비교한다.
 *
 * shortcut: 기기에만 남는다. 앱을 다시 깔거나 다른 폰에서 보면 새 방장 팝업이 뜨지 않는다. 서버가 표시를 주면 그걸로 바꾼다.
 */
interface MapRoleHistory {

    /** [role] 을 저장하고, 그 전에 저장돼 있던 역할을 돌려준다. 처음이면 null 이다. */
    suspend fun replace(userId: Long, mapId: Long, role: MapRole): MapRole?
}

@Singleton
class SharedPreferencesMapRoleHistory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MapRoleHistory {

    private val preferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    override suspend fun replace(userId: Long, mapId: Long, role: MapRole): MapRole? = withContext(Dispatchers.IO) {
        // 같은 기기에서 다른 계정으로 바꿔 로그인해도 섞이지 않게 사람별로 나눈다.
        val key = "${userId}_$mapId"
        val previous = preferences.getString(key, null)?.let { name -> MapRole.entries.firstOrNull { it.name == name } }
        if (previous != role) preferences.edit { putString(key, role.name) }
        previous
    }

    private companion object {
        const val PREFERENCES_NAME = "map_role_history"
    }
}

/** 멤버·관리자였다가 방장이 됐는지. 처음 보는 지도(직접 만든 지도 포함)에서는 띄우지 않는다. */
internal fun becameOwner(previous: MapRole?, current: MapRole): Boolean =
    current == MapRole.Owner && (previous == MapRole.Member || previous == MapRole.Admin)

/**
 * 「이 지도의 새로운 방장이 되었어요!」(시안 `5152:75017`)를 띄울지 정한다. 지도 상세가 쓴다.
 *
 * 지도 상세 뷰모델에 얹지 않는다. 역할을 기억하는 일은 상세 조회와 상관없고, 상세 쪽 테스트가 이 저장소까지
 * 알아야 하게 된다.
 */
@HiltViewModel
internal class NewOwnerNoticeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currentUserStore: CurrentUserStore,
    private val history: MapRoleHistory,
) : ViewModel() {

    private val mapId: Long = savedStateHandle.requireMapId()

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    /** 지도를 읽을 때마다 부른다. 참여한 지도의 역할만 기억한다 - 나간 뒤 다시 들어온 경우를 지난 역할로 잇는다. */
    fun onRoleLoaded(joined: Boolean, role: MapRole) {
        if (!joined || role == MapRole.None) return
        viewModelScope.launch {
            val userId = currentUserStore.load() ?: return@launch
            val previous = history.replace(userId = userId, mapId = mapId, role = role)
            if (becameOwner(previous, role)) _visible.value = true
        }
    }

    fun dismiss() {
        _visible.value = false
    }
}

/** [닫기]와 [확인]은 둘 다 닫기만 한다(시안). */
@Composable
internal fun NewOwnerDialog(onDismiss: () -> Unit) {
    MoaMapConfirmDialog(
        title = "이 지도의 새로운 방장이 되었어요!",
        message = "지도 정보와 장소를 수정하고,\n멤버의 권한을 관리할 수 있어요",
        confirmText = "확인",
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
        dismissText = "닫기",
        dismissColor = MoaMapPrimitiveColors.Gray200,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NewOwnerDialogPreview() {
    MoaMapTheme {
        NewOwnerDialog(onDismiss = {})
    }
}
