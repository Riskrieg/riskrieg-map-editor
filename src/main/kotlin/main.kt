// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.FlatDarkLaf
import com.riskrieg.editor.model.EditorModel
import com.riskrieg.editor.ui.Editor
import java.awt.Desktop
import java.net.URL
import kotlin.system.exitProcess


@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val model by remember { mutableStateOf(EditorModel()) }
    var themeStr by remember { mutableStateOf("dark") }
    FlatDarkLaf.setup()

    Window(
        onCloseRequest = ::exitApplication,
        title = "${com.riskrieg.editor.Constants.NAME} Map Editor v${com.riskrieg.editor.Constants.VERSION}",
        state = rememberWindowState(width = 1280.dp, height = 720.dp),
        icon = painterResource("icon/icon.png")
    ) {
        MenuBar {
            Menu("File", mnemonic = 'F') { // TODO: Set enabled= when it's fixed to work with icons
                Item(
                    "New",
                    icon = painterResource("icons/$themeStr/new.svg"),
                    onClick = { model.newFile() },
                    shortcut = KeyShortcut(Key.N, ctrl = true)
                )
                Item(
                    "Open...",
                    icon = painterResource("icons/$themeStr/open.svg"),
                    onClick = { model.openRkm() },
                    shortcut = KeyShortcut(Key.O, ctrl = true)
                )
                Menu("Import", mnemonic = 'I') {
                    Item(
                        "Image layers...",
                        icon = painterResource("icons/$themeStr/import_image_layers.svg"),
                        onClick = { model.openImageLayers() },
                        shortcut = KeyShortcut(Key.I, ctrl = true)
                    )
                }
                Separator()
                Item(
                    "Save...",
                    icon = painterResource("icons/$themeStr/save.svg"),
                    onClick = { model.saveRkm() },
                    shortcut = KeyShortcut(Key.S, ctrl = true)
                )
                Separator()
                Item(
                    "Exit",
                    icon = painterResource("icons/$themeStr/exit.svg"),
                    onClick = { exitProcess(0) },
                    shortcut = KeyShortcut(Key.E, ctrl = true)
                )
            }
            Menu("Edit", mnemonic = 'E') {
                Item(
                    "Add as territory",
                    icon = painterResource("icons/$themeStr/add_as_territory.svg"),
                    onClick = { model.submitSelectedRegions() },
                    shortcut = KeyShortcut(Key.F1, ctrl = false)
                )
                Item(
                    "Submit selected neighbors",
                    icon = painterResource("icons/$themeStr/submit_selected_neighbors.svg"),
                    onClick = { model.submitSelectedNeighbors() },
                    shortcut = KeyShortcut(Key.F2, ctrl = false)
                )
                Separator()
                Item(
                    "Delete selected territory",
                    icon = painterResource("icons/$themeStr/delete_selected_territory.svg"),
                    onClick = { model.deleteSelectedTerritory() },
                    shortcut = KeyShortcut(Key.Delete, ctrl = false)
                )
                Item(
                    "Delete all",
                    icon = painterResource("icons/$themeStr/delete_all.svg"),
                    onClick = { model.deleteAll() },
                    shortcut = KeyShortcut(Key.Delete, alt = true)
                )
                Separator()
                Item(
                    "Deselect",
                    icon = painterResource("icons/$themeStr/deselect.svg"),
                    onClick = { model.deselectAll() },
                    shortcut = KeyShortcut(Key.D, ctrl = true)
                )
            }
            Menu("Debug", mnemonic = 'D') {
                Item(
                    "Replace map image...",
                    icon = painterResource("icons/$themeStr/replace_map_image.svg"),
                    onClick = { model.replaceMapImage() },
                    shortcut = KeyShortcut(Key.B, alt = true)
                )
                Item(
                    "Replace text image...",
                    icon = painterResource("icons/$themeStr/replace_text_image.svg"),
                    onClick = { model.replaceTextImage() },
                    shortcut = KeyShortcut(Key.T, alt = true)
                )
                Separator()
                Item(
                    "Import graph...",
                    icon = painterResource("icons/$themeStr/import_graph.svg"),
                    onClick = { model.importGraph() },
                    shortcut = KeyShortcut(Key.G, alt = true)
                )
                Item(
                    "Export graph...",
                    icon = painterResource("icons/$themeStr/export_graph.svg"),
                    onClick = { model.exportGraph() },
                    shortcut = KeyShortcut(Key.R, alt = true)
                )
            }
            Menu("Help", mnemonic = 'H') {
                Item(
                    "Discord",
                    icon = painterResource("icons/$themeStr/discord.svg"),
                    onClick = {
                        openLink("https://www.discord.com/invite/weU8jYDbW4")
                    }
                )
                Separator()
                Item(
                    "About",
                    icon = painterResource("icons/$themeStr/about.svg"),
                    onClick = {
                        openLink("https://www.riskrieg.com")
                    }
                )
            }
        }
        Editor(model).build()
    }
}

private fun openLink(linkStr: String) {
    try {
        Desktop.getDesktop().browse(URL(linkStr).toURI())
    } catch (e: Exception) {
        // TODO: Open dialog popup?
    }
}