package com.example.archerylog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LocationType { INDOOR, OUTDOOR }

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String = "",
    val email: String = "",
    val avatarUri: String = ""
)

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val locationType: LocationType,
    val distance: Int,
    val totalScore: Int = 0,
    val totalShots: Int = 0,
    val title: String = "",
    val venue: String = "",
    val weather: String = "Sunny",
    val wind: String = "Low"
)

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val endNumber: Int,
    val endTotalScore: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endId: Long,
    val score: String,
    val numericValue: Int,
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
    val endId: Long,
    val sessionId: Long,
    val endNumber: Int,
    val endTotalScore: Int,
    val timestamp: Long,
    val locationType: LocationType,
    val distance: Int
)

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val question: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis()
)
