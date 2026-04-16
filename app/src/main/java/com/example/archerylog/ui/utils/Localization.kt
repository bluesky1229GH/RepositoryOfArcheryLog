package com.example.archerylog.ui.utils

enum class AppLanguage(val label: String) {
    ENGLISH("English"),
    JAPANESE("日本語"),
    CHINESE("中文")
}

class L10n(val language: AppLanguage) {
    // Navigation
    val navHome = when(language) {
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.JAPANESE -> "ホーム"
        AppLanguage.CHINESE -> "首页"
    }
    val navRecords = when(language) {
        AppLanguage.ENGLISH -> "Records"
        AppLanguage.JAPANESE -> "履歴"
        AppLanguage.CHINESE -> "记录"
    }
    val navAdd = when(language) {
        AppLanguage.ENGLISH -> "New"
        AppLanguage.JAPANESE -> "新規"
        AppLanguage.CHINESE -> "新建"
    }
    val navAccount = when(language) {
        AppLanguage.ENGLISH -> "Account"
        AppLanguage.JAPANESE -> "アカウント"
        AppLanguage.CHINESE -> "账户"
    }

    // Dashboard / Statistics
    val dashboardTitle = when(language) {
        AppLanguage.ENGLISH -> "Data Analysis"
        AppLanguage.JAPANESE -> "データ分析"
        AppLanguage.CHINESE -> "数据分析"
    }
    val timeRange = when(language) {
        AppLanguage.ENGLISH -> "Time Range"
        AppLanguage.JAPANESE -> "期間"
        AppLanguage.CHINESE -> "时间范围"
    }
    val locationFilter = when(language) {
        AppLanguage.ENGLISH -> "Location Filter"
        AppLanguage.JAPANESE -> "場所"
        AppLanguage.CHINESE -> "场地筛选"
    }
    val distanceFilter = when(language) {
        AppLanguage.ENGLISH -> "Distance Filter"
        AppLanguage.JAPANESE -> "距離"
        AppLanguage.CHINESE -> "距离筛选"
    }
    val allTime = when(language) {
        AppLanguage.ENGLISH -> "All Time"
        AppLanguage.JAPANESE -> "全期間"
        AppLanguage.CHINESE -> "全部时间"
    }
    val last24h = when(language) {
        AppLanguage.ENGLISH -> "Last 24h"
        AppLanguage.JAPANESE -> "過去24時間"
        AppLanguage.CHINESE -> "过去24小时"
    }
    val lastWeek = when(language) {
        AppLanguage.ENGLISH -> "Last Week"
        AppLanguage.JAPANESE -> "先週"
        AppLanguage.CHINESE -> "过去一周"
    }
    val lastMonth = when(language) {
        AppLanguage.ENGLISH -> "Last Month"
        AppLanguage.JAPANESE -> "先月"
        AppLanguage.CHINESE -> "过去一月"
    }
    val lastYear = when(language) {
        AppLanguage.ENGLISH -> "Last Year"
        AppLanguage.JAPANESE -> "昨年"
        AppLanguage.CHINESE -> "过去一年"
    }
    val all = when(language) {
        AppLanguage.ENGLISH -> "All"
        AppLanguage.JAPANESE -> "すべて"
        AppLanguage.CHINESE -> "全部"
    }
    val indoor = when(language) {
        AppLanguage.ENGLISH -> "Indoor"
        AppLanguage.JAPANESE -> "屋内"
        AppLanguage.CHINESE -> "室内"
    }
    val outdoor = when(language) {
        AppLanguage.ENGLISH -> "Outdoor"
        AppLanguage.JAPANESE -> "屋外"
        AppLanguage.CHINESE -> "室外"
    }
    val noDataMatch = when(language) {
        AppLanguage.ENGLISH -> "No data available for these filters"
        AppLanguage.JAPANESE -> "条件に一致するデータがありません"
        AppLanguage.CHINESE -> "没有符合条件的数据"
    }
    val trendTitle = when(language) {
        AppLanguage.ENGLISH -> "Avg Score per Arrow"
        AppLanguage.JAPANESE -> "平均スコア推移（1射）"
        AppLanguage.CHINESE -> "单箭均分趋势"
    }
    val zoom = when(language) {
        AppLanguage.ENGLISH -> "Zoom"
        AppLanguage.JAPANESE -> "拡大"
        AppLanguage.CHINESE -> "放大"
    }
    val instructionsTitle = when(language) {
        AppLanguage.ENGLISH -> "Usage Instructions"
        AppLanguage.JAPANESE -> "使い方の説明"
        AppLanguage.CHINESE -> "使用说明"
    }
    val usageGuide = when(language) {
        AppLanguage.ENGLISH -> "• Top Area: Tap Title/Venue info. Select Distance, Weather, and Wind via dropdowns.\n• Bottom Area: Tap the target to enter scoring mode."
        AppLanguage.JAPANESE -> "• 上部：タイトルと会場を入力。距離・天気・風をドロップダウンで選択します。\n• 下部：的をタップして記録モードに切り替えます。"
        AppLanguage.CHINESE -> "• 屏幕上半部：点击标题和场地栏录入详情；通过下拉菜单选择距离、天气及风力。\n• 屏幕下半部：点击靶纸展开记录页面。"
    }
    val setupGuide = when(language) {
        AppLanguage.ENGLISH -> "1. Scoring: Tap positions directly on the target.\n2. Navigation: Top-left shows current End; top-right shows Total Score and Close."
        AppLanguage.JAPANESE -> "1. 記録：的の着弾地点を直接タップして記録します。\n2. 画面：左上に現在のエンド数、右上に合計得点と閉じるボタンを表示。"
        AppLanguage.CHINESE -> "1. 记录方式：在记录页面直接点击靶纸上的位置即刻录入分数。\n2. 界面显示：左上方显示当前组数，右上方显示总分及关闭按钮。"
    }
    val notes = when(language) {
        AppLanguage.ENGLISH -> "• Buttons: Bottom-left [Undo]; Middle [Next] (after 6 shots); Bottom-right [Finish] to save.\n• Arrow scores are displayed at the very bottom."
        AppLanguage.JAPANESE -> "• 操作：左下[取り消し]、中央[次へ]（6射後）、右下[記録完了]で保存。\n• 画面最下部に各矢の環数が表示されます。"
        AppLanguage.CHINESE -> "• 功能按钮：左下角[撤销]按钮；中间[下一组]按钮（满6箭时）；右下角[记录完成]显示后即可点击保存。\n• 页面最下方将动态显示当前组的所有环数。"
    }
    val close = when(language) {
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.JAPANESE -> "閉じる"
        AppLanguage.CHINESE -> "关闭"
    }
    val avgScoreLabel = when(language) {
        AppLanguage.ENGLISH -> "Avg Score (0-10)"
        AppLanguage.JAPANESE -> "平均スコア (0-10)"
        AppLanguage.CHINESE -> "平均分 (0-10)"
    }
    val sessionsChrono = when(language) {
        AppLanguage.ENGLISH -> "Sessions (Chronological)"
        AppLanguage.JAPANESE -> "セッション（時系列）"
        AppLanguage.CHINESE -> "训练场次（按时间）"
    }

