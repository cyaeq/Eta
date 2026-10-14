package io.github.mangi.eta.data.repository

import java.io.File
import java.util.zip.ZipFile

/**
 * 备份产物的命名与内容约定。
 *
 * 交付的补丁包缺少这份实现：第 22 层的功能自检引用了
 * `io.github.mangi.eta.data.repository.EtaFullBackup`，但整套补丁里没有它的定义，
 * 导致 `:app:compileDebugKotlin` 报 UNRESOLVED_REFERENCE。这里按本仓现有的备份实现
 * 补回最小可用版本：
 *
 * - [AutoBackupRunner] 产出的 `.eta-backup.json` 导出本身就是全量数据，算作全量包；
 * - 外部的 `.zip` 全量包（内含 [DATABASE_ENTRY]）一并算作全量包。
 *
 * 注意：本仓当前的自动备份产出的是 JSON 导出，不是含 `private/` 目录树的压缩包，
 * 所以 [databaseEntryPresent] 对 JSON 返回 null（无法用压缩包方式判断）。
 */
internal object EtaFullBackup {

    /** 压缩包形式的全量包里，数据库条目所在的相对路径。 */
    const val DATABASE_ENTRY = "private/databases/eta.db"

    /** 与 [AutoBackupRunner] 的命名保持一致（`Eta-自动备份-<时间戳>.eta-backup.json`）。 */
    private const val JSON_SUFFIX = ".eta-backup.json"
    private const val ZIP_SUFFIX = ".zip"

    /** 文件名是否表示一个全量备份包。 */
    fun isFullBackup(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(JSON_SUFFIX) || lower.endsWith(ZIP_SUFFIX)
    }

    /**
     * 压缩包形式的全量包里是否含数据库文件。
     *
     * 返回 null 表示这个产物不是压缩包（例如 JSON 导出），无法用这种方式判断，
     * 调用方应当省略这条信息而不是报告「缺失」。
     */
    fun databaseEntryPresent(file: File): Boolean? = runCatching {
        ZipFile(file).use { zip -> zip.getEntry(DATABASE_ENTRY) != null }
    }.getOrNull()
}
