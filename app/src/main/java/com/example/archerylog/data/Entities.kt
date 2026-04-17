package com.example.archerylog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

@Serializable
enum class LocationType { 
    @SerialName("INDOOR") INDOOR, 
    @SerialName("OUTDOOR") OUTDOOR 
}

@Serializable
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val username: String,
    val email: String? = null,
    @SerialName("password_hash") val passwordHash: String = "",
    @SerialName("avatar_uri") val avatarUri: String? = null
)

@Serializable
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class ArcherySession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("location_type") val locationType: LocationType,
    val distance: Int,
    @SerialName("total_score") val totalScore: Int = 0,
    @SerialName("total_shots") val totalShots: Int = 0,
    val title: String? = null,
    val venue: String? = null,
    val weather: String? = null,
    val wind: String? = null
)

@Serializable
@Entity(
    tableName = "ends",
    foreignKeys = [
        ForeignKey(
            entity = ArcherySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SessionEnd(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("session_id") val sessionId: String,
    @SerialName("end_number") val endNumber: Int,
    @SerialName("end_total_score") val endTotalScore: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "shots",
    foreignKeys = [
        ForeignKey(
            entity = SessionEnd::class,
            parentColumns = ["id"],
            childColumns = ["endId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("endId")]
)
data class Shot(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("end_id") val endId: String,
    val score: String,
    @SerialName("numeric_value") val numericValue: Int,
    val x: Float? = null,
    val y: Float? = null
)

data class EndWithShots(
    @androidx.room.Embedded val end: SessionEnd,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "endId"
    )
    val shots: List<Shot>
)

data class EndWithMetadata(
    val endId: String,
    val sessionId: String,
    val endNumber: Int,
    val endTotalScore: Int,
    val timestamp: Long,
    val locationType: LocationType,
    val distance: Int
)

data class ShotWithMetadata(
    val score: String,
    val numericValue: Int,
    val timestamp: Long,
    val locationType: LocationType,
    val distance: Int
)

@Serializable
@Entity(
    tableName = "ai_favorites",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class AiFavorite(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    val question: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis()
)
