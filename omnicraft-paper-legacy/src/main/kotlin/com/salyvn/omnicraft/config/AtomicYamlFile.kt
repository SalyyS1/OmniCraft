package com.salyvn.omnicraft.config

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AtomicYamlFile {
    fun save(file: File, yaml: YamlConfiguration) {
        file.parentFile.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        yaml.save(temporary)
        try {
            Files.move(
                temporary.toPath(), file.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
