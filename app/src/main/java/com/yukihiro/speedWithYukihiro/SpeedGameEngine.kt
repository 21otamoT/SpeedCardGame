package com.yukihiro.speedWithYukihiro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SpeedGameEngine {
    // 全ての状態（State）をエンジンが管理する
    var currentScreen by mutableStateOf("START")
    var playerHands by mutableStateOf<List<Card>>(emptyList())
    var comHands by mutableStateOf<List<Card>>(emptyList())
    var fieldCards by mutableStateOf<Pair<Card?, Card?>>(Pair(null, null))
    var drawDeck by mutableStateOf<List<Card>>(emptyList())
    var winner by mutableStateOf<String?>(null)

    init {
        resetGame()
    }

    fun resetGame() {
        val newDeck = createShuffledDeck()
        playerHands = newDeck.subList(0, 4)
        comHands = newDeck.subList(4, 8)
        fieldCards = Pair(newDeck[8], newDeck[9])
        drawDeck = newDeck.subList(10, newDeck.size)
        winner = null
    }

    private fun createShuffledDeck(): List<Card> {
        val fullDeck = mutableListOf<Card>()
        for (suit in Suit.entries) {
            for (value in 1..13) {
                fullDeck.add(Card(suit, value))
            }
        }
        return fullDeck.shuffled()
    }

    // カードを出せるかどうかの判定関数
    fun canPlaceCard(fieldCard: Card, playerCard: Card): Boolean {
        val diff = Math.abs(fieldCard.value - playerCard.value)
        // 差が 1 (例: 3と4)、または 12 (1と13の繋がり) であれば出せる
        return diff == 1 || diff == 12
    }

    fun checkWinner() {
        if (winner != null) return
        if (playerHands.isEmpty() && drawDeck.isEmpty()) {
            winner = "PLAYER"
//            currentScreen = "RESULT"
        } else if (comHands.isEmpty() && drawDeck.isEmpty()) {
            winner = "COM"
//            currentScreen = "RESULT"
        }
    }

    fun refreshHands(currentHands: List<Card>, usedCard: Card): List<Card> {
        return currentHands.map { card ->
            if (card == usedCard) {
                if (drawDeck.isNotEmpty()) {
                    val nextCard = drawDeck.first()
                    drawDeck = drawDeck.drop(1)
                    nextCard
                } else null
            } else card
        }.filterNotNull()
    }

    // 誰も出せない（手詰まり）状態のときの処理。メッセージ種別を返却
    fun handleStuck(): String {
        return if (drawDeck.size >= 2) {
            fieldCards = Pair(drawDeck[0], drawDeck[1])
            drawDeck = drawDeck.drop(2)
            "あいない！（場を更新します）"
        } else if (playerHands.isNotEmpty() && comHands.isNotEmpty()) {
            val nextPlayerField = playerHands.first()
            val nextComField = comHands.first()
            fieldCards = Pair(nextPlayerField, nextComField)
            playerHands = playerHands.drop(1)
            comHands = comHands.drop(1)
            "山札がありません！手札を場に出します"
        } else ""
    }
}
