package com.yukihiro.speedWithYukihiro

import kotlin.random.Random

class ComPlayer(private val engine: SpeedGameEngine) {
    // プレイヤー・COM双方がカードを出せるかチェック
    fun canAnyonePlay(): Boolean {
        val (left, right) = engine.fieldCards
        val playerCan = engine.playerHands.any { left != null && engine.canPlaceCard(left, it) || (right != null && engine.canPlaceCard(right, it)) }
        val comCan = engine.comHands.any { left != null && engine.canPlaceCard(left, it) || (right != null && engine.canPlaceCard(right, it )) }
        return playerCan || comCan
    }

    // COMの1ステップの思考
    fun executeThink() {
        if (engine.winner != null) return

        // 💡 選択された難易度の確率に基づいて行動するか決める
        val currentDiff = engine.selectedDifficulty
        if (Random.nextDouble() > currentDiff.successChance) return

        val (left, right) = engine.fieldCards

        val playableCard = engine.comHands.firstOrNull { card ->
            (left != null && engine.canPlaceCard(left, card)) || (right != null && engine.canPlaceCard(right, card))
        } ?: return

        // 出せる方に配置
        if (left != null && engine.canPlaceCard(left, playableCard)) {
            engine.fieldCards = Pair(playableCard, right)
            engine.resetCombo()
        } else if (right != null && engine.canPlaceCard(right, playableCard)) {
            engine.fieldCards = Pair(left, playableCard)
            engine.resetCombo()
        }

        // 手札の更新
        engine.comHands = engine.refreshHands(engine.comHands, playableCard)
    }
}