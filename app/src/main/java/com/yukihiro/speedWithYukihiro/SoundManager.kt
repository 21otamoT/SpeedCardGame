package com.yukihiro.speedWithYukihiro

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {
    private var swipeSoundId: Int = 0
    // SoundPoolの初期化（効果音に適した低遅延設定
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5) // 同時に鳴らせる最大音数
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    init {
        // 音声ファイルの読み込み (res/raw/se_swipe.mp3)
        swipeSoundId = soundPool.load(context, R.raw.se_swipe, 1)
    }

    // シュッという音を鳴らす関数
    fun playSwipe() {
        if (swipeSoundId != 0) {
            // 左右の音量1.0, 優先度1, ループ0(なし), 再生速度1.0
            soundPool.play(swipeSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }
    // アプリ終了時にメモリを解放する
    fun release() {
        soundPool.release()
    }
}