package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Package(
    val id: Long? = null,
    val name: String,
    val price: Double,
    val tools: List<String> = emptyList(),
    val created_at: String? = null
)
