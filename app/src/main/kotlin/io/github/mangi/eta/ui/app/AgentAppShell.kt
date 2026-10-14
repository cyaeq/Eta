package io.github.mangi.eta.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.components.AdaptiveTopAppBar
import io.github.mangi.eta.ui.components.ConversationSidePaneScaffold
import io.github.mangi.eta.ui.components.MiuixBackButton
import io.github.mangi.eta.ui.components.ShortcutIcon
import io.github.mangi.eta.ui.components.openVirtualScreen
import io.github.mangi.eta.ui.components.rememberVirtualScreenMenuEntries
import io.github.mangi.eta.ui.components.TopBarBackdrop
import io.github.mangi.eta.ui.components.captureForTopBar
import io.github.mangi.eta.ui.components.rememberTopBarBackdrop
import io.github.mangi.eta.ui.components.topBarContainerColor
import io.github.mangi.eta.ui.model.ConversationPaneUiState
import io.github.mangi.eta.ui.model.ConversationSummaryUi
import io.github.mangi.eta.ui.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * Agent App 统一壳层。
 *
 * - 负责全局 Scaffold、状态栏/横向安全边距、顶层工具栏。
 * - 首页工具栏只保留历史入口与溢出菜单（新建对话、终端、浏览器），保持聊天舞台干净。
 * - 非首页子路由统一提供返回按钮与标题，避免每个页面各自像独立设置页。
 * - Settings 由标准二级页骨架自己提供 TopAppBar，壳层在此路由不重复绘制。
 */
