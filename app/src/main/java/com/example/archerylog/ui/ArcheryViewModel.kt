package com.example.archerylog.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.archerylog.data.*
import com.example.archerylog.domain.ArcheryRepository
import com.example.archerylog.ui.utils.AppLanguage
import com.google.ai.client.generativeai.GenerativeModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ArcheryViewModel(application: Application) : AndroidViewModel(application) {
    
    // 1. Supabase & AI Configuration
    private val supabase = io.github.jan.supabase.createSupabaseClient(
        supabaseUrl = com.example.archerylog.BuildConfig.SUPABASE_URL.trim(),
        supabaseKey = com.example.archerylog.BuildConfig.SUPABASE_ANON_KEY.trim()
    ) {
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(io.github.jan.supabase.auth.Auth)

        defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        })
    }

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = com.example.archerylog.BuildConfig.GEMINI_API_KEY
        )
    }

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun askGemini(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val prompt = "Context: Archery Coach. \nQuestion: $question"
                val response = generativeModel.generateContent(prompt)
                _aiResponse.value = response.text ?: "Empty response"
            } catch (t: Throwable) {
                _aiResponse.value = "Error: ${t.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiResponse() { _aiResponse.value = null }

    // 2. Data Repository & Auth Flow
    private val repository: ArcheryRepository = ArcheryRepository(ArcheryDatabase.getDatabase(application).archeryDao())
    private val prefs = application.getSharedPreferences("archery_prefs", Context.MODE_PRIVATE)

    // 1. User State
    private val _currentUserId = MutableStateFlow(prefs.getString("current_user_uuid", "") ?: "")
    val currentUserId = _currentUserId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        // Automatically sync on startup if logged in
        val userId = _currentUserId.value
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                // Ensure local profile exists so UI doesn't hang on "Loading..."
                try {
                    if (repository.getUserById(userId) == null) {
                        val session = supabase.auth.currentSessionOrNull()
                        val email = session?.user?.email ?: ""
                        repository.insertUser(User(id = userId, email = email, username = email.substringBefore("@")))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                syncDataFromCloud(userId)
            }
        }
    }

    val isLoggedIn: Boolean get() = _currentUserId.value.isNotBlank()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(null) else repository.getUserByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions: StateFlow<List<ArcherySession>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAllSessionsForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allEndsWithMetadata: StateFlow<List<EndWithMetadata>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAllEndsWithMetadataForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allShotsWithMetadata: StateFlow<List<ShotWithMetadata>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAllShotsWithMetadataForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val aiFavorites: StateFlow<List<AiFavorite>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAiFavoritesForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentLanguage = MutableStateFlow(
        AppLanguage.valueOf(prefs.getString("current_language", AppLanguage.CHINESE.name) ?: AppLanguage.CHINESE.name)
    )
    val currentLanguage = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("current_language", language.name).apply()
    }

    // 3. Active Session State
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

    private val _currentSessionId = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSession: StateFlow<ArcherySession?> = _currentSessionId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(null) else repository.getSessionByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentEndId = MutableStateFlow("")
    val currentEndId = _currentEndId.asStateFlow()

    private val _currentEndNumber = MutableStateFlow(1)
    val currentEndNumber = _currentEndNumber.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentEndShots: StateFlow<List<Shot>> = _currentEndId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else flow {
            emitAll(repository.getEndsWithShotsForSession(_currentSessionId.value).map { list ->
                list.find { it.end.id == id }?.shots ?: emptyList()
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionEndsWithShots: StateFlow<List<EndWithShots>> = _currentSessionId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getEndsWithShotsForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEndCompletionDialog = MutableStateFlow(false)
    val showEndCompletionDialog = _showEndCompletionDialog.asStateFlow()

    // 4. Scoring Engine (Unified Local + Supabase)
    fun startSessionManual() {
        val userId = _currentUserId.value
        if (userId.isBlank() || _isCreatingSession.value || _isSessionActive.value) return
        
        viewModelScope.launch {
            _isCreatingSession.value = true
            val session = ArcherySession(
                userId = userId,
                locationType = currentLocationType,
                distance = currentDistance,
                title = currentSessionTitle,
                venue = currentVenue,
                weather = currentWeather,
                wind = currentWind
            )
            repository.insertSession(session)
            // Async upload to Supabase
            try { supabase.postgrest.from("sessions").insert(session) } catch (e: Exception) {}

            _currentSessionId.value = session.id
            _currentEndNumber.value = 1
            startNewEnd(session.id, 1)
            _isSessionActive.value = true
            _isCreatingSession.value = false
        }
    }

    private suspend fun startNewEnd(sessionId: String, endNum: Int) {
        try {
            val end = SessionEnd(sessionId = sessionId, endNumber = endNum)
            repository.insertEnd(end)
            try { supabase.postgrest.from("ends").insert(end) } catch (e: Exception) {}
            _currentEndId.value = end.id
            _currentEndNumber.value = endNum
        } catch (e: Exception) {
            e.printStackTrace()
            // If the session was deleted during this call, finish everything safely
            finishSession()
        }
    }

    fun moveToNextEnd() {
        val sessionId = _currentSessionId.value
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            startNewEnd(sessionId, _currentEndNumber.value + 1)
        }
    }

    fun addShot(score: String, x: Float, y: Float) {
        val endId = _currentEndId.value
        if (endId.isBlank()) return
        
        viewModelScope.launch {
            try {
                // Prevent adding more than 6 shots to any single end
                if (repository.getShotsForEnd(endId).size >= 6) {
                    _showEndCompletionDialog.value = true
                    return@launch
                }
                
                val numericValue = if (score == "X") 10 else score.toIntOrNull() ?: 0
                val shot = Shot(endId = endId, score = score, numericValue = numericValue, x = x, y = y)
                repository.insertShot(shot)
                try { supabase.postgrest.from("shots").insert(shot) } catch (e: Exception) {}
                
                repository.addScoreToEnd(endId, numericValue)
                repository.modifySessionScore(_currentSessionId.value, numericValue, 1)
                
                if (repository.getShotsForEnd(endId).size >= 6) {
                    _showEndCompletionDialog.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun undoLastShot() {
        viewModelScope.launch {
            val shots = currentEndShots.value
            if (shots.isEmpty()) return@launch
            
            val lastShot = shots.last()
            repository.deleteShot(lastShot.id)
            try { supabase.postgrest.from("shots").delete { filter { eq("id", lastShot.id) } } } catch (e: Exception) {}
            
            val scoreVal = if (lastShot.score == "X") 10 else lastShot.score.toIntOrNull() ?: 0
            repository.addScoreToEnd(_currentEndId.value, -scoreVal)
            repository.modifySessionScore(_currentSessionId.value, -scoreVal, -1)
            _showEndCompletionDialog.value = false
        }
    }

    fun finishSession() {
        val sessionId = _currentSessionId.value
        if (sessionId.isBlank()) return

        val totalShots = currentSessionEndsWithShots.value.sumOf { it.shots.size }
        
        if (totalShots == 0) {
            // If empty, treat as abandonment to clean up DB
            abandonSession()
            return
        }

        resetSessionState()
    }

    private fun resetSessionState() {
        currentSessionTitle = ""
        currentVenue = ""
        _isSessionActive.value = false
        _currentSessionId.value = ""
        _currentEndId.value = ""
        _showEndCompletionDialog.value = false
    }

    fun abandonSession() {
        val sessionId = _currentSessionId.value
        if (sessionId.isNotBlank()) {
            // Immediately reset UI to avoid recursion and ensure responsiveness
            resetSessionState()
            
            viewModelScope.launch {
                try {
                    repository.deleteSession(sessionId)
                    try { supabase.postgrest.from("sessions").delete { filter { eq("id", sessionId) } } } catch (e: Exception) {}
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun confirmEndAndContinue() {
        _showEndCompletionDialog.value = false
    }

    // 5. Supabase Auth Integration
    suspend fun signup(email: String, passwordHash: String): String? {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = passwordHash
            }
            val userId = supabase.auth.currentUserOrNull()?.id ?: return "Registration failed: No user ID returned"
            // Create local profile
            repository.insertUser(User(id = userId, email = email, username = email.substringBefore("@")))
            loginInternal(userId)
            null // Success
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.localizedMessage ?: ""
            when {
                msg.contains("invalid_credentials", ignoreCase = true) -> "邮箱或密码错误"
                msg.contains("User already exists", ignoreCase = true) -> "该邮箱已被注册"
                else -> "操作失败，请检查网络后再试"
            }
        }
    }

    suspend fun login(email: String, passwordHash: String): String? {
        return try { 
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = passwordHash
            }
            val userId = supabase.auth.currentUserOrNull()?.id ?: return "登录失败：未获取到 ID"
            
            if (repository.getUserById(userId) == null) {
                repository.insertUser(User(id = userId, email = email, username = email.substringBefore("@")))
            }
            loginInternal(userId)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.localizedMessage ?: ""
            if (msg.contains("invalid_credentials", ignoreCase = true)) {
                "邮箱或密码错误"
            } else {
                "登录失败，请确认账号是否存在或密码正确"
            }
        }
    }

    private fun loginInternal(userId: String) {
        _currentUserId.value = userId
        prefs.edit().putString("current_user_uuid", userId).apply()
        viewModelScope.launch {
            syncDataFromCloud(userId)
        }
    }

    private suspend fun syncDataFromCloud(userId: String) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        android.util.Log.e("ArcherySync", "!!! SYNC STARTED !!! userId: $userId")
        
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // DEBUG PROBE: Fetch ANY session from cloud to check ID format
                try {
                    val anySession = supabase.postgrest.from("sessions").select {
                        limit(1)
                    }.decodeSingleOrNull<ArcherySession>()
                    
                    if (anySession != null) {
                        android.util.Log.e("ArcherySync", "PROBE SUCCESS! Found a session in cloud with userId: ${anySession.userId}")
                    } else {
                        android.util.Log.e("ArcherySync", "PROBE: No sessions at all in the cloud table 'sessions'")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ArcherySync", "PROBE FAILED: ${e.message}")
                }

                // 0. Sync User Profile (Optional)
                try {
                    val cloudUser = supabase.postgrest.from("users")
                        .select { filter { eq("id", userId) } }
                        .decodeSingleOrNull<User>()
                    
                    if (cloudUser != null) {
                        android.util.Log.e("ArcherySync", "User profile fetched: ${cloudUser.username}")
                        repository.insertUser(cloudUser)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ArcherySync", "Optional User profile sync skipped: ${e.message}")
                }

                // 1. Fetch and Sync Sessions
                val cloudSessions = try {
                    supabase.postgrest.from("sessions")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<ArcherySession>()
                } catch (e: Exception) {
                    android.util.Log.e("ArcherySync", "CRITICAL: Sessions fetch failed: ${e.message}")
                    emptyList<ArcherySession>()
                }
                
                android.util.Log.e("ArcherySync", "Sessions fetched: ${cloudSessions.size}")
                if (cloudSessions.isNotEmpty()) {
                    repository.insertSessions(cloudSessions)
                    val sessionIds = cloudSessions.map { it.id }

                    // 2. Fetch and Sync Ends (Chunked)
                    val allEnds = mutableListOf<SessionEnd>()
                    sessionIds.chunked(50).forEach { ids ->
                         try {
                             val chunkedEnds = supabase.postgrest.from("ends")
                                .select { filter { isIn("session_id", ids) } }
                                .decodeList<SessionEnd>()
                             allEnds.addAll(chunkedEnds)
                         } catch (e: Exception) {
                             android.util.Log.e("ArcherySync", "End chunk fetch failed: ${e.message}")
                         }
                    }
                    android.util.Log.e("ArcherySync", "Ends fetched: ${allEnds.size}")
                    
                    if (allEnds.isNotEmpty()) {
                        repository.insertEnds(allEnds)
                        val endIds = allEnds.map { it.id }

                        // 3. Fetch and Sync Shots (Chunked)
                        val allShots = mutableListOf<Shot>()
                        endIds.chunked(50).forEach { ids ->
                            try {
                                val chunkedShots = supabase.postgrest.from("shots")
                                    .select { filter { isIn("end_id", ids) } }
                                    .decodeList<Shot>()
                                allShots.addAll(chunkedShots)
                            } catch (e: Exception) {
                                android.util.Log.e("ArcherySync", "Shot chunk fetch failed: ${e.message}")
                            }
                        }
                        android.util.Log.e("ArcherySync", "Shots fetched: ${allShots.size}")
                        if (allShots.isNotEmpty()) {
                            repository.insertShots(allShots)
                        }
                    }
                }
                android.util.Log.e("ArcherySync", "!!! SYNC PROCESS COMPLETED !!!")
                
                // 4. Data Repair & Recalculation Step
                // This ensures charts can show data even if aggregated scores were missing in the cloud
                try {
                    val sessions = repository.getAllSessionsForUser(userId).first()
                    sessions.forEach { session ->
                        val ends = repository.getEndsWithShotsForSession(session.id).first()
                        var sessionScore = 0
                        var sessionShots = 0
                        
                        ends.forEach { end ->
                            val score = end.shots.sumOf { it.numericValue }
                            if (end.end.endTotalScore == 0 && score > 0) {
                                repository.addScoreToEnd(end.end.id, score)
                            }
                            sessionScore += score
                            sessionShots += end.shots.size
                        }
                        
                        if (session.totalScore == 0 && sessionScore > 0) {
                            repository.modifySessionScore(session.id, sessionScore, sessionShots)
                        }
                    }
                    android.util.Log.e("ArcherySync", "!!! DATA RECALCULATION COMPLETED !!!")
                } catch (e: Exception) {
                    android.util.Log.e("ArcherySync", "Data repair failed: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ArcherySync", "!!! UNEXPECTED SYNC ERROR !!!: ${e.message}")
                android.util.Log.e("ArcherySync", "Stacktrace: ${android.util.Log.getStackTraceString(e)}")
            } finally {
                _isSyncing.value = false
                android.util.Log.e("ArcherySync", "Sync state reset to false")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (e: Exception) {}
            _currentUserId.value = ""
            prefs.edit().putString("current_user_uuid", "").apply()
            finishSession()
        }
    }

    fun getSessionDetails(sessionId: String) = repository.getEndsWithShotsForSession(sessionId)
    fun getSessionByIdFlow(sessionId: String) = repository.getSessionByIdFlow(sessionId)
    fun deleteSession(sessionId: String) { 
        viewModelScope.launch { 
            repository.deleteSession(sessionId)
            try { supabase.postgrest.from("sessions").delete { filter { eq("id", sessionId) } } } catch (e: Exception) {}
        } 
    }
    fun dismissEndDialog() { _showEndCompletionDialog.value = false }

    // 6. User Profile & Account Management
    private val _avatarUri = MutableStateFlow(prefs.getString("user_avatar_uri", ""))
    val avatarUri = _avatarUri.asStateFlow()

    fun updateAvatarUri(newUri: String) {
        val userId = _currentUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.updateAvatarUri(userId, newUri)
            _avatarUri.value = newUri
            prefs.edit().putString("user_avatar_uri", newUri).apply()
        }
    }

    fun deleteAiFavorite(id: String) {
        viewModelScope.launch {
            repository.deleteAiFavorite(id)
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            try {
                supabase.auth.updateUser {
                    email = newEmail
                }
                repository.updateEmail(_currentUserId.value, newEmail)
            } catch (e: Exception) {}
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                supabase.auth.updateUser {
                    password = newPassword
                }
                repository.updatePassword(_currentUserId.value, newPassword)
            } catch (e: Exception) {}
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val userId = _currentUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.deleteUser(userId)
                supabase.auth.signOut()
                _currentUserId.value = ""
                prefs.edit().putString("current_user_uuid", "").apply()
                finishSession()
                onSuccess()
            } catch (e: Exception) {}
        }
    }

    fun saveAiFavorite(question: String, answer: String) {
        val userId = _currentUserId.value
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                repository.insertAiFavorite(AiFavorite(userId = userId, question = question, answer = answer))
            }
        }
    }
}