    // New Analysis Strings
    val ringDistribution = when(language) {
        AppLanguage.ENGLISH -> "Ring Distribution"
        AppLanguage.JAPANESE -> "グルーピング"
        AppLanguage.CHINESE -> "环值稳定度分布"
    }
    val goldZone = when(language) {
        AppLanguage.ENGLISH -> "Gold (9-10)"
        AppLanguage.JAPANESE -> "イエロー (9-10)"
        AppLanguage.CHINESE -> "黄区 (9-10环)"
    }
    val redZone = when(language) {
        AppLanguage.ENGLISH -> "Red (7-8)"
        AppLanguage.JAPANESE -> "レッド (7-8)"
        AppLanguage.CHINESE -> "红区 (7-8环)"
    }
    val blueZone = when(language) {
        AppLanguage.ENGLISH -> "Blue (5-6)"
        AppLanguage.JAPANESE -> "ブルー (5-6)"
        AppLanguage.CHINESE -> "蓝区 (5-6环)"
    }
    val blackZone = when(language) {
        AppLanguage.ENGLISH -> "Black (3-4)"
        AppLanguage.JAPANESE -> "ブラック (3-4)"
        AppLanguage.CHINESE -> "黑区 (3-4环)"
    }
    val whiteZone = when(language) {
        AppLanguage.ENGLISH -> "White (0-2)"
        AppLanguage.JAPANESE -> "ホワイト/脱靶 (0-2)"
        AppLanguage.CHINESE -> "白区/脱靶 (0-2环)"
    }

