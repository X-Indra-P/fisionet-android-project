package com.project.fisionettest.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Tool(
    val id: Int? = null,
    val nama_tools: String
)
