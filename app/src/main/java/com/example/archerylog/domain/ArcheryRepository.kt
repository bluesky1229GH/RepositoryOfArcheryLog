package com.example.archerylog.domain

import com.example.archerylog.data.ArcheryDao
import com.example.archerylog.data.ArcherySession
import com.example.archerylog.data.SessionEnd
import com.example.archerylog.data.Shot
import com.example.archerylog.data.User
import kotlinx.coroutines.flow.Flow

class ArcheryRepository(private val archeryDao: ArcheryDao) {

    val allUsers: Flow<List<User>> = archeryDao.getAllUsers()

    fun getAllSessionsForUser(userId: String): Flow<List<ArcherySession>> = archeryDao.getAllSessionsForUser(userId)
    fun getAllEndsWithMetadataForUser(userId: String) = archeryDao.getAllEndsWithMetadataForUser(userId)
    fun getAllShotsWithMetadataForUser(userId: String) = archeryDao.getAllShotsWithMetadataForUser(userId)
    
    suspend fun insertUser(user: User) = archeryDao.insertUser(user)
    suspend fun getUserByUsername(username: String): User? = archeryDao.getUserByUsername(username)
    fun getUserByIdFlow(userId: String): Flow<User?> = archeryDao.getUserByIdFlow(userId)
    suspend fun getUserById(userId: String): User? = archeryDao.getUserById(userId)
    
    suspend fun updatePassword(userId: String, newPassword: String) = archeryDao.updatePassword(userId, newPassword)
    suspend fun updateEmail(userId: String, newEmail: String) {
        archeryDao.updateEmail(userId, newEmail)
    }

    suspend fun updateUsername(userId: String, newUsername: String) {
        archeryDao.updateUsername(userId, newUsername)
    }

    suspend fun updateAvatarUri(userId: String, newUri: String) {
        archeryDao.updateAvatarUri(userId, newUri)
    }

    suspend fun deleteUser(userId: String) = archeryDao.deleteUser(userId)

    suspend fun insertSession(session: ArcherySession) {
        archeryDao.insertSession(session)
    }

    suspend fun insertSessions(sessions: List<ArcherySession>) {
        archeryDao.insertSessions(sessions)
    }

    suspend fun insertEnd(end: SessionEnd) {
        archeryDao.insertEnd(end)
    }

    suspend fun insertEnds(ends: List<SessionEnd>) {
        archeryDao.insertEnds(ends)
    }

    suspend fun insertShot(shot: Shot) {
        archeryDao.insertShot(shot)
    }

    suspend fun insertShots(shots: List<Shot>) {
        archeryDao.insertShots(shots)
    }

    fun getEndsForSession(sessionId: String) = archeryDao.getEndsForSession(sessionId)

    suspend fun getShotsForEnd(endId: String): List<Shot> {
        return archeryDao.getShotsForEnd(endId)
    }

    suspend fun modifySessionScore(sessionId: String, score: Int, shotsCount: Int) = archeryDao.modifySessionScore(sessionId, score, shotsCount)
    suspend fun deleteShot(shotId: String) = archeryDao.deleteShotById(shotId)
    suspend fun addScoreToEnd(endId: String, score: Int) = archeryDao.addScoreToEnd(endId, score)
    fun getEndsWithShotsForSession(sessionId: String) = archeryDao.getEndsWithShotsForSession(sessionId)
    suspend fun deleteSession(sessionId: String) = archeryDao.deleteSessionById(sessionId)
    suspend fun deleteEnd(endId: String) = archeryDao.deleteEndById(endId)
    suspend fun deleteEmptyEndsForSession(sessionId: String) = archeryDao.deleteEmptyEndsForSession(sessionId)
    suspend fun getSessionById(sessionId: String) = archeryDao.getSessionById(sessionId)
    fun getSessionByIdFlow(sessionId: String) = archeryDao.getSessionByIdFlow(sessionId)

    fun getAiFavoritesForUser(userId: String) = archeryDao.getAiFavoritesForUser(userId)
    suspend fun insertAiFavorite(favorite: com.example.archerylog.data.AiFavorite) = archeryDao.insertAiFavorite(favorite)
    suspend fun deleteAiFavorite(id: String) = archeryDao.deleteAiFavorite(id)
}