    // Records
    val recordsTitle = when(language) {
        AppLanguage.ENGLISH -> "Data"
        AppLanguage.JAPANESE -> "データ"
        AppLanguage.CHINESE -> "数据"
    }
    val deleteSessionTitle = when(language) {
        AppLanguage.ENGLISH -> "Delete Session"
        AppLanguage.JAPANESE -> "セッションを削除"
        AppLanguage.CHINESE -> "删除训练"
    }
    val deleteSessionConfirm = when(language) {
        AppLanguage.ENGLISH -> "Are you sure you want to delete this log? This action cannot be undone."
        AppLanguage.JAPANESE -> "このログを削除してもよろしいですか？この操作は取り消せません。"
        AppLanguage.CHINESE -> "确定要删除这条日志吗？此操作无法撤销。"
    }
    val delete = when(language) {
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.JAPANESE -> "削除"
        AppLanguage.CHINESE -> "删除"
    }
    
    // AI Consultant
    val aiConsultant = when(language) {
        AppLanguage.ENGLISH -> "AI Coach"
        AppLanguage.JAPANESE -> "AI コーチ"
        AppLanguage.CHINESE -> "AI 教练"
    }
    val askAiPlaceholder = when(language) {
        AppLanguage.ENGLISH -> "Ask about archery tech, posture..."
        AppLanguage.JAPANESE -> "技術や姿勢について質問..."
        AppLanguage.CHINESE -> "提问专业技术、姿势要领..."
    }
    val askButton = when(language) {
        AppLanguage.ENGLISH -> "Ask"
        AppLanguage.JAPANESE -> "質問する"
        AppLanguage.CHINESE -> "提问"
    }
    val cancel = when(language) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.JAPANESE -> "キャンセル"
        AppLanguage.CHINESE -> "取消"
    }
    val date = when(language) {
        AppLanguage.ENGLISH -> "Date"
        AppLanguage.JAPANESE -> "日付"
        AppLanguage.CHINESE -> "日期"
    }
    val startDate = when(language) {
        AppLanguage.ENGLISH -> "Start Date"
        AppLanguage.JAPANESE -> "開始日"
        AppLanguage.CHINESE -> "开始日期"
    }
    val endDate = when(language) {
        AppLanguage.ENGLISH -> "End Date"
        AppLanguage.JAPANESE -> "終了日"
        AppLanguage.CHINESE -> "结束日期"
    }
    val score = when(language) {
        AppLanguage.ENGLISH -> "Score"
        AppLanguage.JAPANESE -> "スコア"
        AppLanguage.CHINESE -> "得分"
    }
    val newSession = when(language) {
        AppLanguage.ENGLISH -> "New Session"
        AppLanguage.JAPANESE -> "新しいセッション"
        AppLanguage.CHINESE -> "新训练"
    }
    val addRecordTitle = when(language) {
        AppLanguage.ENGLISH -> "Add Record"
        AppLanguage.JAPANESE -> "記録を追加"
        AppLanguage.CHINESE -> "新增记录"
    }
    val distance = when(language) {
        AppLanguage.ENGLISH -> "Distance (m)"
        AppLanguage.JAPANESE -> "距離 (m)"
        AppLanguage.CHINESE -> "距离 (m)"
    }
    val start = when(language) {
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.JAPANESE -> "次へ"
        AppLanguage.CHINESE -> "下一组"
    }
    val tapTargetToStart = when(language) {
        AppLanguage.ENGLISH -> "Tap the target to start recording"
        AppLanguage.JAPANESE -> "的をタップして记录を開始してください"
        AppLanguage.CHINESE -> "点击靶纸以开始记录练习"
    }

    // Account
    val accountTitle = when(language) {
        AppLanguage.ENGLISH -> "Account"
        AppLanguage.JAPANESE -> "アカウント"
        AppLanguage.CHINESE -> "账户"
    }
    val takePhoto = when(language) {
        AppLanguage.ENGLISH -> "Take Photo"
        AppLanguage.JAPANESE -> "写真を撮る"
        AppLanguage.CHINESE -> "拍照"
    }
    val chooseGallery = when(language) {
        AppLanguage.ENGLISH -> "Choose Gallery"
        AppLanguage.JAPANESE -> "ギャラリーから選択"
        AppLanguage.CHINESE -> "从相册选择"
    }
    val username = when(language) {
        AppLanguage.ENGLISH -> "Username"
        AppLanguage.JAPANESE -> "ユーザー名"
        AppLanguage.CHINESE -> "用户名"
    }
    val email = when(language) {
        AppLanguage.ENGLISH -> "Email"
        AppLanguage.JAPANESE -> "メール"
        AppLanguage.CHINESE -> "邮箱"
    }
    val sessionTitle = when(language) {
        AppLanguage.ENGLISH -> "Title"
        AppLanguage.JAPANESE -> "タイトル"
        AppLanguage.CHINESE -> "标题"
    }
    val venue = when(language) {
        AppLanguage.ENGLISH -> "Venue"
        AppLanguage.JAPANESE -> "会場"
        AppLanguage.CHINESE -> "场地"
    }
    val weather = when(language) {
        AppLanguage.ENGLISH -> "Weather"
        AppLanguage.JAPANESE -> "天気"
        AppLanguage.CHINESE -> "天气"
    }
    val wind = when(language) {
        AppLanguage.ENGLISH -> "Wind"
        AppLanguage.JAPANESE -> "風力"
        AppLanguage.CHINESE -> "风力"
    }
    // Weather options
    val sunny = when(language) {
        AppLanguage.ENGLISH -> "Sunny"
        AppLanguage.JAPANESE -> "晴れ"
        AppLanguage.CHINESE -> "晴天"
    }
    val cloudy = when(language) {
        AppLanguage.ENGLISH -> "Cloudy"
        AppLanguage.JAPANESE -> "曇り"
        AppLanguage.CHINESE -> "多云"
    }
    val rainy = when(language) {
        AppLanguage.ENGLISH -> "Rainy"
        AppLanguage.JAPANESE -> "雨"
        AppLanguage.CHINESE -> "雨天"
    }
    // Wind options
    val lowWind = when(language) {
        AppLanguage.ENGLISH -> "Low"
        AppLanguage.JAPANESE -> "弱"
        AppLanguage.CHINESE -> "小"
    }
    val midWind = when(language) {
        AppLanguage.ENGLISH -> "Mid"
        AppLanguage.JAPANESE -> "中"
        AppLanguage.CHINESE -> "中"
    }
    val highWind = when(language) {
        AppLanguage.ENGLISH -> "High"
        AppLanguage.JAPANESE -> "强"
        AppLanguage.CHINESE -> "大"
    }
    val notSet = when(language) {
        AppLanguage.ENGLISH -> "Not set"
        AppLanguage.JAPANESE -> "未設定"
        AppLanguage.CHINESE -> "未设置"
    }
    val password = when(language) {
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.JAPANESE -> "パスワード"
        AppLanguage.CHINESE -> "密码"
    }
    val logout = when(language) {
        AppLanguage.ENGLISH -> "Log Out"
        AppLanguage.JAPANESE -> "ログアウト"
        AppLanguage.CHINESE -> "退出登录"
    }
    val deleteAccount = when(language) {
        AppLanguage.ENGLISH -> "Delete Account"
        AppLanguage.JAPANESE -> "アカウント削除"
        AppLanguage.CHINESE -> "注销账户"
    }
    val settingsLanguage = when(language) {
        AppLanguage.ENGLISH -> "Language"
        AppLanguage.JAPANESE -> "言語"
        AppLanguage.CHINESE -> "语言"
    }
    val save = when(language) {
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.JAPANESE -> "保存"
        AppLanguage.CHINESE -> "保存"
    }
    val changeEmail = when(language) {
        AppLanguage.ENGLISH -> "Change Email"
        AppLanguage.JAPANESE -> "メール変更"
        AppLanguage.CHINESE -> "更改邮箱"
    }
    val changePassword = when(language) {
        AppLanguage.ENGLISH -> "Change Password"
        AppLanguage.JAPANESE -> "パスワード変更"
        AppLanguage.CHINESE -> "更改密码"
    }
    val oldPassword = when(language) {
        AppLanguage.ENGLISH -> "Old Password"
        AppLanguage.JAPANESE -> "現在のパスワード"
        AppLanguage.CHINESE -> "旧密码"
    }
    val newPassword = when(language) {
        AppLanguage.ENGLISH -> "New Password"
        AppLanguage.JAPANESE -> "新しいパスワード"
        AppLanguage.CHINESE -> "新密码"
    }
    val deleteAccountConfirm = when(language) {
        AppLanguage.ENGLISH -> "Are you sure you want to delete your account? All your sessions and scores will be permanently deleted."
        AppLanguage.JAPANESE -> "アカウントを削除してもよろしいですか？すべてのデータが永久に削除されます。"
        AppLanguage.CHINESE -> "确定要注销账户吗？所有训练数据将被永久删除。"
    }

    // Login / Signup
    val loginTitle = when(language) {
        AppLanguage.ENGLISH -> "Login"
        AppLanguage.JAPANESE -> "ログイン"
        AppLanguage.CHINESE -> "登录"
    }
    val signupTitle = when(language) {
        AppLanguage.ENGLISH -> "Sign Up"
        AppLanguage.JAPANESE -> "新規登録"
        AppLanguage.CHINESE -> "注册"
    }
    val loginButton = when(language) {
        AppLanguage.ENGLISH -> "Login"
        AppLanguage.JAPANESE -> "ログイン"
        AppLanguage.CHINESE -> "登录"
    }
    val signupButton = when(language) {
        AppLanguage.ENGLISH -> "Sign Up"
        AppLanguage.JAPANESE -> "登録"
        AppLanguage.CHINESE -> "注册"
    }
    val noAccount = when(language) {
        AppLanguage.ENGLISH -> "No account? Sign up"
        AppLanguage.JAPANESE -> "アカウントをお持ちでない方はこちら"
        AppLanguage.CHINESE -> "没有账号？去注册"
    }
    val hasAccount = when(language) {
        AppLanguage.ENGLISH -> "Already have an account? Login"
        AppLanguage.JAPANESE -> "既にアカウントをお持ちの方はこちら"
        AppLanguage.CHINESE -> "已有账号？去登录"
    }
    val loginFailed = when(language) {
        AppLanguage.ENGLISH -> "Login Failed. Please check your credentials."
        AppLanguage.JAPANESE -> "ログインに失敗しました。入力内容を確認してください。"
        AppLanguage.CHINESE -> "登录失败，请检查用户名或密码。"
    }
    val signupFailed = when(language) {
        AppLanguage.ENGLISH -> "Sign up failed. Username might be taken."
        AppLanguage.JAPANESE -> "登録に失敗しました。ユーザー名が既に使われている可能性があります。"
        AppLanguage.CHINESE -> "注册失败，用户名可能已被占用。"
    }

    // Log Session
    val end = when(language) {
        AppLanguage.ENGLISH -> "End"
        AppLanguage.JAPANESE -> "エンド"
        AppLanguage.CHINESE -> "组"
    }
    val totalScore = when(language) {
        AppLanguage.ENGLISH -> "Total Score"
        AppLanguage.JAPANESE -> "合計"
        AppLanguage.CHINESE -> "总分"
    }
    val nextEnd = when(language) {
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.JAPANESE -> "次のエンド"
        AppLanguage.CHINESE -> "下一组"
    }
    val finish = when(language) {
        AppLanguage.ENGLISH -> "Finish"
        AppLanguage.JAPANESE -> "記録完了"
        AppLanguage.CHINESE -> "记录完成"
    }
    val undo = when(language) {
        AppLanguage.ENGLISH -> "Undo"
        AppLanguage.JAPANESE -> "取り消し"
        AppLanguage.CHINESE -> "撤销"
    }
    val abandonSessionTitle = when(language) {
        AppLanguage.ENGLISH -> "Abandon Session"
        AppLanguage.JAPANESE -> "練習を破棄"
        AppLanguage.CHINESE -> "放弃练习"
    }
    val abandonSessionConfirm = when(language) {
        AppLanguage.ENGLISH -> "Are you sure you want to abandon this session? Current records will be deleted."
        AppLanguage.JAPANESE -> "この練習を破棄してもよろしいですか？現在の記録は削除されます。"
        AppLanguage.CHINESE -> "确定要放弃本次练习吗？当前记录将被删除且无法找回。"
    }
    val sessionDetailsTitle = when(language) {
        AppLanguage.ENGLISH -> "Session Details"
        AppLanguage.JAPANESE -> "セッション詳細"
        AppLanguage.CHINESE -> "训练详情"
    }
    fun getEndCompleteMessage(current: Int): String {
        val isLast = current >= 6
        return when(language) {
            AppLanguage.ENGLISH -> if (isLast) "All 6 ends complete. Finish training?" else "End $current complete. Move to end ${current + 1}?"
            AppLanguage.JAPANESE -> if (isLast) "6エンドすべての記録が完了しました。練習を終了しますか？" else "${current}エンドの记录が完了しました。${current + 1}エンドに進みますか？"
            AppLanguage.CHINESE -> if (isLast) "6轮练习已全部记录完成。要结束并保存吗？" else "第${current}轮记录完成，要开始第${current + 1}轮吗？"
        }
    }

    val bufferMessage = when(language) {
        AppLanguage.ENGLISH -> "End complete.\nTo modify, tap the BLACK 'Undo' button.\nTo proceed, tap the GREEN 'Next' button below."
        AppLanguage.JAPANESE -> "エンド記録完了。\n修正するには黒の「取り消し」を、\n次へ進むには下の緑色「次へ」ボタンをタップしてください。"
        AppLanguage.CHINESE -> "本组录入已完成。\n如需修改，请点击左下角黑色的“撤销”按钮；\n如需进入下一组，请点击下方的绿色“下一组”按钮。"
    }

    val myFavorites = when(language) {
        AppLanguage.ENGLISH -> "Favorites"
        AppLanguage.JAPANESE -> "いいね"
        AppLanguage.CHINESE -> "收藏"
    }

    val viewSavedAdvice = when(language) {
        AppLanguage.ENGLISH -> "View Saved Advice"
        AppLanguage.JAPANESE -> "保存されたアドバイスを表示"
        AppLanguage.CHINESE -> "查看已保存的建议"
    }

    val lastEndNotice = when(language) {
        AppLanguage.ENGLISH -> "This is the last end. Please press [Finish] to end the session."
        AppLanguage.JAPANESE -> "これが最後のエンドです。終了したら「記録完了」をクリックしてください。"
        AppLanguage.CHINESE -> "这已经是最后一组，打完本组后请点击“记录完成”以关闭。"
    }

    val saveToFavorites = when(language) {
        AppLanguage.ENGLISH -> "Save to Favorites"
        AppLanguage.JAPANESE -> "いいね"
        AppLanguage.CHINESE -> "收藏此建议"
    }

    val savedSuccess = when(language) {
        AppLanguage.ENGLISH -> "Saved!"
        AppLanguage.JAPANESE -> "保存しました！"
        AppLanguage.CHINESE -> "已收藏！"
    }

    val syncingData = when(language) {
        AppLanguage.ENGLISH -> "Syncing data from cloud..."
        AppLanguage.JAPANESE -> "クラウドから同期中..."
        AppLanguage.CHINESE -> "正在从云端同步历史记录..."
    }

    // Date Picker
    val pickerCancel = when(language) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.JAPANESE -> "キャンセル"
        AppLanguage.CHINESE -> "取消"
    }
    val pickerDone = when(language) {
        AppLanguage.ENGLISH -> "Done"
        AppLanguage.JAPANESE -> "完了"
        AppLanguage.CHINESE -> "确定"
    }
    val pickerYesterday = when(language) {
        AppLanguage.ENGLISH -> "Yesterday"
        AppLanguage.JAPANESE -> "昨日"
        AppLanguage.CHINESE -> "昨天"
    }
    val pickerToday = when(language) {
        AppLanguage.ENGLISH -> "Today"
        AppLanguage.JAPANESE -> "今日"
        AppLanguage.CHINESE -> "今天"
    }
    val pickerYearSuffix = when(language) {
        AppLanguage.ENGLISH -> ""
        AppLanguage.JAPANESE -> "年"
        AppLanguage.CHINESE -> "年"
    }
    val pickerMonthSuffix = when(language) {
        AppLanguage.ENGLISH -> ""
        AppLanguage.JAPANESE -> "月"
        AppLanguage.CHINESE -> "月"
    }
    val pickerDaySuffix = when(language) {
        AppLanguage.ENGLISH -> ""
        AppLanguage.JAPANESE -> "日"
        AppLanguage.CHINESE -> "日"
    }
    // For English, we use month names instead of numbers
    val pickerMonthNames = when(language) {
        AppLanguage.ENGLISH -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        AppLanguage.JAPANESE -> emptyList()
        AppLanguage.CHINESE -> emptyList()
    }
    val pickerLocale = when(language) {
        AppLanguage.ENGLISH -> java.util.Locale.ENGLISH
        AppLanguage.JAPANESE -> java.util.Locale.JAPANESE
        AppLanguage.CHINESE -> java.util.Locale.CHINESE
    }
}
