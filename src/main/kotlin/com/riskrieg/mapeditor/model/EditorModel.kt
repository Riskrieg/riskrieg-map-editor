package com.riskrieg.mapeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aaronjyoder.util.json.gson.GsonUtil
import com.riskrieg.mapeditor.Constants
import com.riskrieg.mapeditor.fill.MilazzoFill
import com.riskrieg.mapeditor.util.Extensions.convert
import com.riskrieg.mapeditor.util.Extensions.toBitmap
import com.riskrieg.mapeditor.util.ImageUtil
import org.jetbrains.skija.Bitmap
import org.jgrapht.Graphs
import org.jgrapht.graph.SimpleGraph
import java.awt.Color
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter


class EditorModel(mapName: String = "") {

    private var graph = SimpleGraph<Territory, Border>(Border::class.java)

    private var base: BufferedImage by mutableStateOf(BufferedImage(1, 1, 2))
    private var text: BufferedImage by mutableStateOf(BufferedImage(1, 1, 2))

    private var baseBitmap by mutableStateOf(base.toBitmap().asImageBitmap())
    private var textBitmap by mutableStateOf(text.toBitmap().asImageBitmap())

    private val submittedTerritories = mutableStateListOf<Territory>() // Unfortunately necessary for now
    private val finishedTerritories = mutableStateListOf<Territory>() // Unfortunately necessary for now

    init {
        if (mapName.isNotBlank()) { // Primarily for easy debugging
            base = ImageIO.read(File("src/main/resources/" + Constants.MAP_PATH + "$mapName/$mapName-base.png")).convert(2)
            text = ImageIO.read(File("src/main/resources/" + Constants.MAP_PATH + "$mapName/$mapName-text.png")).convert(2)
            baseBitmap = base.toBitmap().asImageBitmap()
            textBitmap = text.toBitmap().asImageBitmap()
        }
    }

    var editMode by mutableStateOf(EditMode.EDIT_TERRITORY)

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
            for (point in territory.seedPoints) {
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.SUBMITTED_COLOR).fill(point)
            }
        }

        for (territory in finishedTerritories) {
            for (point in territory.seedPoints) {
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.FINISHED_COLOR).fill(point)
            }
        }

        for (territory in neighbors) {
            for (point in territory.seedPoints) {
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.NEIGHBOR_SELECT_COLOR).fill(point)
            }
        }

        if (selectedRegions.isNotEmpty()) {
            for (point in selectedRegions) {
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.SELECT_COLOR).fill(point)
            }
        } else if (selected != noTerritorySelected) {
            for (point in selected.seedPoints) {
                MilazzoFill(copy, Color(copy.getRGB(point.x, point.y)), Constants.SELECT_COLOR).fill(point)
            }
        }

        baseBitmap = copy.toBitmap().asImageBitmap()
    }

    fun getSelectedRegions(): Deque<Point> {
        return selectedRegions
    }

    fun getSubmittedTerritories(): SnapshotStateList<Territory> {
//        submittedTerritories.sort()
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
            val result = Territory(name, selectedRegions.toSet())
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
        // TODO: Write this
    }

    /* EditMode.EDIT_NEIGHBORS */
    private val noTerritorySelected: Territory = Territory("UNSELECTED", Point(-1, -1))
    private var selected: Territory = noTerritorySelected
    private val neighbors: MutableSet<Territory> = HashSet()

    fun hasSelection(): Boolean {
        return selected != noTerritorySelected
    }

    fun isSelected(point: Point): Boolean { // Might not need this but it's here for now just in case
        val root = ImageUtil.getRootPixel(base, point)
        return selected.seedPoints.contains(root)
    }

    fun select(point: Point) {
        this.selected = getTerritory(point).orElse(noTerritorySelected)
        if (selected != noTerritorySelected) {
            neighbors.addAll(Graphs.neighborListOf(graph, selected)) // Add all existing neighbors
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
                val border = Border(selected, selectedNeighbor)
                graph.addEdge(selected, selectedNeighbor, border)
            }
            finishedTerritories.add(selected)
            deselect()
        }
    }

    /* File I/O */

    fun importMapAsLayers() {
        val chooser = JFileChooser()
        val filter = FileNameExtensionFilter("Images (*.png)", "png")
        chooser.fileFilter = filter
        val successBase = chooser.showDialog(null, "Import Base Layer")
        if (successBase == JFileChooser.APPROVE_OPTION) {
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

    fun importGraphFile() {
        val chooser = JFileChooser()
        val filter = FileNameExtensionFilter("Json Graph (*.json)", "json")
        chooser.fileFilter = filter
        val successGraph = chooser.showDialog(null, "Import Graph File")
        if (successGraph == JFileChooser.APPROVE_OPTION) {
            try {

                if (base.width != 1 && base.height != 1) {
                    val mapGraph: MapGraph = GsonUtil.read(chooser.selectedFile.path, MapGraph::class.java)
                    graph = SimpleGraph<Territory, Border>(Border::class.java)

                    for (territory in mapGraph.vertices()) {
                        graph.addVertex(territory)
                    }
                    for (border in mapGraph.edges()) {
                        graph.addEdge(border.source, border.target, border)
                    }
                    submittedTerritories.addAll(graph.vertexSet())
                    finishedTerritories.addAll(graph.vertexSet())
                    update()
                    editMode = EditMode.EDIT_NEIGHBORS
                } else {
                    JOptionPane.showMessageDialog(null, "Please import a base image layer and text image layer before importing a graph file.")
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

    fun importMapFile() {
        reset()
        val chooser = JFileChooser()
        val filter = FileNameExtensionFilter("Riskrieg Map (*.rkm)", "rkm")
        chooser.fileFilter = filter
        val success = chooser.showDialog(null, "Import")
        // TODO: Finish writing this
    }

    /* Private Methods */
    private fun getTerritory(point: Point): Optional<Territory> {
        val root = ImageUtil.getRootPixel(base, point)
        for (territory in graph.vertexSet()) {
            if (territory.seedPoints.contains(root)) {
                return Optional.of(territory)
            }
        }
        return Optional.empty()
    }

}