package com.example.cloudphone.utils

import com.google.gson.Gson

fun Any.toJson(): String = Gson().toJson(this)
inline fun <reified T> String.fromJson(): T = Gson().fromJson(this, T::class.java)
