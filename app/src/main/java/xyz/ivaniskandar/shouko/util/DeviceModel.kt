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
        "XQ-BT52",
        "XQ-BT44"
    ).contains(Build.MODEL)

    // Xperia 5 III
    val isPDX214 = arrayOf(
        "A103SO",
        "SOG05",
        "SO-53B",
        "XQ-BQ42",
        "XQ-BQ52",
        "XQ-BQ62",
        "XQ-BQ72"
    ).contains(Build.MODEL)

    // Xperia 1 III
    val isPDX215 = arrayOf(
        "A101SO",
        "SO-51B",
        "SOG03",
        "XQ-BC72",
        "XQ-BC62",
        "XQ-BC52",
        "XQ-BC42"
    ).contains(Build.MODEL)

    // Xperia 1 IV
    val isPDX223 = arrayOf(
        "XQ-CT54",
        "XQ-CT62",
        "XQ-CT72"
    ).contains(Build.MODEL)
}
