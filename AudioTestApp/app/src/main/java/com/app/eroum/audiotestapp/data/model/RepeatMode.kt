package com.app.eroum.audiotestapp.data.model

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.OFF -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.OFF
}
