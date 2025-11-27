package com.faithl.zephyrion.core.models

import com.faithl.zephyrion.storage.DatabaseConfig
import taboolib.module.database.*

/**
 * Settings表定义
 */
object SettingsTable {

    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("settings")

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
                    add("setting") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("value") {
                        type(ColumnTypeSQL.VARCHAR, 255) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQL.INT) {
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
                    add("setting") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("value") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("vault_id") {
                        type(ColumnTypeSQLite.INTEGER) {
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
     * 设置类型枚举
     */
    enum class SettingType {
        // 可在此处添加设置类型
    }
}

/**
 * Setting数据类
 */
data class Setting(
    var id: Int,
    var setting: String,
    var value: String,
    var vaultId: Int,
    var createdAt: Long,
    var updatedAt: Long
) {

    companion object {

        private val table: Table<*, *>
            get() = DatabaseConfig.settingsTable

        private val dataSource
            get() = DatabaseConfig.dataSource
    }

    /**
     * 获取关联的保险库
     */
    val vault: Vault
        get() = Vault.findById(vaultId)
            ?: error("Vault not found for setting $id")
}
