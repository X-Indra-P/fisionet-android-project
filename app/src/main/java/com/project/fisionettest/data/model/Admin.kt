package com.project.fisionettest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Admin(
    val id: String,
    @SerialName("display_name")
    val displayName: String? = null,
    val role: Int = 1, // 1 = Admin
    @SerialName("created_at")
    val createdAt: String? = null
)
