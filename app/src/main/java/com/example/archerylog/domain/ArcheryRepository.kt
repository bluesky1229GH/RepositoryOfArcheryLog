package com.example.archerylog.domain

import com.example.archerylog.data.ArcheryDao
import com.example.archerylog.data.ArcherySession
import com.example.archerylog.data.SessionEnd
import com.example.archerylog.data.Shot
import com.example.archerylog.data.User
import kotlinx.coroutines.flow.Flow

class ArcheryRepository(private val archeryDao: ArcheryDao) {

    val allUsers: Flow<List<User>> = archeryDao.getAllUsers()

    fun getAllSessionsForUser(userId: Long): Flow<List<ArcherySession>> = archeryDao.getAllSessionsForUser(userId)
    fun getAllEndsWithMetadataForUser(userId: Long) = archeryDao.getAllEndsWithMetadataForUser(userId)
    
    suspend fun insertUser(user: User): Long = archeryDao.insertUser(user)
    suspend fun getUserByUsername(username: String): User? = archeryDao.getUserByUsername(username)
    fun getUserByIdFlow(userId: Long): Flow<User?> = archeryDao.getUserByIdFlow(userId)
    suspend fun getUserById(userId: Long): User? = archeryDao.getUserById(userId)
    
    suspend fun updatePassword(userId: Long, newPassword: String) = archeryDao.updatePassword(userId, newPassword)
    suspend fun updateEmail(userId: Long, newEmail: String) {
        archeryDao.updateEmail(userId, newEmail)
    }

    suspend fun updateAvatarUri(userId: Long, newUri: String) {
        archeryDao.updateAvatarUri(userId, newUri)
    }

    suspend fun deleteUser(userId: Long) = archeryDao.deleteUser(userId)

    suspend fun insertSession(session: ArcherySession): Long {
        return archeryDao.insertSession(session)
    }

    suspend fun insertEnd(end: SessionEnd): Long {
        return archeryDao.insertEnd(end)
    }

    suspend fun insertShot(shot: Shot): Long {
        return archeryDao.insertShot(shot)
    }

    fun getEndsForSession(sessionId: Long) = archeryDao.getEndsForSession(sessionId)

    suspend fun getShotsForEnd(endId: Long): List<Shot> {
        return archeryDao.getShotsForEnd(endId)
    }

    suspend fun modifySessionScore(sessionId: Long, score: Int, shotsCount: Int) = archeryDao.modifySessionScore(sessionId, score, shotsCount)
    suspend fun deleteShot(shotId: Long) = archeryDao.deleteShotById(shotId)
    suspend fun addScoreToEnd(endId: Long, score: Int) = archeryDao.addScoreToEnd(endId, score)
    fun getEndsWithShotsForSession(sessionId: Long) = archeryDao.getEndsWithShotsForSession(sessionId)
    suspend fun deleteSession(sessionId: Long) = archeryDao.deleteSessionById(sessionId)
    suspend fun deleteEnd(endId: Long) = archeryDao.deleteEndById(endId)
    suspend fun getSessionById(sessionId: Long) = archeryDao.getSessionById(sessionId)
    fun getSessionByIdFlow(sessionId: Long) = archeryDao.getSessionByIdFlow(sessionId)

    fun getAiFavoritesForUser(userId: Long) = archeryDao.getAiFavoritesForUser(userId)
    suspend fun insertAiFavorite(favorite: com.example.archerylog.data.AiFavorite) = archeryDao.insertAiFavorite(favorite)
    suspend fun deleteAiFavorite(id: Long) = archeryDao.deleteAiFavorite(id)
}
