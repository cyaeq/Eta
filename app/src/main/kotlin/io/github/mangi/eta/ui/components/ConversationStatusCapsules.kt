package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
) {
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
    val contextText = count + (limit?.let { "/$it" } ?: "") + (percent?.let { " · $it" } ?: "") + compactionText
    val contextDescription = stringResource(R.string.conversation_context_capsule) + " " + contextText
    val speed = (lastUsage ?: messages.asReversed().filterIsInstance<AgentMessageUi>()
        .firstOrNull { it.usage != null }?.usage)?.averageTokensPerSecond
    val speedText = speed?.let {
        NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it)
    } ?: "—"
    // 独占一行（顶栏 bottomContent），行首放快捷图标、其余靠右贴胶囊。
    // 用可伸缩 Spacer 而不是 Arrangement.End：内容超宽时 Spacer 会先归零，
    // 不会像 End 那样把整组胶囊顶到屏幕外。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        Spacer(Modifier.weight(1f))
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
