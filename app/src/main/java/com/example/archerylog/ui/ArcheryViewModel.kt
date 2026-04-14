package com.example.archerylog.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.archerylog.data.*
import com.example.archerylog.domain.ArcheryRepository
import com.example.archerylog.ui.utils.AppLanguage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ArcheryViewModel(application: Application) : AndroidViewModel(application) {
    
    // 1. AI Configuration (2026 Stable Standard)
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = com.example.archerylog.BuildConfig.GEMINI_API_KEY
        )
    }

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun askGemini(question: String) {
        if (question.isBlank()) return
        val apiKey = com.example.archerylog.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_ACTUAL_KEY_HERE") {
            _aiResponse.value = "Error: Please add your real GEMINI_API_KEY to local.properties."
            return
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val prompt = "Context: You are a professional Archery Coach. Help the user with their archery training based on this question. \nUser question: $question"
                val response = generativeModel.generateContent(prompt)
                _aiResponse.value = response.text ?: "Error: Empty response"
            } catch (t: Throwable) {
                val errorMsg = t.localizedMessage ?: t.message ?: "Unknown error"
                _aiResponse.value = "Crash Prevention: ${t.javaClass.simpleName} - $errorMsg"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    // 2. Data Repository & Base Flow
    private val repository: ArcheryRepository = ArcheryRepository(ArcheryDatabase.getDatabase(application).archeryDao())
    private val prefs = application.getSharedPreferences("archery_prefs", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getLong("current_user_id", -1L))
    val currentUserId = _currentUserId.asStateFlow()
    val isLoggedIn: Boolean get() = _currentUserId.value != -1L

    init {
        // Safety check: If DB was wiped (destructive migration), the cached ID might no longer exist.
        viewModelScope.launch {
            val id = _currentUserId.value
            if (id != -1L) {
                val user = repository.getUserById(id)
                if (user == null) {
                    logout()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = _currentUserId.flatMapLatest { id ->
        if (id == -1L) flowOf(null) else repository.getUserByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions: StateFlow<List<ArcherySession>> = _currentUserId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else repository.getAllSessionsForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allEndsWithMetadata: StateFlow<List<EndWithMetadata>> = _currentUserId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else repository.getAllEndsWithMetadataForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val aiFavorites: StateFlow<List<AiFavorite>> = _currentUserId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else repository.getAiFavoritesForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAiFavorite(question: String, answer: String) {
        val userId = _currentUserId.value
        if (userId == -1L) return
        viewModelScope.launch {
            try {
                repository.insertAiFavorite(AiFavorite(userId = userId, question = question, answer = answer))
            } catch (e: Exception) {
                // Silently handle or log if user session is invalid during migration
            }
        }
    }
    fun deleteAiFavorite(id: Long) {
        viewModelScope.launch {
            repository.deleteAiFavorite(id)
        }
    }

    private val _currentLanguage = MutableStateFlow(
        AppLanguage.valueOf(prefs.getString("current_language", AppLanguage.CHINESE.name) ?: AppLanguage.CHINESE.name)
    )
    val currentLanguage = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("current_language", language.name).apply()
    }

    // 3. Active Session (Scoring Engine) State
    var currentSessionTitle by mutableStateOf("")
    var currentVenue by mutableStateOf("")
    var currentDistance by mutableStateOf(30)
    var currentLocationType by mutableStateOf(LocationType.OUTDOOR)
    var currentWeather by mutableStateOf("Sunny")
    var currentWind by mutableStateOf("Low")

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive = _isSessionActive.asStateFlow()

    private val _isCreatingSession = MutableStateFlow(false)
    val isCreatingSession = _isCreatingSession.asStateFlow()

    private val _currentSessionId = MutableStateFlow(-1L)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSession: StateFlow<ArcherySession?> = _currentSessionId.flatMapLatest { id ->
        if (id == -1L) flowOf(null) else repository.getSessionByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentEndId = MutableStateFlow(-1L)
    val currentEndId = _currentEndId.asStateFlow()

    private val _currentEndNumber = MutableStateFlow(1)
    val currentEndNumber = _currentEndNumber.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentEndShots: StateFlow<List<Shot>> = _currentEndId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else flow {
            // Since we need to react to changes, we'll poll or use a custom flow if Repository provided one
            // For now, we'll use a trick or assume repository has a way. 
            // Better: use getEndsWithShotsForSession and filter
            emitAll(repository.getEndsWithShotsForSession(_currentSessionId.value).map { list ->
                list.find { it.end.id == id }?.shots ?: emptyList()
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionEndsWithShots: StateFlow<List<EndWithShots>> = _currentSessionId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else repository.getEndsWithShotsForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEndCompletionDialog = MutableStateFlow(false)
    val showEndCompletionDialog = _showEndCompletionDialog.asStateFlow()

    // 4. Scoring Engine Logic
    fun startSessionManual() {
        val userId = _currentUserId.value
        if (userId == -1L) return
        
        viewModelScope.launch {
            _isCreatingSession.value = true
            val sessionId = repository.insertSession(ArcherySession(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                locationType = currentLocationType,
                distance = currentDistance,
                title = currentSessionTitle,
                venue = currentVenue,
                weather = currentWeather,
                wind = currentWind
            ))
            _currentSessionId.value = sessionId
            _currentEndNumber.value = 1
            startNewEnd(sessionId, 1)
            _isSessionActive.value = true
            _isCreatingSession.value = false
        }
    }

    private suspend fun startNewEnd(sessionId: Long, endNum: Int) {
        val endId = repository.insertEnd(SessionEnd(
            sessionId = sessionId,
            endNumber = endNum,
            endTotalScore = 0
        ))
        _currentEndId.value = endId
        _currentEndNumber.value = endNum
    }

    fun addShot(score: String, x: Float, y: Float) {
        val endId = _currentEndId.value
        if (endId == -1L) return
        
        viewModelScope.launch {
            val numericValue = if (score == "X") 10 else score.toIntOrNull() ?: 0
            repository.insertShot(Shot(endId = endId, score = score, numericValue = numericValue, x = x, y = y))
            
            // Update end & session score
            repository.addScoreToEnd(endId, numericValue)
            repository.modifySessionScore(_currentSessionId.value, numericValue, 1)
            
            // Auto check if end is complete (assuming 6 shots/end as per screens)
            // Use the updated list from the repository directly to be accurate
            val updatedShots = repository.getShotsForEnd(endId)
            if (updatedShots.size >= 6) {
                _showEndCompletionDialog.value = true
            }
        }
    }

    fun undoLastShot() {
        viewModelScope.launch {
            val shots = currentEndShots.value
            if (shots.isEmpty()) {
                // If it's the start of an end, allow abandonment or simple finish
                if (_currentEndNumber.value == 1) {
                    abandonSession()
                } else {
                    finishSession()
                }
                return@launch
            }
            
            val lastShot = shots.last()
            repository.deleteShot(lastShot.id)
            
            val scoreVal = if (lastShot.score == "X") 10 else lastShot.score.toIntOrNull() ?: 0
            repository.addScoreToEnd(_currentEndId.value, -scoreVal)
            repository.modifySessionScore(_currentSessionId.value, -scoreVal, -1)
            
            _showEndCompletionDialog.value = false
        }
    }

    fun moveToNextEnd() {
        val sessionId = _currentSessionId.value
        if (sessionId == -1L) return
        
        viewModelScope.launch {
            _showEndCompletionDialog.value = false
            startNewEnd(sessionId, _currentEndNumber.value + 1)
        }
    }

    fun finishSession() {
        _isSessionActive.value = false
        _currentSessionId.value = -1L
        _currentEndId.value = -1L
        _showEndCompletionDialog.value = false
    }

    fun abandonSession() {
        val sessionId = _currentSessionId.value
        if (sessionId != -1L) {
            viewModelScope.launch {
                repository.deleteSession(sessionId)
                finishSession()
            }
        }
    }

    fun dismissEndDialog() {
        _showEndCompletionDialog.value = false
    }

    fun confirmEndAndContinue() {
        if (_currentEndNumber.value >= 6) { // Final end?
            finishSession()
        } else {
            moveToNextEnd()
        }
    }

    // 5. User Operations
    suspend fun signup(username: String, passwordHash: String): Boolean {
        if (repository.getUserByUsername(username) != null) return false
        val id = repository.insertUser(User(username = username, passwordHash = passwordHash))
        loginInternal(id)
        return true
    }

    suspend fun login(username: String, passwordHash: String): Boolean {
        val user = repository.getUserByUsername(username)
        return if (user != null && user.passwordHash == passwordHash) {
            loginInternal(user.id)
            true
        } else false
    }

    private fun loginInternal(userId: Long) {
        _currentUserId.value = userId
        prefs.edit().putLong("current_user_id", userId).apply()
    }

    fun logout() {
        _currentUserId.value = -1L
        prefs.edit().putLong("current_user_id", -1L).apply()
        finishSession()
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val id = _currentUserId.value
        if (id != -1L) {
            viewModelScope.launch {
                repository.deleteUser(id)
                logout()
                onSuccess()
            }
        }
    }

    fun updatePassword(newHash: String) {
        val id = _currentUserId.value
        if (id != -1L) viewModelScope.launch { repository.updatePassword(id, newHash) }
    }

    fun updatePassword(userId: Long, newHash: String) {
        // Redundant or for admin tools
        viewModelScope.launch { repository.updatePassword(userId, newHash) }
    }

    fun updateEmail(email: String) {
        val id = _currentUserId.value
        if (id != -1L) viewModelScope.launch { repository.updateEmail(id, email) }
    }

    fun updateAvatarUri(uri: String) {
        val id = _currentUserId.value
        if (id != -1L) viewModelScope.launch { repository.updateAvatarUri(id, uri) }
    }

    // 6. DB Proxies & Session Management
    fun getEndsWithShotsForSession(sessionId: Long) = repository.getEndsWithShotsForSession(sessionId)
    fun getSessionDetails(sessionId: Long) = repository.getEndsWithShotsForSession(sessionId)
    fun getSessionByIdFlow(sessionId: Long) = repository.getSessionByIdFlow(sessionId)
    suspend fun getSessionById(sessionId: Long) = repository.getSessionById(sessionId)
    
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
