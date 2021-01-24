package com.kn.chartscanvas

import android.graphics.Shader

data class Item(var label: String, var value: Float, var color: Int) {

    // computed values
    var startAngle = 0
    var endAngle = 0
    var highlight = 0
    var shader: Shader? = null

}
