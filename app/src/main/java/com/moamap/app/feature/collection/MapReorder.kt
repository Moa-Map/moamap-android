package com.moamap.app.feature.collection

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlin.math.abs

/**
 * 모음 편집에서 손잡이로 카드 순서를 바꾸는 끌기 상태.
 *
 * 목록이 스크롤 되는 `Column` 안에 있어 `LazyColumn` 용 라이브러리를 쓰지 않고 직접 둔다.
 * 끄는 카드는 손가락을 따라 [dragOffset] 만큼 떠 있다가, 이웃 카드의 절반을 넘으면
 * [onMove] 로 순서를 바꾸고 그만큼 오프셋을 되돌린다. 목록 끝을 넘어서는 따라가지 않는다.
 * 순서 자체는 부르는 쪽이 들고 있다.
 */
@Stable
internal class MapReorderState(
    private val spacingPx: Float,
    private val onMove: (mapId: Long, targetId: Long) -> Unit,
) {
    var draggingId by mutableStateOf<Long?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set

    /**
     * 끄는 동안 쓰는 순서. [onMove] 가 반영돼 다시 그려지기 전에도 다음 이웃을 맞게 찾으려고
     * 여기서도 바꿔 둔다. 끌지 않을 때 [sync] 로 화면 순서와 맞춘다.
     */
    private var order: List<Long> = emptyList()
    private val heights = mutableMapOf<Long, Int>()

    fun sync(ids: List<Long>) {
        if (draggingId == null) order = ids
    }

    fun onSizeChanged(mapId: Long, height: Int) {
        heights[mapId] = height
    }

    /** 이미 다른 카드를 끄는 중이면(두 손가락으로 손잡이 둘을 잡는 등) 무시한다. */
    fun start(mapId: Long) {
        if (draggingId != null) return
        draggingId = mapId
        dragOffset = 0f
    }

    /** 끄는 카드의 손잡이에서 온 움직임만 받는다. */
    fun drag(mapId: Long, delta: Float) {
        if (draggingId != mapId) return
        dragOffset += delta
        while (true) {
            val index = order.indexOf(mapId)
            val target = when {
                dragOffset > 0 -> order.getOrNull(index + 1)
                dragOffset < 0 -> order.getOrNull(index - 1)
                else -> null
            }
            if (target == null) {
                // 맨 끝 카드는 목록 밖으로 나가 다른 섹션을 덮지 않게 제자리에 멈춘다.
                dragOffset = 0f
                return
            }
            val step = (heights[target] ?: return) + spacingPx
            if (abs(dragOffset) < step / 2) return

            val targetIndex = order.indexOf(target)
            order = order.toMutableList().apply { add(targetIndex, removeAt(index)) }
            dragOffset -= if (dragOffset > 0) step else -step
            onMove(mapId, target)
        }
    }

    /** 다른 카드가 끌리는 중이면 건드리지 않는다. */
    fun end(mapId: Long) {
        if (draggingId != mapId) return
        draggingId = null
        dragOffset = 0f
    }

    /**
     * 끌지 않고 한 칸 옮긴다. 끌 수 없는 사용자(TalkBack 등)를 위한 접근성 동작이다.
     * 끄는 중이거나 더 옮길 자리가 없으면 false 를 돌려준다.
     */
    fun moveByOne(mapId: Long, down: Boolean): Boolean {
        if (draggingId != null) return false
        val index = order.indexOf(mapId)
        if (index < 0) return false
        val target = order.getOrNull(if (down) index + 1 else index - 1) ?: return false

        val targetIndex = order.indexOf(target)
        order = order.toMutableList().apply { add(targetIndex, removeAt(index)) }
        onMove(mapId, target)
        return true
    }
}

/** [spacing] 은 카드 사이 간격이다. 이웃을 넘었는지 잴 때 카드 높이에 더한다. */
@Composable
internal fun rememberMapReorderState(
    ids: List<Long>,
    spacing: Dp,
    onMove: (mapId: Long, targetId: Long) -> Unit,
): MapReorderState {
    val currentOnMove by rememberUpdatedState(onMove)
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val state = remember(spacingPx) {
        MapReorderState(spacingPx) { mapId, targetId -> currentOnMove(mapId, targetId) }
    }
    SideEffect { state.sync(ids) }
    return state
}

/** 순서를 바꿀 카드에 단다. 끄는 카드는 손가락을 따라가고 다른 카드 위에 그린다. */
internal fun Modifier.reorderableItem(state: MapReorderState, mapId: Long): Modifier = this
    .onSizeChanged { size -> state.onSizeChanged(mapId, size.height) }
    .zIndex(if (state.draggingId == mapId) 1f else 0f)
    .graphicsLayer { translationY = if (state.draggingId == mapId) state.dragOffset else 0f }

/**
 * 손잡이에 단다. 손잡이를 잡고 위아래로 끌면 카드가 따라온다.
 *
 * 끄는 도중 손잡이가 사라지면(편집 완료 등) 제스처가 취소 콜백 없이 끊기므로 `finally` 에서도 놓는다.
 * 끌 수 없는 사용자를 위해 위·아래로 옮기는 접근성 동작도 단다. 손잡이는 카드의 클릭 영역 안에
 * 있어 동작이 카드에 합쳐져, TalkBack 에서 카드를 고른 뒤 동작 메뉴로 옮길 수 있다.
 */
internal fun Modifier.reorderHandle(state: MapReorderState, mapId: Long): Modifier = this
    .semantics {
        customActions = listOf(
            CustomAccessibilityAction("위로 옮기기") { state.moveByOne(mapId, down = false) },
            CustomAccessibilityAction("아래로 옮기기") { state.moveByOne(mapId, down = true) },
        )
    }
    .pointerInput(state, mapId) {
        try {
            detectVerticalDragGestures(
                onDragStart = { state.start(mapId) },
                onDragEnd = { state.end(mapId) },
                onDragCancel = { state.end(mapId) },
                onVerticalDrag = { change, delta ->
                    change.consume()
                    state.drag(mapId, delta)
                },
            )
        } finally {
            state.end(mapId)
        }
    }
