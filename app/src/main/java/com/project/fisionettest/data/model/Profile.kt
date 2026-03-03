package com.project.fisionettest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("display_name")
    val displayName: String? = null,
    val role: Int = 2, // 1 = Admin, 2 = Therapist
    val status: String = "pending", // pending, verified, rejected
    val clinic: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
