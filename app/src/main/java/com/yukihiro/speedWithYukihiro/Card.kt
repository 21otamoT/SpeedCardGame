package com.yukihiro.speedWithYukihiro

import java.util.UUID

// スート（マーク）の定義
enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADE("♠", false),
    HEART("♥", true),
    DIAMOND("♦", true),
    CLUB("♣", false)
}

// カード1枚を表するデータモデル
data class Card(
    val suit: Suit,
    val value: Int, // 1〜13
    val id: String = UUID.randomUUID().toString()
) {
    // 表示用の文字（1 -> "A", 11 -> "J", 12 -> "Q", 13 -> "K"）
    val displayText: String
        get() = when (value) {
            1 -> "A"
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            else -> value.toString()
        }
}