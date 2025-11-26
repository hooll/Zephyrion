package com.faithl.zephyrion.storage

import org.jetbrains.exposed.sql.Database
import taboolib.common.platform.function.getDataFolder
import java.io.File

object SQLite : Type() {

    override fun connect() {
        val workdir = getDataFolder()
        if (!workdir.exists()) {
            workdir.mkdirs()
        }
        val file = workdir.path + "/zephyrion.db"
        if (!File(file).exists()) {
            File(file).createNewFile()
        }
        Database.connect("jdbc:sqlite:$file", "org.sqlite.JDBC")
    }

}