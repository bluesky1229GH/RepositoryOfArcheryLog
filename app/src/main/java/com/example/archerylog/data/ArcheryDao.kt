package com.example.archerylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArcheryDao {
    @Insert
    suspend fun insertSession(session: ArcherySession): Long

    @Insert
    suspend fun insertEnd(end: SessionEnd): Long

    @Insert
    suspend fun insertShot(shot: Shot): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Query("UPDATE users SET passwordHash = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: Long, newPassword: String)

    @Query("UPDATE users SET email = :newEmail WHERE id = :userId")
    suspend fun updateEmail(userId: Long, newEmail: String)

    @Query("UPDATE users SET avatarUri = :newUri WHERE id = :userId")
    suspend fun updateAvatarUri(userId: Long, newUri: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSessionsForUser(userId: Long): Flow<List<ArcherySession>>

    @Query("SELECT * FROM ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    fun getEndsForSession(sessionId: Long): Flow<List<SessionEnd>>

    @Query("SELECT * FROM shots WHERE endId = :endId")
    suspend fun getShotsForEnd(endId: Long): List<Shot>

    @Query("UPDATE sessions SET totalScore = totalScore + :score, totalShots = totalShots + :shotsCount WHERE id = :sessionId")
    suspend fun modifySessionScore(sessionId: Long, score: Int, shotsCount: Int)

    @Query("DELETE FROM shots WHERE id = :shotId")
    suspend fun deleteShotById(shotId: Long)

    @Query("UPDATE ends SET endTotalScore = endTotalScore + :score WHERE id = :endId")
    suspend fun addScoreToEnd(endId: Long, score: Int)

    @androidx.room.Transaction
    @Query("SELECT * FROM ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    fun getEndsWithShotsForSession(sessionId: Long): kotlinx.coroutines.flow.Flow<List<EndWithShots>>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM ends WHERE id = :endId")
    suspend fun deleteEndById(endId: Long)

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionByIdFlow(sessionId: Long): Flow<ArcherySession?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ArcherySession?

    @Query("""
        SELECT ends.id as endId, ends.sessionId, ends.endNumber, ends.endTotalScore, 
               ends.timestamp, sessions.locationType, sessions.distance 
        FROM ends 
        JOIN sessions ON ends.sessionId = sessions.id 
        WHERE sessions.userId = :userId 
        ORDER BY ends.timestamp ASC, ends.endNumber ASC
    """)
    fun getAllEndsWithMetadataForUser(userId: Long): Flow<List<EndWithMetadata>>

    @Query("""
        SELECT shots.score, shots.numericValue, ends.timestamp, sessions.locationType, sessions.distance 
        FROM shots 
        JOIN ends ON shots.endId = ends.id
        JOIN sessions ON ends.sessionId = sessions.id 
        WHERE sessions.userId = :userId
    """)
    fun getAllShotsWithMetadataForUser(userId: Long): Flow<List<ShotWithMetadata>>

    @Insert
    suspend fun insertAiFavorite(favorite: AiFavorite): Long

    @Query("SELECT * FROM ai_favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAiFavoritesForUser(userId: Long): Flow<List<AiFavorite>>

    @Query("DELETE FROM ai_favorites WHERE id = :id")
    suspend fun deleteAiFavorite(id: Long)
}
