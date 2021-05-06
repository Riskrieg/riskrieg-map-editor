package com.riskrieg.mapeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aaronjyoder.util.json.gson.GsonUtil
import com.riskrieg.map.RkmMap
import com.riskrieg.map.data.MapAuthor
import com.riskrieg.map.data.MapGraph
import com.riskrieg.map.data.MapImage
import com.riskrieg.map.data.MapName
import com.riskrieg.map.edge.Border
import com.riskrieg.map.territory.SeedPoint
import com.riskrieg.map.territory.TerritoryId
import com.riskrieg.map.vertex.Territory
import com.riskrieg.mapeditor.Constants
import com.riskrieg.mapeditor.fill.MilazzoFill
import com.riskrieg.mapeditor.util.Extensions.toBitmap
import com.riskrieg.mapeditor.util.ImageUtil
import com.riskrieg.rkm.RkmReader
import com.riskrieg.rkm.RkmWriter
import org.jetbrains.skija.Bitmap
import org.jgrapht.Graphs
import org.jgrapht.graph.SimpleGraph
import java.awt.Color
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter


class EditorModel(mapName: String = "") {

    // Metadata that will be exported, and may also be used in the editor model

    var mapSimpleName: String by mutableStateOf("")
    var mapDisplayName: String by mutableStateOf("")
    var mapAuthorName: String by mutableStateOf("")

    private var graph = SimpleGraph<Territory, Border>(Border::class.java)

    private var base: BufferedImage by mutableStateOf(BufferedImage(1, 1, 2))
    private var text: BufferedImage by mutableStateOf(BufferedImage(1, 1, 2))

    // Purely for editor model

    var mousePos by mutableStateOf(Point(0, 0))

    var editMode by mutableStateOf(EditMode.NO_EDIT)

    private var baseBitmap by mutableStateOf(base.toBitmap().asImageBitmap())
    private var textBitmap by mutableStateOf(text.toBitmap().asImageBitmap())

    private val submittedTerritories = mutableStateListOf<Territory>() // Unfortunately necessary for now
    private val finishedTerritories = mutableStateListOf<Territory>() // Unfortunately necessary for now

    /* Basic Functions */
    fun reset() {
        editMode = EditMode.NO_EDIT
        clearSelectedRegions()
        deselect()
        submittedTerritories.clear()
        finishedTerritories.clear()
        graph = SimpleGraph<Territory, Border>(Border::class.java)
        base = BufferedImage(1, 1, 2)
        text = BufferedImage(1, 1, 2)
        baseBitmap = Bitmap().asImageBitmap()
        textBitmap = Bitmap().asImageBitmap()
        mapSimpleName = ""
        mapDisplayName = ""
        mapAuthorName = ""
    }

    fun base(): ImageBitmap {
        return baseBitmap
    }

    fun text(): ImageBitmap {
        return textBitmap
    }

    fun width(): Int {
        return baseBitmap.width
    }

    fun height(): Int {
        return baseBitmap.height
    }

    fun update() {
        val copy = BufferedImage(base.width, base.height, BufferedImage.TYPE_INT_ARGB)
        val g2d = copy.createGraphics()
        g2d.drawImage(base, 0, 0, null)
        g2d.dispose()

        for (territory in submittedTerritories) {
            for (seedPoint in territory.seedPoints()) {
                val point = seedPoint.asPoint()
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.SUBMITTED_COLOR).fill(point)
            }
        }