@Composable
fun AgentAppShell(
    currentRoute: AppRoute?,
    isCurrentRoute: Boolean,
    conversationPaneState: ConversationPaneUiState?,
    isConversationPaneOpen: Boolean,
    onBack: () -> Unit,
    onOpenConversationPane: () -> Unit,
    onDismissConversationPane: () -> Unit,
    onSearchConversations: (String) -> Unit,
    onNewConversation: () -> Unit,
    onOpenTerminal: () -> Unit,
    onLaunchKimiWeb: () -> Unit,
    kimiWebLabel: String,
    canStopKimiWeb: Boolean,
    onStopKimiWeb: () -> Unit,
    onRefreshKimiWeb: () -> Unit,
    onOpenBrowser: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onConversationRename: (ConversationSummaryUi) -> Unit,
    onMoveToCategory: (ConversationSummaryUi) -> Unit,
    onConversationExport: (ConversationSummaryUi) -> Unit,
    onConversationDelete: (ConversationSummaryUi) -> Unit,
    onConversationPin: (ConversationSummaryUi) -> Unit,
    onBatchDelete: (List<ConversationSummaryUi>) -> Unit,
    onOpenTools: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRootSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenModelProviders: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenCategories: () -> Unit,
    conversationStatus: (@Composable () -> Unit)? = null,
    /** 首页顶栏右侧的「工具档」入口；与 [conversationStatus] 同款可空槽位，由调用方注入。 */
    profileAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberTopBarBackdrop()
    val topBarColor = topBarContainerColor(backdrop)
    // 首页顶栏标题＝当前会话标题（标题为空时退回预览文本）；没选中会话时留空。
    // 工具档从下面那行挪上来之后，导航图标与右侧操作之间的空位正好给标题用。
    val homeTitle = conversationPaneState
        ?.let { pane -> pane.conversations.firstOrNull { it.id == pane.selectedConversationId } }
        ?.let { it.title.ifBlank { it.preview } }
        ?.takeIf { it.isNotBlank() }
        .orEmpty()
    val pageContent: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // 设置了自定义背景图时让容器透明，底图才不会被 Scaffold 默认的 surface 色盖住。
            containerColor = pageContainerColor(),
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            topBar = {
                if (currentRoute !is AppRoute.Settings) {
                    TopBarBackdrop(backdrop) {
                        AgentTopBar(
                            route = currentRoute,
                            homeTitle = homeTitle,
                            scrollBehavior = scrollBehavior,
                            color = topBarColor,
                            onBack = onBack,
                            onOpenConversationPane = onOpenConversationPane,
                            onOpenCategories = onOpenCategories,
                            onNewConversation = onNewConversation,
                            onOpenTerminal = onOpenTerminal,
                            onLaunchKimiWeb = onLaunchKimiWeb,
                            kimiWebLabel = kimiWebLabel,
                            canStopKimiWeb = canStopKimiWeb,
                            onStopKimiWeb = onStopKimiWeb,
                            onRefreshKimiWeb = onRefreshKimiWeb,
                            onOpenBrowser = onOpenBrowser,
                            conversationStatus = conversationStatus,
                            profileAction = profileAction,
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .captureForTopBar(backdrop)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                content(padding)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (conversationPaneState != null && currentRoute is AppRoute.Home) {
            ConversationSidePaneScaffold(
                state = conversationPaneState,
                visible = isConversationPaneOpen,
                backHandlerEnabled = isCurrentRoute,
                onOpen = onOpenConversationPane,
                onDismiss = onDismissConversationPane,
                onSearchChange = onSearchConversations,
                onNewConversation = onNewConversation,
                onConversationSelected = onSelectConversation,
                onConversationRename = onConversationRename,
                onMoveToCategory = onMoveToCategory,
                onConversationExport = onConversationExport,
                onConversationDelete = onConversationDelete,
                onConversationPin = onConversationPin,
                onBatchDelete = onBatchDelete,
                onOpenSettings = onOpenSettings,
                onOpenRootSettings = onOpenRootSettings,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenModelProviders = onOpenModelProviders,
                onOpenTools = onOpenTools,
                onOpenSkills = onOpenSkills,
                onOpenCharacters = onOpenCharacters,
                onOpenPermissions = onOpenPermissions,
                onOpenTasks = onOpenTasks,
                onOpenCategories = onOpenCategories,
            ) {
                pageContent()
            }
        } else {
            pageContent()
        }
    }
}

@Composable
private fun AgentTopBar(
    route: AppRoute?,
    homeTitle: String,
    scrollBehavior: ScrollBehavior,
    color: Color,
    onBack: () -> Unit,
    onOpenConversationPane: () -> Unit,
    onOpenCategories: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenTerminal: () -> Unit,
    onLaunchKimiWeb: () -> Unit,
    kimiWebLabel: String,
    canStopKimiWeb: Boolean,
    onStopKimiWeb: () -> Unit,
    onRefreshKimiWeb: () -> Unit,
    onOpenBrowser: () -> Unit,
    conversationStatus: (@Composable () -> Unit)?,
    profileAction: (@Composable () -> Unit)?,
) {
    val isHome = route is AppRoute.Home
    val navigationIcon: @Composable () -> Unit = {
        if (isHome) {
            IconButton(onClick = onOpenConversationPane) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ShortText,
                    modifier = Modifier.size(24.dp),
                    contentDescription = stringResource(R.string.action_conversation_history),
                )
            }
        } else {
            MiuixBackButton(onClick = onBack)
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        if (isHome) {
            // 顶栏这一排：工具档入口、新建对话、溢出菜单。
            // 状态胶囊组不再挤在这里：手机宽度装不下「导航图标 + 三枚胶囊 + 两个按钮」，
            // 胶囊组会向左溢出盖住导航图标（token 破 100K 后文字变长时尤其明显）。
            profileAction?.invoke()
            IconButton(onClick = onNewConversation) {
                Icon(
                    imageVector = Icons.Rounded.AddComment,
                    contentDescription = stringResource(R.string.action_new_conversation),
                    modifier = Modifier.size(TopBarMenuIconSize),
                )
            }
            TopBarOverflowMenu(
                onOpenCategories = onOpenCategories,
                onNewConversation = onNewConversation,
                onOpenTerminal = onOpenTerminal,
                onLaunchKimiWeb = onLaunchKimiWeb,
                kimiWebLabel = kimiWebLabel,
                canStopKimiWeb = canStopKimiWeb,
                onStopKimiWeb = onStopKimiWeb,
                onRefreshKimiWeb = onRefreshKimiWeb,
                onOpenBrowser = onOpenBrowser,
            )
        }
    }
    // 状态胶囊单独占一行（bottomContent），拿满整条栏宽，不会再和导航图标、右侧按钮抢位置。
    val statusRow: @Composable () -> Unit = {
        if (isHome) conversationStatus?.invoke()
    }

    if (isHome) {
        // 首页聊天舞台保持紧凑；标题处显示当前会话标题。二级内容页统一使用可折叠大标题。
        SmallTopAppBar(
            title = homeTitle,
            color = color,
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = statusRow,
            // 首页标题靠左显示（不再是居中大标题），26dp 的默认内边距纯属浪费；
            // 这台机器只有 363dp 宽，省下来的横向空间全部让给会话标题。
            titlePadding = HomeTitlePadding,
            // miuix 的 actions 行本身是 Arrangement.End（靠右），右边原来有 16dp 默认留白，
            // 收到 6dp：工具档、＋、⋯ 一起往右贴，行尾也不空。
            actionIconPadding = 6.dp,
        )
    } else {
        AdaptiveTopAppBar(
            title = titleForRoute(route),
            color = color,
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
        )
    }
}

private val TopBarMenuIconSize = 20.dp

/**
 * 首页标题的横向内边距。
 *
 * Miuix 默认 26dp 是给居中大标题留的呼吸空间；首页标题靠左显示、左边已经有导航图标兜着，
 * 这份内边距就纯属浪费。这台机器只有 363dp 宽，左右各省 18dp ≈ 多显示 3 个汉字。
 */
private val HomeTitlePadding = 8.dp

/**
 * 首页顶栏溢出菜单。WindowListPopup 以父布局为锚点，因此与触发按钮包在同一个 Box 中，
 * 弹层从按钮下方右对齐展开。
 */
@Composable
private fun TopBarOverflowMenu(
    onOpenCategories: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenTerminal: () -> Unit,
    onLaunchKimiWeb: () -> Unit,
    kimiWebLabel: String,
    canStopKimiWeb: Boolean,
    onStopKimiWeb: () -> Unit,
    onRefreshKimiWeb: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { onRefreshKimiWeb(); showMenu = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                modifier = Modifier.size(24.dp),
                contentDescription = stringResource(R.string.action_more),
            )
        }
        WindowListPopup(
            show = showMenu,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { showMenu = false },
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val virtualEntries = rememberVirtualScreenMenuEntries()
            val categoriesText = stringResource(R.string.sidebar_categories)
            val openTerminalText = stringResource(R.string.action_open_terminal)
            val stopKimiWebText = stringResource(R.string.capability_kimi_stop)
            val openBrowserText = stringResource(R.string.action_open_browser)
            // 虚拟屏入口与收藏应用平铺在菜单最上方：点开菜单就能直达，不再有二级页面。
            val menuItems: List<DropdownItem> = buildList {
                virtualEntries.forEach { entry ->
                    add(
                        DropdownItem(
                            text = entry.label,
                            icon = { modifier ->
                                val bitmap = entry.icon
                                if (bitmap != null) {
                                    ShortcutIcon(bitmap, modifier.size(TopBarMenuIconSize))
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Smartphone,
                                        contentDescription = null,
                                        modifier = modifier.size(TopBarMenuIconSize),
                                    )
                                }
                            },
                        ),
                    )
                }
                add(
                    DropdownItem(
                        text = categoriesText,
                        icon = { modifier ->
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                modifier = modifier.size(TopBarMenuIconSize),
                            )
                        },
                    ),
                )
                add(
                    DropdownItem(
                        text = openTerminalText,
                        icon = { modifier ->
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                modifier = modifier.size(TopBarMenuIconSize),
                            )
                        },
                    ),
                )
                add(
                    DropdownItem(
                        text = openBrowserText,
                        icon = { modifier ->
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                modifier = modifier.size(TopBarMenuIconSize),
                            )
                        },
                    ),
                )
                if (canStopKimiWeb) {
                    add(DropdownItem(text = stopKimiWebText))
                }
            }
            ListPopupColumn {
                menuItems.forEachIndexed { index, item ->
                    DropdownImpl(
                        item = item,
                        optionSize = menuItems.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = {
                            showMenu = false
                            val entry = virtualEntries.getOrNull(index)
                            when {
                                entry != null -> openVirtualScreen(context, scope, entry.component)
                                index == virtualEntries.size -> onOpenCategories()
                                index == virtualEntries.size + 1 -> onOpenTerminal()
                                index == virtualEntries.size + 2 -> onOpenBrowser()
                                else -> onStopKimiWeb()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun titleForRoute(route: AppRoute?): String = when (route) {
    is AppRoute.Home -> ""
    is AppRoute.Browser -> stringResource(R.string.route_browser)
    is AppRoute.Terminal -> stringResource(R.string.route_terminal)
    is AppRoute.Tools -> stringResource(R.string.route_tools)
    is AppRoute.Skills -> stringResource(R.string.route_skills)
    is AppRoute.Characters -> "角色"
    is AppRoute.CharacterDetail -> "角色详情"
    is AppRoute.CharacterEditor -> "编辑角色"
    is AppRoute.CharacterPersona -> "我的人设"
    is AppRoute.CharacterMemory -> "剧情记忆"
    is AppRoute.Permissions -> stringResource(R.string.route_permissions)
    is AppRoute.ConversationCategories -> stringResource(R.string.conversation_categories_title)
    is AppRoute.SystemEnhance -> stringResource(R.string.route_system_enhancements)
    is AppRoute.Settings -> stringResource(R.string.route_settings)
    is AppRoute.TokenUsage -> stringResource(R.string.usage_stats_title)
    is AppRoute.DataUsage -> stringResource(R.string.data_usage_title)
    is AppRoute.SelfCheck -> "功能自检"
    is AppRoute.DiagnosticsPatrol -> stringResource(R.string.diag_patrol_title)
    is AppRoute.SpeechSettings -> stringResource(R.string.speech_settings_title)
    is AppRoute.SpeechRecognition -> stringResource(R.string.speech_recognition_title)
    is AppRoute.SpeechSynthesis -> stringResource(R.string.speech_synthesis_title)
    is AppRoute.SpeechOss -> stringResource(R.string.speech_oss_title)
    is AppRoute.AppearanceSettings -> stringResource(R.string.appearance_title)
    is AppRoute.DataBackup -> stringResource(R.string.data_backup_title)
    is AppRoute.Memory -> stringResource(R.string.route_memory)
    is AppRoute.Tasks -> stringResource(R.string.automation_title)
    is AppRoute.VirtualScreen -> stringResource(R.string.virtual_screen_title)
    is AppRoute.LinuxEnvironment -> stringResource(R.string.route_linux_environment)
    is AppRoute.Workspace -> stringResource(R.string.capability_workspace)
    is AppRoute.SharedFolders -> stringResource(R.string.route_shared_folders)
    is AppRoute.LinuxFiles -> stringResource(R.string.route_linux_files)
    is AppRoute.ModelProviders -> stringResource(R.string.route_model_providers)
    is AppRoute.CommunityCatalog -> "从目录添加"
    is AppRoute.CommunityCatalogProvider -> "选择模型"
    is AppRoute.McpServers -> stringResource(R.string.route_mcp_servers)
    is AppRoute.McpServerDetail -> stringResource(R.string.route_mcp_server_detail)
    is AppRoute.ModelProviderDetail -> stringResource(R.string.route_provider_details)
    is AppRoute.ModelProviderNew -> stringResource(R.string.route_new_provider)
    null -> stringResource(R.string.app_name)
}
