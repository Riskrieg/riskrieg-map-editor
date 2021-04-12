package com.riskrieg.mapeditor

import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.KeyStroke
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuItem
import com.riskrieg.mapeditor.model.EditMode
import com.riskrieg.mapeditor.model.EditorModel
import com.riskrieg.mapeditor.ui.Editor
import javax.imageio.ImageIO

class Init {

    private val model: EditorModel = EditorModel()

    fun start() {
        Window(
            title = "${Constants.NAME} Map Editor v2.0.0-alpha.1",
            icon = ImageIO.read(Init::class.java.classLoader.getResourceAsStream("icon/icon.png")),
            size = IntSize(Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT),
            menuBar = MyMenuBar()
        ) {
            DesktopMaterialTheme {
                Editor(model).init()
            }
        }
    }

    fun MyMenuBar(): MenuBar {
        return MenuBar(
            Menu(
                name = "File",
                MenuItem(
                    name = "New",
                    shortcut = KeyStroke(Key.N),
                    onClick = {
                        model.reset()
                    }
                ),
                MenuItem(
                    name = "Open...",
                    shortcut = KeyStroke(Key.O),
                    onClick = {
                        model.openRkmFile()
                    }
                ),
                MenuItem(
                    name = "Export...",
                    shortcut = KeyStroke(Key.E),
                    onClick = {
                        model.exportAsRkm()
                    }
                ),
                MenuItem(
                    name = "Export Graph...",
                    shortcut = KeyStroke(Key.R),
                    onClick = {
                        model.exportGraphFile()
                    }
                )
            ),
            Menu(
                name = "Import",
                MenuItem(
                    name = "Image Layers...",
                    shortcut = KeyStroke(Key.L),
                    onClick = {
                        model.importMapAsLayers()
                    }
                ),
                MenuItem(
                    name = "Graph...",
                    shortcut = KeyStroke(Key.G),
                    onClick = {
                        model.importGraphFile()
                    }
                )
            ),
            Menu(
                name = "Mode",
                MenuItem(
                    name = "Edit Territories",
                    shortcut = KeyStroke(Key.T),
                    onClick = {
                        model.deselect()
                        model.editMode = EditMode.EDIT_TERRITORY
                        model.update()
                    }
                ),
                MenuItem(
                    name = "Edit Neighbors",
                    shortcut = KeyStroke(Key.B),
                    onClick = {
                        model.clearSelectedRegions()
                        model.editMode = EditMode.EDIT_NEIGHBORS
                        model.update()
                    }
                )
            )
        )
    }

}