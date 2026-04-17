package com.example.archerylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArcheryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ArcherySession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ArcherySession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnd(end: SessionEnd)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnds(ends: List<SessionEnd>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: Shot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShots(shots: List<Shot>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("UPDATE users SET passwordHash = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: String, newPassword: String)

    @Query("UPDATE users SET email = :newEmail WHERE id = :userId")
    suspend fun updateEmail(userId: String, newEmail: String)

    @Query("UPDATE users SET username = :newUsername WHERE id = :userId")
    suspend fun updateUsername(userId: String, newUsername: String)

    @Query("UPDATE users SET avatarUri = :newUri WHERE id = :userId")
    suspend fun updateAvatarUri(userId: String, newUri: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSessionsForUser(userId: String): Flow<List<ArcherySession>>

    @Query("SELECT * FROM ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    fun getEndsForSession(sessionId: String): Flow<List<SessionEnd>>

    @Query("SELECT * FROM shots WHERE endId = :endId")
    suspend fun getShotsForEnd(endId: String): List<Shot>

    @Query("UPDATE sessions SET totalScore = totalScore + :score, totalShots = totalShots + :shotsCount WHERE id = :sessionId")
    suspend fun modifySessionScore(sessionId: String, score: Int, shotsCount: Int)

    @Query("DELETE FROM ends WHERE sessionId = :sessionId AND id NOT IN (SELECT endId FROM shots)")
    suspend fun deleteEmptyEndsForSession(sessionId: String)

    @Query("DELETE FROM shots WHERE id = :shotId")
    suspend fun deleteShotById(shotId: String)

    @Query("UPDATE ends SET endTotalScore = endTotalScore + :score WHERE id = :endId")
    suspend fun addScoreToEnd(endId: String, score: Int)

    @androidx.room.Transaction
    @Query("SELECT * FROM ends WHERE sessionId = :sessionId ORDER BY endNumber ASC")
    fun getEndsWithShotsForSession(sessionId: String): kotlinx.coroutines.flow.Flow<List<EndWithShots>>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM ends WHERE id = :endId")
    suspend fun deleteEndById(endId: String)

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionByIdFlow(sessionId: String): Flow<ArcherySession?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ArcherySession?

    @Query("""
        SELECT ends.id as endId, ends.sessionId, ends.endNumber, ends.endTotalScore, 
               ends.timestamp, sessions.locationType, sessions.distance 
        FROM ends 
        JOIN sessions ON ends.sessionId = sessions.id 
        WHERE sessions.userId = :userId 
        ORDER BY ends.timestamp ASC, ends.endNumber ASC
    """)
    fun getAllEndsWithMetadataForUser(userId: String): Flow<List<EndWithMetadata>>

    @Query("""
        SELECT shots.score, shots.numericValue, ends.timestamp, sessions.locationType, sessions.distance 
        FROM shots 
        JOIN ends ON shots.endId = ends.id
        JOIN sessions ON ends.sessionId = sessions.id 
        WHERE sessions.userId = :userId
    """)
    fun getAllShotsWithMetadataForUser(userId: String): Flow<List<ShotWithMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiFavorite(favorite: AiFavorite)

    @Query("SELECT * FROM ai_favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAiFavoritesForUser(userId: String): Flow<List<AiFavorite>>

    @Query("DELETE FROM ai_favorites WHERE id = :id")
    suspend fun deleteAiFavorite(id: String)
}
