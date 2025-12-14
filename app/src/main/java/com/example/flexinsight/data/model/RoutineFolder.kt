package com.example.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * Routine Folder API Response
 */
data class RoutineFolderResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("index")
    val index: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Paginated Routine Folders Response
 */
data class PaginatedRoutineFolderResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("folders")
    val folders: List<RoutineFolderResponse>
)

/**
 * Domain model for Routine Folder
 */
data class RoutineFolder(
    val id: Int,
    val title: String,
    val index: Int
)

fun RoutineFolderResponse.toRoutineFolder(): RoutineFolder {
    return RoutineFolder(
        id = id,
        title = title,
        index = index
    )
}
