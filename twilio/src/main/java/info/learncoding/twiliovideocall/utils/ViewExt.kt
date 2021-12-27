package info.learncoding.twiliovideocall.utils

import android.view.View

fun View.visibility(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}