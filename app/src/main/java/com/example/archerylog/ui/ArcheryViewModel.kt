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
import com.example.archerylog.utils.AvatarCacheManager
import com.example.archerylog.ui.utils.L10n
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.parseSessionFromFragment
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class ArcheryViewModel(application: Application) : AndroidViewModel(application) {
    
    // 1. Supabase & AI Configuration
    private val supabase = io.github.jan.supabase.createSupabaseClient(
        supabaseUrl = com.example.archerylog.BuildConfig.SUPABASE_URL.trim(),
        supabaseKey = com.example.archerylog.BuildConfig.SUPABASE_ANON_KEY.trim()
    ) {
        install(io.github.jan.supabase.postgrest.Postgrest)
        install(io.github.jan.supabase.auth.Auth)
        install(io.github.jan.supabase.storage.Storage)

        defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        })
    }
    
    private val geminiApiKey = com.example.archerylog.BuildConfig.GEMINI_API_KEY

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
                val result = withContext(Dispatchers.IO) {
                    callGeminiApi(prompt)
                }
                _aiResponse.value = result
            } catch (t: Throwable) {
                _aiResponse.value = "Error: ${t.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiApiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        connection.outputStream.use { os ->
            os.write(requestBody.toString().toByteArray())
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API Error ($responseCode): $errorStream")
        }

        val responseText = connection.inputStream.bufferedReader().readText()
        val jsonResponse = JSONObject(responseText)
        return jsonResponse
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    private val scoreMutex = Mutex()

    fun clearAiResponse() { _aiResponse.value = null }

    private val repository: ArcheryRepository = ArcheryRepository(ArcheryDatabase.getDatabase(application).archeryDao())
    private val prefs = application.getSharedPreferences("archery_prefs", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getString("current_user_uuid", "") ?: "")
    val currentUserId = _currentUserId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _showSyncMask = MutableStateFlow(false)
    val showSyncMask = _showSyncMask.asStateFlow()

    private val _oauthError = MutableStateFlow<String?>(null)
    val oauthError = _oauthError.asStateFlow()

    fun clearOauthError() {
        _oauthError.value = null
    }

    private val _debugMessage = MutableStateFlow<String?>(null)
    val debugMessage = _debugMessage.asStateFlow()

    fun clearDebugMessage() {
        _debugMessage.value = null
    }

    init {
        val userId = _currentUserId.value
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    syncDataFromCloud(userId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val isLoggedIn: Boolean get() = _currentUserId.value.isNotBlank()

    val currentUser: StateFlow<User?> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(null) else repository.getUserByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSessions: StateFlow<List<ArcherySession>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAllSessionsForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEndsWithMetadata: StateFlow<List<EndWithMetadata>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAllEndsWithMetadataForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShotsWithMetadata: StateFlow<List<ShotWithMetadata>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAllShotsWithMetadataForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiFavorites: StateFlow<List<AiFavorite>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getAiFavoritesForUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentLanguage = MutableStateFlow(
        run {
            val savedLanguage = prefs.getString("current_language", null)
            if (savedLanguage != null) {
                try {
                    AppLanguage.valueOf(savedLanguage)
                } catch (e: Exception) {
                    AppLanguage.ENGLISH
                }
            } else {
                val systemLanguage = java.util.Locale.getDefault().language
                when {
                    systemLanguage.startsWith("zh") -> AppLanguage.CHINESE
                    systemLanguage.startsWith("ja") -> AppLanguage.JAPANESE
                    else -> AppLanguage.ENGLISH
                }
            }
        }
    )
    val currentLanguage = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("current_language", language.name).apply()
    }

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

    val currentSession: StateFlow<ArcherySession?> = _currentSessionId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(null) else repository.getSessionByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentEndId = MutableStateFlow("")
    val currentEndId = _currentEndId.asStateFlow()

    private val _currentEndNumber = MutableStateFlow(1)
    val currentEndNumber = _currentEndNumber.asStateFlow()

    val currentEndShots: StateFlow<List<Shot>> = _currentEndId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else flow {
            emitAll(repository.getEndsWithShotsForSession(_currentSessionId.value).map { list ->
                list.find { it.end.id == id }?.shots ?: emptyList()
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSessionEndsWithShots: StateFlow<List<EndWithShots>> = _currentSessionId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else repository.getEndsWithShotsForSession(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showEndCompletionDialog = MutableStateFlow(false)
    val showEndCompletionDialog = _showEndCompletionDialog.asStateFlow()

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
            _showEndCompletionDialog.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            finishSession()
        }
    }

    fun moveToNextEnd() {
        val sessionId = _currentSessionId.value
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            scoreMutex.withLock {
                startNewEnd(sessionId, _currentEndNumber.value + 1)
            }
        }
    }

    fun addShot(score: String, x: Float, y: Float) {
        val endId = _currentEndId.value
        if (endId.isBlank()) return
        
        viewModelScope.launch {
            scoreMutex.withLock {
                try {
                    val currentShots = repository.getShotsForEnd(endId)
                    if (currentShots.size >= 6) {
                        _showEndCompletionDialog.value = true
                        return@withLock
                    }
                    
                    val numericValue = if (score == "X") 10 else score.toIntOrNull() ?: 0
                    val shot = Shot(endId = endId, score = score, numericValue = numericValue, x = x, y = y)
                    repository.insertShot(shot)
                    
                    viewModelScope.launch {
                        try { supabase.postgrest.from("shots").insert(shot) } catch (e: Exception) {}
                    }
                    
                    repository.addScoreToEnd(endId, numericValue)
                    repository.modifySessionScore(_currentSessionId.value, numericValue, 1)
                    
                    if (currentShots.size + 1 >= 6) {
                        _showEndCompletionDialog.value = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun undoLastShot() {
        viewModelScope.launch {
            scoreMutex.withLock {
                val shots = currentEndShots.value
                if (shots.isEmpty()) return@withLock
                
                val lastShot = shots.last()
                repository.deleteShot(lastShot.id)
                viewModelScope.launch {
                    try { supabase.postgrest.from("shots").delete { filter { eq("id", lastShot.id) } } } catch (e: Exception) {}
                }
                
                val scoreVal = if (lastShot.score == "X") 10 else lastShot.score.toIntOrNull() ?: 0
                repository.addScoreToEnd(_currentEndId.value, -scoreVal)
                repository.modifySessionScore(_currentSessionId.value, -scoreVal, -1)
                _showEndCompletionDialog.value = false
            }
        }
    }

    fun finishSession() {
        val sessionId = _currentSessionId.value
        if (sessionId.isBlank()) return

        val totalShots = currentSessionEndsWithShots.value.sumOf { it.shots.size }
        
        if (totalShots == 0) {
            abandonSession()
            return
        }

        viewModelScope.launch {
            try {
                repository.deleteEmptyEndsForSession(sessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            resetSessionState()
        }
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

    suspend fun signup(email: String, passwordHash: String): String? {
        val l10n = L10n(currentLanguage.value)
        return try {
            val params = kotlinx.serialization.json.buildJsonObject {
                put("check_email", kotlinx.serialization.json.JsonPrimitive(email))
            }
            val isEmailRegistered = try {
                val rpcResult = supabase.postgrest.rpc("check_email_exists", params).data
                rpcResult.trim() == "true"
            } catch (e: Exception) {
                false
            }
            if (isEmailRegistered) {
                return l10n.emailAlreadyRegistered
            }

            try { supabase.auth.signOut() } catch (_: Exception) {}
            
            val langCode = when (currentLanguage.value) {
                AppLanguage.ENGLISH -> "en"
                AppLanguage.JAPANESE -> "ja"
                AppLanguage.CHINESE -> "zh"
            }
            supabase.auth.signUpWith(Email, redirectUrl = "https://auth.blueskylabs.app/?lang=$langCode") {
                this.email = email
                this.password = passwordHash
            }
            val user = supabase.auth.currentUserOrNull()
            
            if (user == null) {
                return l10n.verificationSentHint
            }
            
            val userId = user.id
            
            if (user.confirmedAt == null || user.emailConfirmedAt == null) {
                try {
                    val newUser = User(id = userId, email = email, username = email.substringBefore("@"))
                    supabase.postgrest.from("users").upsert(newUser)
                    saveOrUpdateUserLocally(newUser)
                } catch (e: Exception) {}
                try { supabase.auth.signOut() } catch (_: Exception) {}
                return l10n.verificationSentHint
            }

            val newUser = User(id = userId, email = email, username = email.substringBefore("@"))
            supabase.postgrest.from("users").upsert(newUser)
            saveOrUpdateUserLocally(newUser)
            
            loginInternal(userId)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.localizedMessage ?: ""
            when {
                msg.contains("rate limit exceeded", ignoreCase = true) || msg.contains("Too many requests", ignoreCase = true) -> l10n.rateLimitExceeded
                msg.contains("invalid_credentials", ignoreCase = true) -> l10n.invalidCredentials
                msg.contains("User already exists", ignoreCase = true) -> l10n.emailAlreadyRegistered
                else -> l10n.operationFailed
            }
        }
    }

    suspend fun login(identifier: String, passwordHash: String): String? {
        val l10n = L10n(currentLanguage.value)
        return try { 
            var finalEmail = identifier
            
            // Clear any stale session state before attempting sign-in.
            // After logout, the Supabase Auth SDK may retain internal state
            // that interferes with a fresh signInWith() call.
            try {
                supabase.auth.signOut()
            } catch (_: Exception) {
                // Ignore — we just want to ensure a clean auth state
            }
            
            if (!identifier.contains("@")) {
                // Try to look up the email by username from the users table.
                // This query may fail if RLS blocks anonymous access (e.g. after logout),
                // so we handle the failure gracefully instead of returning an error immediately.
                val cloudUser = try {
                    supabase.postgrest.from("users")
                        .select { 
                            filter { 
                                or {
                                    eq("username", identifier.lowercase())
                                    eq("username", identifier) 
                                }
                            }
                        }
                        .decodeSingleOrNull<User>()
                } catch (e: Exception) {
                    android.util.Log.w("ArcheryLogin", "Username lookup failed (likely RLS after signOut): ${e.message}")
                    null // Don't return error — fall through to try local DB lookup
                }
                
                if (cloudUser != null && !cloudUser.email.isNullOrEmpty()) {
                    finalEmail = cloudUser.email!!
                } else {
                    // Cloud lookup failed or returned no result — try local database as fallback
                    val localUser = try {
                        repository.getUserByUsername(identifier) ?: repository.getUserByUsername(identifier.lowercase())
                    } catch (e: Exception) { null }
                    
                    if (localUser != null && !localUser.email.isNullOrEmpty()) {
                        finalEmail = localUser.email!!
                    } else {
                        return l10n.loginUserNotFound
                    }
                }
            }

            android.util.Log.d("ArcheryLogin", "Attempting signInWith(Email) for: $finalEmail")
            supabase.auth.signInWith(Email) {
                this.email = finalEmail
                this.password = passwordHash
            }
            android.util.Log.d("ArcheryLogin", "signInWith succeeded, retrieving user...")
            val userId = supabase.auth.currentUserOrNull()?.id ?: return l10n.loginNoIdError
            android.util.Log.d("ArcheryLogin", "User ID: $userId")
            
            val cloudProfile = try {
                supabase.postgrest.from("users")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<User>()
            } catch (e: Exception) { null }
            
            val finalUser = cloudProfile ?: User(
                id = userId, 
                email = finalEmail, 
                username = finalEmail.substringBefore("@")
            )
            saveOrUpdateUserLocally(finalUser)
            
            if (cloudProfile == null) {
                try {
                    supabase.postgrest.from("users").upsert(finalUser)
                } catch (e: Exception) {}
            }
            
            loginInternal(userId)
            null
        } catch (e: Exception) {
            android.util.Log.e("ArcheryLogin", "Login FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            val msg = (e.localizedMessage ?: "") + " " + (e.message ?: "")
            when {
                msg.contains("Email not confirmed", ignoreCase = true) || 
                msg.contains("Email not verified", ignoreCase = true) -> l10n.emailNotVerifiedError
                msg.contains("invalid_credentials", ignoreCase = true) ||
                msg.contains("Invalid login credentials", ignoreCase = true) ||
                msg.contains("invalid_grant", ignoreCase = true) -> l10n.invalidCredentials
                else -> l10n.loginFailed
            }
        }
    }

    suspend fun loginAsGuest() {
        val existing = repository.getUserById("guest")
        if (existing == null) {
            val guestUser = User(
                id = "guest",
                username = "Guest",
                email = "guest@local.archerylog",
                passwordHash = ""
            )
            repository.insertUser(guestUser)
        }
        loginInternal("guest")
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            try {
                supabase.auth.signInWith(
                    provider = Google,
                    redirectUrl = "archerylog://callback"
                )
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn", "Failed to start Google sign-in: ${e.message}", e)
            }
        }
    }

    fun handleDeepLink(url: String) {
        _debugMessage.value = "收到回调 URL: $url"
        viewModelScope.launch {
            try {
                android.util.Log.d("OAuthCallback", "Received callback URL: $url")
                if (url.startsWith("archerylog://")) {
                    val session = try {
                        if (url.contains("#")) {
                            val fragment = url.substringAfter("#")
                            supabase.auth.parseSessionFromFragment(fragment)
                        } else {
                            supabase.auth.parseSessionFromUrl(url)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OAuthCallback", "Parse failed, falling back to parseSessionFromUrl: ${e.message}")
                        _debugMessage.value = "解析会话错误，尝试后备方案: ${e.message}"
                        supabase.auth.parseSessionFromUrl(url)
                    }
                    
                    _debugMessage.value = "会话解析成功，正在导入 SDK..."
                    supabase.auth.importSession(session)
                    
                    val user = session.user
                    if (user != null) {
                        val userId = user.id
                        val email = user.email ?: ""
                        _debugMessage.value = "用户已就绪: $email ($userId). 正在查询云端 profile..."
                        
                        val cloudProfile = try {
                            supabase.postgrest.from("users")
                                .select { filter { eq("id", userId) } }
                                .decodeSingleOrNull<User>()
                        } catch (e: Exception) { 
                            _debugMessage.value = "查询云端 profile 报错: ${e.message}"
                            null 
                        }
                        
                        val finalUser = cloudProfile ?: User(
                            id = userId, 
                            email = email, 
                            username = email.substringBefore("@")
                        )
                        _debugMessage.value = "正在写入本地数据库..."
                        saveOrUpdateUserLocally(finalUser)
                        
                        if (cloudProfile == null) {
                            try {
                                _debugMessage.value = "正在同步个人信息到云端..."
                                supabase.postgrest.from("users").upsert(finalUser)
                            } catch (e: Exception) {
                                _debugMessage.value = "同步到云端 users 表报错: ${e.message}"
                            }
                        }
                        
                        _debugMessage.value = "正在调用 loginInternal 并同步记录..."
                        loginInternal(userId)
                    } else {
                        _debugMessage.value = "导入成功，但获取 currentUserOrNull 为空！"
                    }
                } else {
                    _debugMessage.value = "忽略非本 App Scheme 的 URL: $url"
                }
            } catch (e: Exception) {
                android.util.Log.e("OAuthCallback", "Failed to parse session from URL: ${e.message}", e)
                _oauthError.value = e.localizedMessage ?: e.message ?: "OAuth Login Failed"
                _debugMessage.value = "handleDeepLink 崩溃: ${e.message}"
            }
        }
    }

    suspend fun resendVerificationEmail(email: String): String? {
        val l10n = L10n(currentLanguage.value)
        return try {
            supabase.auth.resendEmail(type = OtpType.Email.SIGNUP, email = email)
            l10n.resendSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to send: ${e.localizedMessage}"
        }
    }

    suspend fun resetPassword(email: String): String? {
        val l10n = L10n(currentLanguage.value)
        return try {
            val langCode = when (currentLanguage.value) {
                AppLanguage.ENGLISH -> "en"
                AppLanguage.JAPANESE -> "ja"
                AppLanguage.CHINESE -> "zh"
            }
            supabase.auth.resetPasswordForEmail(
                email = email,
                redirectUrl = "https://auth.blueskylabs.app/reset-password.html?lang=$langCode"
            )
            null // success — caller will show the success message
        } catch (e: Exception) {
            android.util.Log.e("ArcheryLogin", "Reset password failed: ${e.message}", e)
            l10n.forgotPasswordFailed
        }
    }

    private fun loginInternal(userId: String) {
        _currentUserId.value = userId
        prefs.edit().putString("current_user_uuid", userId).apply()
        viewModelScope.launch {
            if (userId != "guest") {
                _showSyncMask.value = true
            }
            try {
                syncDataFromCloud(userId)
            } finally {
                _showSyncMask.value = false
            }
        }
    }

    private suspend fun syncDataFromCloud(userId: String) {
        if (userId == "guest") return
        if (_isSyncing.value) return
        _isSyncing.value = true
        
        withContext(Dispatchers.IO) {
            try {
                try {
                    val cloudUser = try {
                        supabase.postgrest.from("users")
                            .select { filter { eq("id", userId) } }
                            .decodeSingleOrNull<User>()
                    } catch (e: Exception) { null }
                    
                    if (cloudUser != null) {
                        saveOrUpdateUserLocally(cloudUser)
                        cloudUser.avatarUri?.let { uri ->
                            viewModelScope.launch {
                                AvatarCacheManager.downloadToCache(getApplication(), uri)
                            }
                        }
                    } else {
                        val session = try { supabase.auth.retrieveUserForCurrentSession(updateSession = true) } catch (e: Exception) { null }
                        val email = session?.email
                        if (email != null) {
                            val local = repository.getUserById(userId)
                            if (local == null || local.username.startsWith("archer_")) {
                                val repairedUser = User(id = userId, email = email, username = email.substringBefore("@"))
                                saveOrUpdateUserLocally(repairedUser)
                                // IMPORTANT: Push this repaired profile back to cloud so username login works next time
                                try {
                                    supabase.postgrest.from("users").upsert(repairedUser)
                                } catch (e: Exception) {
                                    android.util.Log.e("ArcherySync", "Failed to sync repaired profile back to cloud: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ArcherySync", "User profile sync error: ${e.message}")
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

    suspend fun logout() {
        val userId = _currentUserId.value
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            android.util.Log.w("ArcheryLogout", "signOut error (ignored): ${e.message}")
        }
        if (userId.isNotBlank() && userId != "guest") {
            try {
                repository.deleteUser(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _currentUserId.value = ""
        prefs.edit().putString("current_user_uuid", "").apply()
        finishSession()
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
        
        // 1. 立即更新本地状态，让 UI 实时反馈 (秒开预览)
        _avatarUri.value = newUri
        viewModelScope.launch {
            repository.updateAvatarUri(userId, newUri)
            prefs.edit().putString("user_avatar_uri", newUri).apply()
            
            try {
                // 2. 如果是本地文件，尝试异步上传云端
                if (newUri.startsWith("file://")) {
                    val filePath = newUri.removePrefix("file://")
                    val file = File(filePath)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val bucket = supabase.storage["avatars"]
                        val fileName = "$userId.jpg"
                        
                        // 上传并覆盖旧头像
                        bucket.upload(fileName, bytes) {
                            upsert = true
                        }
                        
                        // 获取公开 URL
                        val publicUrl = bucket.publicUrl(fileName)
                        
                        // 3. 上传成功后，将云端 URL 同步到云端 users 表
                        supabase.postgrest.from("users").update({
                            set("avatar_uri", publicUrl)
                        }) {
                            filter { eq("id", userId) }
                        }
                        
                        // 同时更新本地记录为云端 URL
                        repository.updateAvatarUri(userId, publicUrl)
                        _avatarUri.value = publicUrl
                        prefs.edit().putString("user_avatar_uri", publicUrl).apply()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ArcheryLog", "Cloud avatar sync failed!", e)
                // 这里不需要 fallback，因为本地已经显示着 file:// 路径了
            }
        }
    }

    suspend fun updateUsername(newUsername: String): String? {
        val userId = _currentUserId.value
        if (userId.isBlank()) return "Update failed: Not logged in"
        if (newUsername.isBlank()) return "Username cannot be empty"
        
        return try {
            // 1. Cloud side uniqueness check
            val existing = supabase.postgrest.from("users")
                .select { filter { eq("username", newUsername) } }
                .decodeSingleOrNull<User>()
            
            if (existing != null && existing.id != userId) {
                return "This username is already taken"
            }
            
            // 2. Fetch current user to preserve email/avatar
            val currentUserData = repository.getUserById(userId) ?: return "User profile not found"
            val updatedUser = currentUserData.copy(username = newUsername)
            
            // 3. Push to Cloud
            supabase.postgrest.from("users").upsert(updatedUser)
            
            // 4. Update locally
            repository.updateUsername(userId, newUsername)
            
            null // Success
        } catch (e: Exception) {
            e.printStackTrace()
            "Update failed: ${e.localizedMessage}"
        }
    }

    fun deleteAiFavorite(id: String) {
        viewModelScope.launch {
            repository.deleteAiFavorite(id)
        }
    }

    fun updateEmail(newEmail: String) {
        val userId = _currentUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                // 1. Update Auth
                supabase.auth.updateUser {
                    email = newEmail
                }
                // 2. Update Local
                repository.updateEmail(userId, newEmail)
                // 3. Update Cloud Profile
                val user = repository.getUserById(userId)
                if (user != null) {
                    supabase.postgrest.from("users").upsert(user.copy(email = newEmail))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updatePassword(oldPassword: String, newPassword: String): String? {
        val l10n = L10n(currentLanguage.value)
        val email = currentUser.value?.email ?: return "Email not found"
        
        val tempSupabase = io.github.jan.supabase.createSupabaseClient(
            supabaseUrl = com.example.archerylog.BuildConfig.SUPABASE_URL.trim(),
            supabaseKey = com.example.archerylog.BuildConfig.SUPABASE_ANON_KEY.trim()
        ) {
            install(io.github.jan.supabase.auth.Auth)
        }
        
        return try {
            tempSupabase.auth.signInWith(Email) {
                this.email = email
                this.password = oldPassword
            }
            try { tempSupabase.auth.signOut() } catch (_: Exception) {}
            
            supabase.auth.updateUser {
                password = newPassword
            }
            repository.updatePassword(_currentUserId.value, "")
            null
        } catch (e: Exception) {
            e.printStackTrace()
            l10n.oldPasswordIncorrect
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val userId = _currentUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            try {
                android.util.Log.d("DeleteAccount", "Starting account deletion for userId: $userId")
                // 1. Call server-side function to delete user from Supabase Auth & users table
                supabase.postgrest.rpc("delete_own_account")
                android.util.Log.d("DeleteAccount", "RPC delete_own_account succeeded!")
                // 2. Clear the local cached session
                try { supabase.auth.signOut() } catch (_: Exception) {}
                // 3. Clean up local data
                repository.deleteUser(userId)
                _currentUserId.value = ""
                prefs.edit().putString("current_user_uuid", "").apply()
                finishSession()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("DeleteAccount", "RPC FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                // If RPC fails, still try to sign out and clean up locally
                try {
                    supabase.auth.signOut()
                    repository.deleteUser(userId)
                    _currentUserId.value = ""
                    prefs.edit().putString("current_user_uuid", "").apply()
                    finishSession()
                    onSuccess()
                } catch (e2: Exception) {
                    android.util.Log.e("DeleteAccount", "Fallback also failed: ${e2.message}")
                }
            }
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

    private suspend fun saveOrUpdateUserLocally(user: User) {
        val existing = repository.getUserById(user.id)
        if (existing == null) {
            repository.insertUser(user)
        } else {
            repository.updateUsername(user.id, user.username)
            user.email?.let { repository.updateEmail(user.id, it) }
            user.avatarUri?.let { repository.updateAvatarUri(user.id, it) }
            repository.updatePassword(user.id, user.passwordHash)
        }
    }
}
