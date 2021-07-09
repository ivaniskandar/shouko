package xyz.ivaniskandar.shouko.util

import android.os.Build

object DeviceModel {
    // Xperia 5 II
    val isPDX206 = arrayOf(
        "A002SO",
        "SO-52A",
        "SOG02",
        "XQ-AS42",
        "XQ-AS52",
        "XQ-AS62",
        "XQ-AS72"
    ).contains(Build.MODEL)

    // Xperia 1 II
    val isPDX203 = arrayOf(
        "SO-51A",
        "SOG01",
        "XQ-AT42",
        "XQ-AT51",
        "XQ-AT52",
        "XQ-AT72"
    ).contains(Build.MODEL)

    // Xperia 10 III
    val isPDX213 = arrayOf(
        "A102SO",
        "SO-52B",
        "SOG04",
        "XQ-BT52"
    ).contains(Build.MODEL)
}
