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
    val id_cabang: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("place_of_birth")
    val placeOfBirth: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    val phone: String? = null,
    val address: String? = null
)
