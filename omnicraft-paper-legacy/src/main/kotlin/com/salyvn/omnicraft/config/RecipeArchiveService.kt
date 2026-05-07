package com.salyvn.omnicraft.config

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RecipeArchiveService(private val plugin: JavaPlugin) {
    fun exportCategory(categoryId: String): File {
        val source = File(plugin.dataFolder, "category/$categoryId")
        val output = File(plugin.dataFolder, "exports/$categoryId.zip")
        output.parentFile.mkdirs()
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            source.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = source.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                zip.putNextEntry(ZipEntry(relative))
                FileInputStream(file).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return output
    }

    fun importCategory(categoryId: String, archive: File) {
        val target = File(plugin.dataFolder, "category/$categoryId")
        target.mkdirs()
        ZipInputStream(FileInputStream(archive)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".yml")) {
                    val output = File(target, entry.name).canonicalFile
                    if (!output.path.startsWith(target.canonicalPath)) error("Unsafe zip entry: ${entry.name}")
                    output.parentFile.mkdirs()
                    FileOutputStream(output).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}
