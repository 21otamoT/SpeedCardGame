package com.yukihiro.speedWithYukihiro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel: ViewModel() {
    // 回転やActivity再生成を越えて生き残る
    val engine = SpeedGameEngine()
    private val comPlayer = ComPlayer(engine)

    // Toast等で表示したい一回限りのメッセージ
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var comJob: Job? = null

    /** START画面 → ゲーム開始 */
    fun startGame() {
        engine.currentScreen = "GAME"
        startComLoop()
    }

    /** RESULT画面 → もう一度遊ぶ */
    fun retry() {
        stopComLoop()
        engine.resetGame()
        engine.currentScreen = "GAME"
        startComLoop()
    }

    /** 勝敗が決まったらCOMを止める */
    fun onGameFinished() {
        stopComLoop()
    }

    private fun startComLoop() {
        if (comJob?.isActive == true) return // 二重起動防止
        comJob = viewModelScope.launch {
            var stuckCount = 0
            while (isActive && engine.winner == null && engine.currentScreen == "GAME") {
                delay(engine.selectedDifficulty.delayMillis)

                if (!comPlayer.canAnyonePlay()) {
                    stuckCount++
                    if (stuckCount >= 2) {
                        val msg = engine.handleStuck()
                        if (msg.isNotEmpty()) _messages.tryEmit(msg)
                        stuckCount = 0
                    }
                    continue
                }
                stuckCount = 0
                comPlayer.executeThink()
            }
        }
    }

    private fun stopComLoop() {
        comJob?.cancel()
        comJob = null
    }

    override fun onCleared() {
        stopComLoop()
        super.onCleared()
    }
}