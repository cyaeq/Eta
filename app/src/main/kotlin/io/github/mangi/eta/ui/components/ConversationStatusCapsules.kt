package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.model.LocalToolProfile
import io.github.mangi.eta.ui.model.AgentChatMessageUi
import io.github.mangi.eta.ui.model.AgentContextUsageUi
import io.github.mangi.eta.ui.model.AgentMessageUi
import io.github.mangi.eta.ui.model.AgentModelOptionUi
import io.github.mangi.eta.ui.model.formatCompactTokenCount
import io.github.mangi.eta.ui.model.latestContextUsage
import io.github.mangi.eta.ui.model.TokenUsageUi
import java.text.NumberFormat
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ConversationStatusCapsules(
    messages: List<AgentChatMessageUi>,
    model: AgentModelOptionUi?,
    lastUsage: TokenUsageUi?,
    trimmed: Pair<Int, Int>? = null,
    downgradedProfileId: String? = null,
    compaction: Pair<Int, Int>? = null,
    /** 行首槽位，用来放快捷图标；不传就是空。 */
    leading: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // 顶栏这一枚只报占用比例：363dp 宽的屏上，带上绝对值（146.3K/1M）会和快捷图标一起把整行撑满，
    // 百分比已经足够表达"还剩多少上下文"。绝对值仍保留在无障碍描述里，读屏不丢信息。
    // 压缩轮次是例外：它只在真的压过上下文时才出现（compaction 为 null 时 compactionText 为空），
    // 属于"发生过才需要知道"的一次性提示，留在可见文字里。
    // 输入栏左侧那枚（AgentContextUsageCapsule）不受影响，仍显示完整数字。
    val usage = latestContextUsage(messages, model).let { it.copy(contextTokens = lastUsage?.contextTokens ?: it.contextTokens) }
    val count = usage.contextTokens?.let { formatCompactTokenCount(it) } ?: "—"
    val limit = usage.contextWindow?.takeIf { it > 0 }?.let { formatCompactTokenCount(it) }
    val percent = usage.contextWindow?.takeIf { it > 0 }?.let { window ->
        usage.contextTokens?.takeIf { it >= 0 }?.let { tokens ->
            NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }.format(tokens.toDouble() / window)
        }
    }
    val compactionText = compaction?.let { (rounds, max) ->
        " · " + stringResource(R.string.conversation_compaction_round_capsule, rounds, max)
    } ?: ""
    val contextText = (percent ?: "—") + compactionText
    val contextDescription = stringResource(R.string.conversation_context_capsule) + " " +
        count + (limit?.let { "/$it" } ?: "") + (percent?.let { " · $it" } ?: "") + compactionText
    val speed = (lastUsage ?: messages.asReversed().filterIsInstance<AgentMessageUi>()
        .firstOrNull { it.usage != null }?.usage)?.averageTokensPerSecond
    val speedText = speed?.let {
        NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it)
    } ?: "—"
    // 独占一行（顶栏 bottomContent）。本组件整块挂在状态行的**右端**，只按内容宽度排布：
    // 行首的 Box(weight) 负责把这一簇顶到右边。内边距与下边距统一由外层给，避免两处叠加。
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading.invoke(this)
            Spacer(Modifier.width(CapsuleGap))
        }
        StatusCapsule(contextText, Modifier, contextDescription)
        Spacer(Modifier.width(CapsuleGap))
        MemoryUsageCapsule()
        trimmed?.let { (rounds, count) ->
            Spacer(Modifier.width(CapsuleGap))
            StatusCapsule(
                stringResource(R.string.conversation_trimmed_capsule, count), Modifier,
                stringResource(R.string.conversation_trimmed_description, rounds),
            )
        }
        downgradedProfileId?.let { id ->
            val label = stringResource(profileLabel(LocalToolProfile.fromId(id)))
            Spacer(Modifier.width(CapsuleGap))
            StatusCapsule(
                stringResource(R.string.conversation_downgrade_capsule, label), Modifier,
                stringResource(R.string.conversation_downgrade_description, label),
            )
        }
    }
}

/** 状态行里相邻两枚胶囊的间距。 */
private val CapsuleGap = 4.dp

/** 输入栏左侧的上下文用量文字胶囊：`125.67K/200K · 62.8%`。 */
@Composable
internal fun AgentContextUsageCapsule(
    usage: AgentContextUsageUi,
    modifier: Modifier = Modifier,
) {
    val tokens = usage.contextTokens ?: return
    val count = formatCompactTokenCount(tokens)
    val limit = usage.contextWindow?.takeIf { it > 0 }?.let { formatCompactTokenCount(it) }
    val percent = usage.contextWindow?.takeIf { it > 0 }?.let { window ->
        NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }
            .format(tokens.toDouble() / window)
    }
    val text = count + (limit?.let { "/$it" } ?: "") + (percent?.let { " · $it" } ?: "")
    Text(
        text = text,
        modifier = modifier
            .background(etaTranslucent(MiuixTheme.colorScheme.surfaceContainer), CircleShape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.footnote1,
        fontSize = 11.sp,
    )
}

@Composable
internal fun StatusCapsule(text: String, modifier: Modifier, description: String = text) {
    Text(text,
        modifier = modifier.background(etaTranslucent(MiuixTheme.colorScheme.surfaceContainer), CircleShape)
            .semantics { contentDescription = description }.padding(horizontal = 8.dp, vertical = 5.dp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.footnote1, fontSize = 11.sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}
