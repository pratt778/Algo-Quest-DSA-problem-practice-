package com.example.algoquest.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Problem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val points: Int,
    val prerequisites: List<String>,
    val tags: List<String>,
    val hints: List<String>,
    val answer: String
) : Parcelable

