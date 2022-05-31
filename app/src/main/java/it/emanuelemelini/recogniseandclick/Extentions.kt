package it.emanuelemelini.recogniseandclick

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import it.emanuelemelini.recogniseandclick.ui.RecentsActivity

private const val TAG = "RecogniseAndClick"

fun Any.logd(tag: String = TAG) {
    if (!BuildConfig.DEBUG) return
    if (this is String) {
        Log.d(tag, this)
    } else {
        Log.d(tag, this.toString())
    }
}

fun Any.loge(tag: String = TAG) {
    if (!BuildConfig.DEBUG) return
    if (this is String) {
        Log.e(tag, this)
    } else {
        Log.e(tag, this.toString())
    }
}

fun Context.dp2px(dpValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

fun String?.censor(): String {
    if (this == null) return ""
    if (this.isBlank()) return ""
    var after = ""
    var c = 0
    val half = this.length / 2
    for (s in this) {
        if (c == half)
            after += "/"
        after += s
        c++
    }
    return after
}

fun SharedPreferences.addStringToSet(key: String, str: String) {
    println("ENTERED SHAREDPREF METHOD")
    try {
        var set = (this.getStringSet(key, null) ?: emptySet()).toSet()

        println("SET FOUND: $set - ${set.size}")

        if (set.isEmpty()) {
            println("SET EMPTY")
            set = setOf(str)
            println("SET FILLED: $set - ${set.size}")
        }

        if (set.stream().noneMatch { it.equals(str) }) {
            println("STRING NOT FOUND")
            set = set.add(str)
            println("SET FILLED: $set - ${set.size}")
        } else {
            println("STRING FOUND")
        }

        with(this.edit()) {
            remove(key)
            putStringSet(key, set)
            apply()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

}

fun SharedPreferences.removeFromStringSet(key: String, str: String) {
    println("ENTERED SHAREDPREF METHOD")
    try {
        var set = (this.getStringSet(key, null) ?: emptySet()).toSet()

        println("SET FOUND: $set - ${set.size}")

        if (set.stream().anyMatch { it.equals(str) }) {
            println("STRING NOT FOUND")
            set = set.remove(str)
            println("SET FILLED: $set - ${set.size}")
        } else {
            println("STRING FOUND")
        }

        with(this.edit()) {
            remove(key)
            putStringSet(key, set)
            apply()
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }

}

fun Set<String>.add(str: String): Set<String> {
    val mutable = this.toMutableSet()
    mutable.add(str)
    return mutable.toSet()
}

fun Set<String>.remove(str: String): Set<String> {
    val mutable = this.toMutableSet()
    mutable.remove(str)
    return mutable.toSet()
}

fun Context.toast(str: String) {
    try {
        val ac = this as Activity
        ac.runOnUiThread {
            Toast.makeText(this, str, Toast.LENGTH_LONG).show()
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

typealias Action = () -> Unit