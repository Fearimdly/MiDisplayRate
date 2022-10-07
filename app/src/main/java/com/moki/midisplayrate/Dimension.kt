package com.moki.midisplayrate

import android.content.res.Resources

fun Int.px() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.dp() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Int.sp() = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()

fun Float.dp() = (this * Resources.getSystem().displayMetrics.density).toInt()