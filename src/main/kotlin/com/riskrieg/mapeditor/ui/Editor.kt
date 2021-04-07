package com.riskrieg.mapeditor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.riskrieg.mapeditor.Constants
import java.awt.Point

class Editor(val mapName: String = "") {

    private lateinit var base: ImageBitmap
    private lateinit var text: ImageBitmap

    private var mousePos = Point(0, 0)

    @Composable
    private fun loadImages() {
        base = imageResource(Constants.MAP_PATH + "north-america/north-america-base.png")
        text = imageResource(Constants.MAP_PATH + "north-america/north-america-text.png")
    }

    @Composable
    fun init() {
        loadImages()
        Row {
            SideBar()
            MapView()
        }
    }

    @Composable
    private fun SideBar() {
        Box(Modifier.fillMaxHeight().width(80.dp).padding(4.dp)) {
            Surface(color = Color.DarkGray, modifier = Modifier.fillMaxSize()) {

            }
        }
    }

    @Composable
    private fun MapView() {
        Box {
            Image(bitmap = base, contentDescription = "", modifier = mapModifier())
            Image(bitmap = text, contentDescription = "", modifier = Modifier.size(text.width.dp, text.height.dp))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun mapModifier(): Modifier {
        return Modifier.size(base.width.dp, base.height.dp).combinedClickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = {
                println("${mousePos.x}, ${mousePos.y}")
            },
            onDoubleClick = {

            },
            onLongClick = {

            }
        ).pointerMoveFilter(onMove = {
            mousePos = Point(it.x.toInt(), it.y.toInt())
            false
        })
    }

}