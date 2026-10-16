package io.github.mangi.eta.ui.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

internal enum class ConversationPaneAnchor {
    Closed,
    Open,
}

/**
 * 侧栏分类胶囊的筛选条件。
 *
 * [All]＝不筛选、显示全部会话；[Uncategorized] 对应 `categoryId == null`
 * 的会话；[Category] 对应某个具体分类。
 */
internal sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Uncategorized : CategoryFilter
    data class Category(val id: String) : CategoryFilter
}

/**
 * [CategoryFilter] 的 `rememberSaveable` 存根：编码成 `"all"` / `"uncategorized"` 或
 * `"category:<id>"`。用密封类型本身当 `Original`（非空），省掉可空 `Saver` 的边界问题。
 */
internal val CategoryFilterSaver: Saver<CategoryFilter, String> = Saver(
    save = { filter ->
        when (filter) {
            CategoryFilter.All -> "all"
            CategoryFilter.Uncategorized -> "uncategorized"
            is CategoryFilter.Category -> "category:" + filter.id
        }
    },
    restore = { raw ->
        when {
            raw == "uncategorized" -> CategoryFilter.Uncategorized
            raw.startsWith("category:") -> CategoryFilter.Category(raw.removePrefix("category:"))
            else -> CategoryFilter.All
        }
    },
)

internal fun conversationPaneSettledValues(
    state: AnchoredDraggableState<ConversationPaneAnchor>,
    isDragging: () -> Boolean,
): Flow<ConversationPaneAnchor> = snapshotFlow {
    val anchor = state.settledValue
    val position = state.anchors.positionOf(anchor)
    if (!isDragging() && !state.isAnimationRunning && abs(state.offset - position) < 0.5f) {
        anchor
    } else {
        null
    }
    // 必须先让 snapshotFlow 观察到运动中的 null，再过滤；回到原锚点也要重新同步。
}.filterNotNull()
