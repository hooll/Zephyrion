package com.faithl.zephyrion.core.services

import com.faithl.zephyrion.api.events.VaultAddItemEvent
import com.faithl.zephyrion.api.events.VaultCloseEvent
import com.faithl.zephyrion.api.events.VaultOpenEvent
import com.faithl.zephyrion.api.events.VaultRemoveItemEvent
import com.faithl.zephyrion.core.models.Vault
import com.faithl.zephyrion.core.models.WorkspaceType
import com.faithl.zephyrion.core.ui.vault.VaultUI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.ui.InventoryViewProxy
import taboolib.module.ui.MenuHolder

/**
 * 保险库同步服务
 *
 * 因为管理员和成员的 UI 不同（管理员有解锁按钮），无法共享整个 Inventory。
 * 只同步物品槽位 (0-35) 的内容。
 */
object VaultSyncService {

    data class ViewerGroup(
        val vaultId: Int,
        val page: Int,
        val viewers: MutableList<Player> = mutableListOf()
    )

    private val groups = mutableSetOf<ViewerGroup>()

    /**
     * 注册查看者
     */
    private fun register(vault: Vault, page: Int, player: Player) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = groups.find { it.vaultId == vault.id && it.page == page }
            ?: ViewerGroup(vault.id, page).also { groups.add(it) }

        if (!group.viewers.contains(player)) {
            group.viewers.add(player)
        }
    }

    /**
     * 注销查看者
     */
    private fun unregister(vault: Vault, page: Int, player: Player) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = groups.find { it.vaultId == vault.id && it.page == page } ?: return
        group.viewers.remove(player)

        if (group.viewers.isEmpty()) {
            groups.remove(group)
        }
    }

    /**
     * 同步物品变化到所有查看者（排除操作者本人）
     * 只同步物品槽位 (0-35)
     */
    private fun syncItemChange(vault: Vault, page: Int, slot: Int, itemStack: ItemStack?, operator: Player?) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return
        if (slot !in 0..35) return

        val group = groups.find { it.vaultId == vault.id && it.page == page } ?: return

        // 清理离线玩家
        group.viewers.removeIf { !it.isOnline }

        group.viewers
            .filter { it != operator } // 排除操作者本人
            .forEach { player ->
                submit {
                    val inv = InventoryViewProxy.getTopInventory(player.openInventory)
                    if (inv.holder is MenuHolder) {
                        inv.setItem(slot, itemStack)
                    }
                }
            }
    }

    /**
     * 刷新所有查看者（解锁槽位后需要重建 UI）
     */
    fun refreshAllViewers(vault: Vault, page: Int) {
        if (vault.workspace.type == WorkspaceType.INDEPENDENT) return

        val group = groups.find { it.vaultId == vault.id && it.page == page } ?: return

        group.viewers.removeIf { !it.isOnline }

        val players = group.viewers.toList()
        groups.remove(group)

        players.forEach { player ->
            player.closeInventory()
            VaultUI(player, vault, page = page).open()
        }
    }

    @SubscribeEvent
    fun onVaultOpen(e: VaultOpenEvent) {
        register(e.vault, e.page, e.opener)
    }

    @SubscribeEvent
    fun onVaultClose(e: VaultCloseEvent) {
        unregister(e.vault, e.page, e.closer)
    }

    @SubscribeEvent
    fun onVaultAddItem(e: VaultAddItemEvent) {
        syncItemChange(e.vault, e.page, e.slot, e.itemStack, e.operator)
    }

    @SubscribeEvent
    fun onVaultRemoveItem(e: VaultRemoveItemEvent) {
        syncItemChange(e.vault, e.page, e.slot, null, e.operator)
    }
}