        for (territory in finishedTerritories) {
            for (seedPoint in territory.seedPoints()) {
                val point = seedPoint.asPoint()
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.FINISHED_COLOR).fill(point)
            }
        }

        for (territory in neighbors) {
            for (seedPoint in territory.seedPoints()) {
                val point = seedPoint.asPoint()
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.NEIGHBOR_SELECT_COLOR).fill(point)
            }
        }

        if (selectedRegions.isNotEmpty()) {
            for (point in selectedRegions) {
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.SELECT_COLOR).fill(point)
            }
        } else if (selected != noTerritorySelected) {
            for (seedPoint in selected.seedPoints()) {
                val point = seedPoint.asPoint()
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.SELECT_COLOR).fill(point)
            }
        }

        baseBitmap = copy.toBitmap().asImageBitmap()
    }

    fun getSelectedRegions(): Deque<Point> {
        return selectedRegions
    }

    fun getSubmittedTerritories(): SnapshotStateList<Territory> {
        return submittedTerritories
    }

    /* EditMode.EDIT_TERRITORY */
    private val selectedRegions: Deque<Point> = ArrayDeque()

    fun isRegionSelected(point: Point): Boolean {
        val root = ImageUtil.getRootPixel(base, point)
        return selectedRegions.contains(root)
    }

    fun clearSelectedRegions() {
        selectedRegions.clear()
    }

    fun selectRegion(point: Point) {
        val root = ImageUtil.getRootPixel(base, point)
        if (base.getRGB(root.x, root.y) == Constants.TERRITORY_COLOR.rgb) {
            if (getTerritory(root).isEmpty) {
                selectedRegions.add(root)
            }
        }
    }

    fun deselectRegion(point: Point) {
        val root = ImageUtil.getRootPixel(base, point)
        selectedRegions.remove(root)
    }

    fun submitRegionsAsTerritory(name: String): Optional<Territory> {
        if (selectedRegions.isNotEmpty()) {
            val seedPoints = HashSet<SeedPoint>()
            for (point in selectedRegions) {
                seedPoints.add(SeedPoint(point.x, point.y))
            }
            val result = Territory(TerritoryId(name), seedPoints)
            if (selectedRegions.isNotEmpty()) {
                graph.addVertex(result)
                submittedTerritories.add(result)
                clearSelectedRegions()
                return Optional.of(result)
            }
            clearSelectedRegions()
        }
        return Optional.empty()
    }

    fun removeSubmitted(territory: Territory) {
        finishedTerritories.remove(territory)
        submittedTerritories.remove(territory)
        graph.removeVertex(territory)
        update()
    }

    /* EditMode.EDIT_NEIGHBORS */
    private val noTerritorySelected: Territory = Territory(TerritoryId("UNSELECTED"), SeedPoint(-1, -1))
    private var selected: Territory = noTerritorySelected
    private val neighbors: MutableSet<Territory> = HashSet()

    fun hasSelection(): Boolean {
        return selected != noTerritorySelected
    }

    fun isSelected(point: Point): Boolean { // Might not need this but it's here for now just in case
        val root = ImageUtil.getRootPixel(base, point)
        return selected.seedPoints().contains(SeedPoint(root.x, root.y))
    }

    fun select(point: Point) {
        this.selected = getTerritory(point).orElse(noTerritorySelected)
        if (selected != noTerritorySelected) {
            neighbors.addAll(Graphs.neighborListOf(graph, selected)) // Add all existing neighbors
        }
    }

    fun select(territory: Territory?) {
        if (territory != null && territory != noTerritorySelected) {
            this.selected = territory
            neighbors.addAll(Graphs.neighborListOf(graph, selected)) // Add all existing neighbors
        } else {
            this.selected = noTerritorySelected
        }
    }

    fun deselect() {
        this.selected = noTerritorySelected
        neighbors.clear()
    }

    fun isNeighbor(point: Point): Boolean {
        val territory = getTerritory(point).orElse(noTerritorySelected)
        return neighbors.contains(territory)
    }

    fun selectNeighbor(point: Point) {
        val territory = getTerritory(point).orElse(noTerritorySelected)
        if (territory == selected || territory == noTerritorySelected) {
            return
        }
        neighbors.add(territory)
    }

    fun deselectNeighbor(point: Point) {
        val territory = getTerritory(point).orElse(noTerritorySelected)
        neighbors.remove(territory)
    }

    fun submitNeighbors() {
        if (selected != noTerritorySelected) {
            if (graph.containsVertex(selected)) {
                val edgesToRemove = HashSet<Border>()
                val currentNeighbors = Graphs.neighborListOf(graph, selected)
                currentNeighbors.removeAll(neighbors)
                for (deselectedNeighbor in currentNeighbors) {
                    for (border in graph.edgeSet()) {
                        if (border.equals(graph.getEdge(selected, deselectedNeighbor))) {
                            edgesToRemove.add(border)
                        }
                    }
                }
                graph.removeAllEdges(edgesToRemove)
            }

            for (selectedNeighbor in neighbors) {
                val border = Border(selected.id(), selectedNeighbor.id())
                graph.addEdge(selected, selectedNeighbor, border)
            }
            if (Graphs.neighborListOf(graph, selected).size == 0) {
                finishedTerritories.remove(selected)
            } else if (!finishedTerritories.contains(selected)) {
                finishedTerritories.add(selected)
            }
            deselect()
        }
    }

    /* File I/O */

    fun openRkmFile() {
        val chooser = JFileChooser()
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("${Constants.NAME} Map (*.rkm)", "rkm")
        chooser.fileFilter = filter
        if (chooser.showDialog(null, "Import") == JFileChooser.APPROVE_OPTION) {
            try {
                val reader = RkmReader(chooser.selectedFile.toPath())
                val map = reader.read()
                reset()
                base = map.mapImage().baseImage()
                baseBitmap = base.toBitmap().asImageBitmap()
                text = map.mapImage().textImage()
                textBitmap = text.toBitmap().asImageBitmap()

                mapSimpleName = map.mapName().simpleName()
                mapDisplayName = map.mapName().displayName()
                mapAuthorName = map.author().name()

                graph = SimpleGraph<Territory, Border>(Border::class.java)

                for (territory in map.graph().vertices()) {
                    graph.addVertex(territory)
                }
                for (border in map.graph().edges()) {
                    val source = map.graph().vertices().find { it.id().equals(border.source()) }
                    val target = map.graph().vertices().find { it.id().equals(border.target()) }
                    graph.addEdge(source, target, border)
                }

                submittedTerritories.addAll(graph.vertexSet())
                for (territory in graph.vertexSet()) {
                    if (Graphs.neighborSetOf(graph, territory).isNotEmpty()) {
                        finishedTerritories.add(territory)
                    }
                }
                update()
                editMode = EditMode.EDIT_NEIGHBORS
            } catch (e: Exception) {
                if (e.message != null && e.message!!.contains("invalid checksum", true)) {
                    JOptionPane.showMessageDialog(null, "Could not open .rkm map file: invalid checksum.")
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid .rkm map file.")
                }
                return
            }
        }
    }

    fun exportAsRkm() {
        if (mapSimpleName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Map simple name cannot be empty.")
            return
        }
        if (mapDisplayName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Map display name cannot be empty.")
            return
        }
        if (mapAuthorName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Map author name cannot be empty.")
            return
        }
        if (graph.vertexSet().size == 0) {
            JOptionPane.showMessageDialog(null, "Nothing to export.")
            return
        } else if (graph.edgeSet().size == 0) {
            JOptionPane.showMessageDialog(null, "Please finish adding territory neighbors before exporting.")
            return
        }
        val chooser = JFileChooser()
        chooser.currentDirectory = File(".")
        chooser.dialogTitle = "Save ${Constants.NAME} Map File"
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("${Constants.NAME} Map File (*.rkm)", "rkm")
        chooser.fileFilter = filter

        chooser.selectedFile = File("$mapSimpleName.rkm")
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            if (chooser.selectedFile.name.isNullOrBlank()) {
                JOptionPane.showMessageDialog(null, "Invalid file name.")
            } else {
                val directory = chooser.currentDirectory.path.replace('\\', '/') + "/"
                val fileName = Regex("[^A-Za-z0-9_\\-]").replace(mapSimpleName, "")
                try {
                    val rkmMap = RkmMap(MapName(mapSimpleName, mapDisplayName), MapAuthor(mapAuthorName), MapGraph(graph), MapImage(base, text))
                    val writer = RkmWriter(rkmMap)
                    val fos = FileOutputStream(File(directory + "${fileName}.rkm"))
                    writer.write(fos)
                    fos.close()
                    JOptionPane.showMessageDialog(null, "Map file successfully exported to the selected directory.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(null, "Unable to save map file due to an error.")
                }
            }
        }
    }

    fun importMapAsLayers() {
        val chooser = JFileChooser()
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("Images (*.png)", "png")
        chooser.fileFilter = filter
        if (chooser.showDialog(null, "Import Base Layer") == JFileChooser.APPROVE_OPTION) {
            try {
                val newBase = ImageIO.read(chooser.selectedFile)
                reset()
                base = newBase
                baseBitmap = base.toBitmap().asImageBitmap()

                val successText = chooser.showDialog(null, "Import Text Layer")
                if (successText == JFileChooser.APPROVE_OPTION) {
                    val newText = ImageIO.read(chooser.selectedFile)
                    if (newText.height == height() && newText.width == width()) {
                        text = newText
                        textBitmap = text.toBitmap().asImageBitmap()
                        editMode = EditMode.EDIT_TERRITORY
                    } else {
                        JOptionPane.showMessageDialog(null, "Your text layer must match the width and height of your base layer. Import your base layer first.")
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun replaceBaseImage() {
        val chooser = JFileChooser()
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("Image (*.png)", "png")
        chooser.fileFilter = filter
        if (chooser.showDialog(null, "New Base Layer Image") == JFileChooser.APPROVE_OPTION) {
            try {
                val newBase = ImageIO.read(chooser.selectedFile)
                clearSelectedRegions()
                deselect()
                base = newBase
                baseBitmap = base.toBitmap().asImageBitmap()
                update()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun replaceTextImage() {
        val chooser = JFileChooser()
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("Image (*.png)", "png")
        chooser.fileFilter = filter
        if (chooser.showDialog(null, "New Text Layer Image") == JFileChooser.APPROVE_OPTION) {
            try {
                val newText = ImageIO.read(chooser.selectedFile)
                if (newText.height == height() && newText.width == width()) {
                    clearSelectedRegions()
                    deselect()
                    text = newText
                    textBitmap = text.toBitmap().asImageBitmap()
                    update()
                } else {
                    JOptionPane.showMessageDialog(null, "Your text layer must match the width and height of your base layer. Import your base layer first.")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun importGraphFile() {
        val chooser = JFileChooser()
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("${Constants.NAME} Graph (*.json)", "json")
        chooser.fileFilter = filter
        if (chooser.showDialog(null, "Import Graph File") == JFileChooser.APPROVE_OPTION) {
            try {

                val mapGraph: MapGraph? = GsonUtil.read(chooser.selectedFile.toPath(), MapGraph::class.java)
                if (base.width != 1 && base.height != 1 && mapGraph != null) {
                    graph = SimpleGraph<Territory, Border>(Border::class.java)

                    for (territory in mapGraph.vertices()) {
                        graph.addVertex(territory)
                    }
                    for (border in mapGraph.edges()) {
                        val source = graph.vertexSet().find { it.id().equals(border.source()) }
                        val target = graph.vertexSet().find { it.id().equals(border.target()) }
                        graph.addEdge(source, target, border)
                    }
                    submittedTerritories.addAll(graph.vertexSet())
                    finishedTerritories.addAll(graph.vertexSet())
                    update()
                    editMode = EditMode.EDIT_NEIGHBORS
                } else {
                    if (mapGraph == null) {
                        JOptionPane.showMessageDialog(null, "Error loading graph file.")
                    } else {
                        JOptionPane.showMessageDialog(null, "Please import a base image layer and text image layer before importing a graph file.")
                    }
                }

            } catch (e: Exception) {
                graph = SimpleGraph<Territory, Border>(Border::class.java)
                submittedTerritories.clear()
                finishedTerritories.clear()
                JOptionPane.showMessageDialog(null, "File invalid: JSON format does not match that of a correct map graph file.")
                e.printStackTrace()
            }
        }
    }

    fun exportGraphFile() {
        if (graph.vertexSet().size == 0) {
            JOptionPane.showMessageDialog(null, "Nothing to export.")
            return
        } else if (graph.edgeSet().size == 0) {
            JOptionPane.showMessageDialog(null, "Please add some territory neighbors before exporting.")
            return
        }
        val chooser = JFileChooser()
        chooser.currentDirectory = File(".")
        chooser.dialogTitle = "Save ${Constants.NAME} Graph File"
        chooser.isAcceptAllFileFilterUsed = false
        val filter = FileNameExtensionFilter("${Constants.NAME} Graph (*.json)", "json")
        chooser.fileFilter = filter

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            if (chooser.selectedFile.name.isNullOrBlank()) {
                JOptionPane.showMessageDialog(null, "Invalid file name.")
            } else {
                val fileName = Regex("[^A-Za-z0-9_\\-]").replace(chooser.selectedFile.nameWithoutExtension, "")
                try {
                    GsonUtil.write(chooser.currentDirectory.toPath().resolve("$fileName.json"), MapGraph::class.java, MapGraph(graph))
                    JOptionPane.showMessageDialog(null, "Graph file successfully exported to the selected directory.")
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(null, "Unable to save graph file due to an error.")
                }
            }
        }
    }

    /* Private Methods */
    private fun getTerritory(point: Point): Optional<Territory> {
        val root = ImageUtil.getRootPixel(base, point)
        for (territory in graph.vertexSet()) {
            if (territory.seedPoints().contains(SeedPoint(root.x, root.y))) {
                return Optional.of(territory)
            }
        }
        return Optional.empty()
    }

}