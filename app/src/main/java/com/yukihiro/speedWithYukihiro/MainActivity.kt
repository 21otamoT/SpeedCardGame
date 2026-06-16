package com.yukihiro.speedWithYukihiro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.Pair
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.ExperimentalFoundationApi // 👈 animateItem用
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            // 💡 画面の遷移状態を管理するStateを追加します（初期値は "START"）
            var currentScreen by remember { mutableStateOf("START") }

            // 1. デッキを1回だけ生成・シャッフル（ここは大正解です！）
            val initialShuffledDeck = remember { createShuffledDeck() }

            // 2. 生成したデッキを使って、それぞれのStateを初期化する（空リストにせず、ここで配る！）
            var playerHands by remember { mutableStateOf(initialShuffledDeck.subList(0, 4)) }
            var comHands by remember { mutableStateOf(initialShuffledDeck.subList(4, 8)) }
            var fieldCards by remember {
                mutableStateOf(
                    Pair<Card?, Card?>(
                        initialShuffledDeck[8],
                        initialShuffledDeck[9]
                    )
                )
            }
            var drawDeck by remember {
                mutableStateOf(
                    initialShuffledDeck.subList(
                        10,
                        initialShuffledDeck.size
                    )
                )
            }

            // null: プレイ中, "PLAYER": プレイヤー勝利, "COM": COMの勝利
            var winner by remember { mutableStateOf<String?>(null) }

            // GAME画面のスコープ内に追加
            var leftFieldOffset by remember { mutableStateOf(Offset.Zero) }
            var rightFieldOffset by remember { mutableStateOf(Offset.Zero) }

            // アニメーション中のカード情報（nullならアニメーションしていない）
            var flyingCard by remember { mutableStateOf<Card?>(null) }
            var flyingCardOffset by remember { mutableStateOf(Offset.Zero) }
            // 実際のアニメーション値を生成するクラス
            val animatableOffset = remember {
                Animatable(
                    Offset.Zero,
                    Offset.VectorConverter
                )
            }

            // -------------------------------------------------------------
            // 【新設】手札や山札の変化を監視して勝敗を判定する (Reactの useEffect に相当)
            // -------------------------------------------------------------
            LaunchedEffect(playerHands, comHands, drawDeck) {
                if (winner != null) return@LaunchedEffect

                // プレイヤーの手札と山札が両方空ならプレイヤーの勝ち
                if (playerHands.isEmpty() && drawDeck.isEmpty()) {
                    winner = "PLAYER"
                    currentScreen = "RESULT"
                }
                // COMの手札と山札が両方空ならCOMの勝ち
                else if (comHands.isEmpty() && drawDeck.isEmpty()) {
                    winner = "COM"
                    currentScreen = "RESULT"
                }
            }

            // -------------------------------------------------------------
            // COMの自動思考 ＋ 手詰まり自動解消ループ
            // -------------------------------------------------------------
            LaunchedEffect(winner, currentScreen) {
                if (winner != null || currentScreen != "GAME") return@LaunchedEffect
                var stuckCount = 0
                while (true) {
                    delay(1000)

                    val leftField = fieldCards.first
                    val rightField = fieldCards.second

                    val playerCanPlay = playerHands.any { card ->
                        (leftField != null && canPlaceCard(
                            leftField,
                            card
                        )) || (rightField != null && canPlaceCard(rightField, card))
                    }
                    val comCanPlay = comHands.any { card ->
                        (leftField != null && canPlaceCard(
                            leftField,
                            card
                        )) || (rightField != null && canPlaceCard(rightField, card))
                    }

                    // 誰も出せない（手詰まり）状態のときの処理
                    if (!playerCanPlay && !comCanPlay) {
                        stuckCount++
                        if (stuckCount >= 2) {
                            if (drawDeck.size >= 2) {
                                fieldCards = Pair(drawDeck[0], drawDeck[1])
                                drawDeck = drawDeck.drop(2)
                                Toast.makeText(
                                    context,
                                    "あいない！（場を更新します）",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                if (playerHands.isNotEmpty() && comHands.isNotEmpty()) {
                                    fieldCards = Pair(playerHands.first(), comHands.first())
                                    playerHands = playerHands.drop(1)
                                    comHands = comHands.drop(1)
                                    Toast.makeText(
                                        context,
                                        "山札がありません！手札を場に出します",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            stuckCount = 0
                        }
                        continue
                    }

                    stuckCount = 0

                    // 【COMの思考】50%の確率で実行
                    if (Math.random() > 0.5) {
                        val playableCard = comHands.firstOrNull { card ->
                            (leftField != null && canPlaceCard(
                                leftField,
                                card
                            )) || (rightField != null && canPlaceCard(rightField, card))
                        }
                        if (playableCard != null) {
                            if (leftField != null && canPlaceCard(leftField, playableCard)) {
                                fieldCards = Pair(playableCard, rightField)
                            } else if (rightField != null && canPlaceCard(
                                    rightField,
                                    playableCard
                                )
                            ) {
                                fieldCards = Pair(leftField, playableCard)
                            }

                            comHands = refreshHands(comHands, playableCard, drawDeck)
                            if (drawDeck.isNotEmpty()) drawDeck = drawDeck.drop(1)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 💡 画面レイアウト（currentScreen の値で3種類に条件分岐）
            // -------------------------------------------------------------
            when (currentScreen) {
                "START" -> {
                    // 🎬 【1. スタート画面】
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bg_title),
                            contentDescription = "背景画像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // 2. 💡 上側のエリアを全体の「2/3」にするための Spacer
                            Spacer(modifier = Modifier.weight(2f))

                            // =============================================================
                            // 📥 【下側 1/3 のエリア】（ボタンをこのエリアの真ん中に置く）
                            // =============================================================
                            // 3. この Box が下側「1/3」のエリアを確保し、その中の中央（Alignment.Center）にボタンを配置します
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f), // 💡 全体の 1/3 の高さを確保
                                contentAlignment = Alignment.Center // 💡 エリア内の中央に寄せる
                            ) {
                                // 4. タップするとゲーム画面に遷移するボタン
                                Button(
                                    onClick = {
                                        currentScreen = "GAME"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text(
                                        "ゲームを始める",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                "GAME" -> {
                    val coroutineScope = rememberCoroutineScope()

                    // 💡 画面全体を覆う最大のコンテナ（Box）を用意します
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // -------------------------------------------------------------
                        // 💡 【新要素】ゲーム画面の背景画像を一番奥に敷きます
                        // -------------------------------------------------------------
                        Image(
                            painter = painterResource(id = R.drawable.bg_game),
                            contentDescription = "ゲーム画面の背景",
                            modifier = Modifier.fillMaxSize(),
                            // 縦横比を保ったまま画面いっぱいに広げて切り抜く設定（CSSの background-size: cover）
                            contentScale = ContentScale.Crop
                        )

                        // 💡 以前作ったゲームプレイのUI全体（Column）です。背景画像の上に重なります。
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 【上】COMの手札
                            PlayerHandsView(
                                hands = comHands,
                                onCardClick = { _, _ ->
                                    Toast.makeText(context, "それは相手の手札です", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 【中央】場のカード（台札：左と右）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(vertical = 32.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左の場
                                fieldCards.first?.let { leftCard ->
                                    PlayingCardView(card = leftCard, modifier = Modifier
                                        .padding(8.dp)
                                        .onGloballyPositioned{ coordinates ->
                                            // 左の場の絶対座標を保存
                                            leftFieldOffset = coordinates.positionInRoot()
                                        }
                                    )
                                }
                                // 右の場
                                fieldCards.second?.let { rightCard ->
                                    PlayingCardView(card = rightCard, modifier = Modifier
                                        .padding(8.dp)
                                        .onGloballyPositioned{ coordinates ->
                                            // 右の場の絶対座標を保存
                                            rightFieldOffset = coordinates.positionInRoot()
                                        }
                                    )
                                }
                            }

                            // 【下】プレイヤーの手札
                            PlayerHandsView(
                                hands = playerHands,
                                onCardClick = { clickedCard, startOffset ->
                                    val leftField = fieldCards.first
                                    val rightField = fieldCards.second

                                    // 飛ぶ前の事前チェック
                                    if ((leftField != null && canPlaceCard(leftField, clickedCard)) ||
                                        (rightField != null && canPlaceCard(rightField, clickedCard))) {

                                        coroutineScope.launch {
                                            flyingCard = clickedCard
                                            animatableOffset.snapTo(startOffset)

                                            val targetOffset = if (leftField != null && canPlaceCard(leftField, clickedCard)) {
                                                leftFieldOffset
                                            } else {
                                                rightFieldOffset
                                            }

                                            // 🚀 場に向かってスライドアニメーションを実行！（250ミリ秒）
                                            animatableOffset.animateTo(
                                                targetValue = targetOffset,
                                                animationSpec = tween(durationMillis = 250)
                                            )

                                            // 🌟 【重要】アニメーション完了後の「最新の場の状態」を取得する
                                            val currentLeft = fieldCards.first
                                            val currentRight = fieldCards.second

                                            // 最新の場で改めて出せるかチェック！（飛んでいる間にCOMに出されていないか？）
                                            if (currentLeft != null && canPlaceCard(currentLeft, clickedCard)) {
                                                fieldCards = Pair(clickedCard, currentRight)
                                                playerHands = refreshHands(playerHands, clickedCard, drawDeck)
                                                if (drawDeck.isNotEmpty()) drawDeck = drawDeck.drop(1)
                                            } else if (currentRight != null && canPlaceCard(currentRight, clickedCard)) {
                                                fieldCards = Pair(currentLeft, clickedCard)
                                                playerHands = refreshHands(playerHands, clickedCard, drawDeck)
                                                if (drawDeck.isNotEmpty()) drawDeck = drawDeck.drop(1)
                                            } else {
                                                // タッチの差でCOMに先を越された場合の処理（スピードの醍醐味！）
                                                Toast.makeText(context, "タッチの差で出せなくなった！", Toast.LENGTH_SHORT).show()
                                            }

                                            flyingCard = null
                                        }
                                    } else {
                                        Toast.makeText(context, "そのカードは出せません！", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        // flyingCardがnullでない時だけ、最前面にカードを描画
                        flyingCard?.let { card ->
                            PlayingCardView(
                                card = card,
                                modifier = Modifier
                                    // 取得したアニメーションの現在地へオフセット（移動）させる
                                    .offset { IntOffset(animatableOffset.value.x.toInt(), animatableOffset.value.y.toInt()) }
                                    .zIndex(10f) // 他の全ての要素より上に表示
                            )
                        }
                    }
                }
                "RESULT" -> {
                    // 🎉 【3. 結果画面】
                    Image(painter = painterResource(
                        id = R.drawable.bg_result),
                        contentDescription = "結果画像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (winner == "PLAYER") "🥳 YOU WIN! 🎉" else "😭 YOU LOSE... 💔",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (winner == "PLAYER") Color(0xFF4CAF50) else Color.Red
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    // リセットしてゲームプレイ画面（"GAME"）に戻す
                                    val newDeck = createShuffledDeck()
                                    playerHands = newDeck.subList(0, 4)
                                    comHands = newDeck.subList(4, 8)
                                    fieldCards = Pair(newDeck[8], newDeck[9])
                                    drawDeck = newDeck.subList(10, newDeck.size)
                                    winner = null

                                    currentScreen = "GAME" // 💡 再びゲーム画面へ戻す
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("もう一度遊ぶ", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

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

@Composable
fun PlayingCardView(card: Card, modifier: Modifier = Modifier) {
    val textColor = if (card.suit.isRed) Color.Red else Color.Black

    // マテリアルデザインの「Card」を使用（React NativeのViewにスタイルをあてる感覚）
    Card(
        modifier = modifier
            .width(90.dp)
            .height(130.dp)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 左上の数字とマーク
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.displayText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(text = card.suit.symbol, fontSize = 14.sp, color = textColor)
            }

            // 中央の大きなマーク
            Text(
                text = card.suit.symbol,
                fontSize = 36.sp,
                color = textColor,
                modifier = Modifier.align(Alignment.Center)
            )

            // 右下の逆向きの数字とマーク（スピードっぽさを出すため）
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = card.suit.symbol, fontSize = 14.sp, color = textColor)
                Text(
                    text = card.displayText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerHandsView(
    hands: List<Card>,
    onCardClick: (Card, Offset) -> Unit // 💡 座標(Offset)も一緒に渡すように変更
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = 4,
            key = { index ->
                if (index < hands.size) hands[index].id else "empty_$index"
            }
        ) { index ->
            if (index < hands.size) {
                val card = hands[index]
                // 💡 このカードの画面上の現在地を保存するState
                var currentOffset by remember { mutableStateOf(Offset.Zero) }

                PlayingCardView(
                    card = card,
                    modifier = Modifier
                        .animateItem()
                        .fillParentMaxWidth(0.22f)
                        .onGloballyPositioned { coordinates ->
                            // 💡 描画されたら絶対座標を取得
                            currentOffset = coordinates.positionInRoot()
                        }
                        // 💡 クリックされた時、カード情報と一緒に「現在地」も渡す
                        .clickable { onCardClick(card, currentOffset) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .animateItem()
                        .fillParentMaxWidth(0.22f)
                        .height(130.dp)
                )
            }
        }
    }
}

fun refreshHands(currentHands: List<Card>, usedCard: Card, deck: List<Card>): List<Card> {
    // Reactの currentHands.map(c => c === usedCard ? deck[0] : c) と同じ処理
    return currentHands.map { card ->
        if (card == usedCard) {
            if (deck.isNotEmpty()) {
                deck.first() // 山札が残っていればその1枚を補充
            } else {
                // 山札が空なら、この手札の枠は消去（スピードの終盤状態）
                null
            }
        } else {
            card
        }
    }.filterNotNull() // nullになった要素（空の枠）を除去して詰める
}

// カードを出せるかどうかの判定関数
fun canPlaceCard(fieldCard: Card, playerCard: Card): Boolean {
    val diff = Math.abs(fieldCard.value - playerCard.value)
    // 差が 1 (例: 3と4)、または 12 (1と13の繋がり) であれば出せる
    return diff == 1 || diff == 12
}

fun createShuffledDeck(): List<Card> {
    val fullDeck = mutableListOf<Card>()
    for (suit in Suit.values()) {
        for (value in 1..13) {
            fullDeck.add(Card(suit, value))
        }
    }
    return fullDeck.shuffled()
}