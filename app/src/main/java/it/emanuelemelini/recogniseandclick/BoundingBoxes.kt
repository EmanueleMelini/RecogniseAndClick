package it.emanuelemelini.recogniseandclick

import android.graphics.Rect

class BoundingBoxes {

    var boxes: MutableList<Rect> = ArrayList()

    fun add(r: Rect) {
        boxes.add(r)
    }

}
