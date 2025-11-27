package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.Zephyrion
import com.faithl.zephyrion.api.ZephyrionAPI
import com.faithl.zephyrion.storage.DatabaseConfig
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import taboolib.module.database.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Workspaces表定义
 */
object WorkspacesTable {

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("workspaces")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(
                                ColumnOptionSQL.PRIMARY_KEY,
                                ColumnOptionSQL.AUTO_INCREMENT,
                                ColumnOptionSQL.UNSIGNED
                            )
                        }
                    }
                    add("name") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("description") {
                        type(ColumnTypeSQL.VARCHAR, 255)
                    }
                    add("type") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("owner") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("members") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(
                                ColumnOptionSQLite.PRIMARY_KEY,
                                ColumnOptionSQLite.AUTOINCREMENT
                            )
                        }
                    }
                    add("name") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("description") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("type") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("owner") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("members") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("created_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("updated_at") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }
            }
            else -> error("unknown database type")
        }
    }

    /**
     * 初始化独立工作空间
     */
    fun initializeIndependentWorkspace() {
        if (!Zephyrion.settings.getBoolean("workspace.independent")) {
            return
        }

        val dataSource = DatabaseConfig.dataSource
        val table = DatabaseConfig.workspacesTable

        // 检查是否已存在
        val exists = table.select(dataSource) {
            where { "name" eq "Independent" }
        }.find()

        if (!exists) {
            table.insert(dataSource, "name", "description", "type", "owner", "members", "created_at", "updated_at") {
                value(
                    "Independent",
                    "Independent workspace",
                    "INDEPENDENT",
                    "Server",
                    "",
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
                )
            }
        }
    }
}

/**
 * Workspace数据类
 */
data class Workspace(
    var id: Int,
    var name: String,
    var desc: String?,
    var type: Type,
    var owner: String,
    var members: String,
    var createdAt: Long,
    var updatedAt: Long
) {

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.workspacesTable

        private val dataSource
            get() = DatabaseConfig.dataSource

        /**
         * 工作空间类型枚举
         */
        enum class Type {
            PUBLIC, PRIVATE, INDEPENDENT
        }

        /**
         * 根据ID获取工作空间
         */
        fun findById(id: Int): Workspace? {
            return table.select(dataSource) {
                where { "id" eq id }
            }.firstOrNull {
                Workspace(
                    id = getInt("id"),
                    name = getString("name"),
                    desc = getString("description"),
                    type = Type.valueOf(getString("type")),
                    owner = getString("owner"),
                    members = getString("members"),
                    createdAt = getLong("created_at"),
                    updatedAt = getLong("updated_at")
                )
            }
        }

        /**
         * 获取独立工作空间
         */
        fun getIndependentWorkspace(): Workspace? {
            return table.select(dataSource) {
                where { "name" eq "Independent" }
            }.firstOrNull {
                Workspace(
                    id = getInt("id"),
                    name = getString("name"),
                    desc = getString("description"),
                    type = Type.valueOf(getString("type")),
                    owner = getString("owner"),
                    members = getString("members"),
                    createdAt = getLong("created_at"),
                    updatedAt = getLong("updated_at")
                )
            }
        }

        /**
         * 获取玩家加入的所有工作空间
         */
        fun getJoinedWorkspaces(player: Player): List<Workspace> {
            val playerUUID = player.uniqueId.toString()
            return table.select(dataSource) {
                where { "members" like "%$playerUUID%" }
            }.map {
                Workspace(
                    id = getInt("id"),
                    name = getString("name"),
                    desc = getString("description"),
                    type = Type.valueOf(getString("type")),
                    owner = getString("owner"),
                    members = getString("members"),
                    createdAt = getLong("created_at"),
                    updatedAt = getLong("updated_at")
                )
            }
        }

        /**
         * 获取指定玩家的指定工作空间
         */
        fun getWorkspace(player: String, name: String): Workspace? {
            return table.select(dataSource) {
                where {
                    "members" like "%$player%"
                    and { "name" eq name }
                }
            }.firstOrNull {
                Workspace(
                    id = getInt("id"),
                    name = getString("name"),
                    desc = getString("description"),
                    type = Type.valueOf(getString("type")),
                    owner = getString("owner"),
                    members = getString("members"),
                    createdAt = getLong("created_at"),
                    updatedAt = getLong("updated_at")
                )
            }
        }

        /**
         * 添加成员到工作空间
         */
        fun addMember(workspace: Workspace, player: Player): Boolean {
            if (workspace.members.contains(player.uniqueId.toString())) {
                return false
            }

            workspace.members = workspace.members + "," + player.uniqueId.toString()
            workspace.updatedAt = System.currentTimeMillis()

            table.update(dataSource) {
                set("members", workspace.members)
                set("updated_at", workspace.updatedAt)
                where { "id" eq workspace.id }
            }
            return true
        }

        /**
         * 从工作空间移除成员
         */
        fun removeMember(workspace: Workspace, player: OfflinePlayer): Boolean {
            if (workspace.owner == player.uniqueId.toString()) {
                return false
            }

            if (!workspace.members.contains(player.uniqueId.toString())) {
                return false
            }

            workspace.members = workspace.members.replace("," + player.uniqueId.toString(), "")
            workspace.updatedAt = System.currentTimeMillis()

            table.update(dataSource) {
                set("members", workspace.members)
                set("updated_at", workspace.updatedAt)
                where { "id" eq workspace.id }
            }
            return true
        }
    }

    /**
     * 重命名工作空间
     */
    fun rename(newName: String): ZephyrionAPI.Result {
        val result = ZephyrionAPI.validateWorkspaceName(newName, getOwner().uniqueId.toString())
        if (!result.success) {
            return result
        }

        name = newName
        updatedAt = System.currentTimeMillis()

        table.update(dataSource) {
            set("name", name)
            set("updated_at", updatedAt)
            where { "id" eq id }
        }
        return ZephyrionAPI.Result(true)
    }

    /**
     * 获取创建时间(格式化)
     */
    fun getCreatedAt(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(createdAt))
    }

    /**
     * 获取更新时间(格式化)
     */
    fun getUpdatedAt(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(updatedAt))
    }

    /**
     * 获取拥有者
     */
    fun getOwner(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(UUID.fromString(owner))
    }

    /**
     * 获取所有成员
     */
    fun getMembers(): List<OfflinePlayer> {
        return members.split(",").filter { it.isNotBlank() }.map {
            Bukkit.getOfflinePlayer(UUID.fromString(it))
        }
    }

    /**
     * 获取所有成员名称
     */
    fun getMembersName(): List<String> {
        return members.split(",").filter { it.isNotBlank() }.mapNotNull {
            val offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(it))
            offlinePlayer.name
        }
    }

    /**
     * 删除工作空间
     */
    fun delete() {
        val user = ZephyrionAPI.getUserData(owner)
        user.workspaceUsed -= 1

        table.update(dataSource) {
            set("workspace_used", user.workspaceUsed)
            where { "player" eq owner }
        }

        table.delete(dataSource) {
            where { "id" eq id }
        }
    }

    /**
     * 检查是否为成员
     */
    fun isMember(uuid: String): Boolean {
        return members.contains(uuid)
    }
}
