package com.yukihiro.speedWithYukihiro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlin.Pair
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.ExperimentalFoundationApi // 👈 animateItem用
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adManager = InterstitialAdManager(this)
        // SDKの初期化と広告の初回ロード
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {
                runOnUiThread {
                    // ログで確認した、正しいIDを使ってロードが始まります
                    adManager.loadAd()
                }
            }
        }
        setContent {
            val context = LocalContext.current
            // 💡 エンジンとCOMクラスをrememberで生成して保持
            val engine = remember { SpeedGameEngine() }
            val comPlayer = remember { ComPlayer(engine) }

            // 勝敗の監視
            LaunchedEffect(engine.playerHands, engine.comHands, engine.drawDeck) {
                engine.checkWinner()
                if (engine.winner != null && engine.currentScreen == "GAME") {
                    // 勝敗が決まったら、まず広告を表示する
                    adManager.showAd {
                        // 💡 広告が閉じられた（または準備がなくてスキップされた）後に、初めて結果画面に切り替える
                        engine.currentScreen = "RESULT"
                    }
                }
            }

            // COMの自動思考 ＋ 手詰まり自動解消ループ
            LaunchedEffect(engine.winner, engine.currentScreen) {
                if (engine.winner != null || engine.currentScreen != "GAME") return@LaunchedEffect
                var stuckCount = 0
                while (true) {
                    delay(1000)

                    if (!comPlayer.canAnyonePlay()) {
                        stuckCount++
                        if (stuckCount >= 2) {
                            val msg = engine.handleStuck()
                            if (msg.isNotEmpty()) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            stuckCount = 0
                        }
                        continue
                    }

                    stuckCount = 0
                    comPlayer.executeThink()
                }
            }

            // アニメーション用State
            var leftFieldOffset by remember { mutableStateOf(Offset.Zero) }
            var rightFieldOffset by remember { mutableStateOf(Offset.Zero) }
            // アニメーション中のカード情報（nullならアニメーションしていない）
            var flyingCard by remember { mutableStateOf<Card?>(null) }
            var flyingCardOffset by remember { mutableStateOf(Offset.Zero) }
            // 実際のアニメーション値を生成するクラス
            val animatableOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
            val coroutineScope = rememberCoroutineScope()


            // -------------------------------------------------------------
            // 💡 画面レイアウト（currentScreen の値で3種類に条件分岐）
            // -------------------------------------------------------------
            when (engine.currentScreen) {
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
                                        engine.currentScreen = "GAME"
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
                    // 💡 画面全体を覆う最大のコンテナ（Box）を用意します
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bg_game),
                            contentDescription = "ゲーム画面の背景",
                            modifier = Modifier.fillMaxSize(),
                            // 縦横比を保ったまま画面いっぱいに広げて切り抜く設定（CSSの background-size: cover）
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 【上】COMの手札
                            PlayerHandsView(
                                hands = engine.comHands,
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
                                engine.fieldCards.first?.let { leftCard ->
                                    PlayingCardView(card = leftCard, modifier = Modifier
                                        .padding(8.dp)
                                        .onGloballyPositioned{ coordinates ->
                                            // 左の場の絶対座標を保存
                                            leftFieldOffset = coordinates.positionInRoot()
                                        }
                                    )
                                }
                                // 右の場
                                engine.fieldCards.second?.let { rightCard ->
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
                                hands = engine.playerHands,
                                onCardClick = { clickedCard, startOffset ->
                                    val (leftField, rightField) = engine.fieldCards

                                    // 飛ぶ前の事前チェック
                                    if ((leftField != null && engine.canPlaceCard(leftField, clickedCard)) ||
                                        (rightField != null && engine.canPlaceCard(rightField, clickedCard))) {

                                        coroutineScope.launch {
                                            flyingCard = clickedCard
                                            animatableOffset.snapTo(startOffset)

                                            val targetOffset = if (leftField != null && engine.canPlaceCard(leftField, clickedCard)) {
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
                                            val currentLeft = engine.fieldCards.first
                                            val currentRight = engine.fieldCards.second

                                            // 最新の場で改めて出せるかチェック！（飛んでいる間にCOMに出されていないか？）
                                            if (currentLeft != null && engine.canPlaceCard(currentLeft, clickedCard)) {
                                                engine.fieldCards = Pair(clickedCard, currentRight)
                                                engine.playerHands = engine.refreshHands(engine.playerHands, clickedCard)
                                            } else if (currentRight != null && engine.canPlaceCard(currentRight, clickedCard)) {
                                                engine.fieldCards = Pair(currentLeft, clickedCard)
                                                engine.playerHands = engine.refreshHands(engine.playerHands, clickedCard)
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
                                text = if (engine.winner == "PLAYER") "🥳 YOU WIN! 🎉" else "😭 YOU LOSE... 💔",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (engine.winner == "PLAYER") Color(0xFF4CAF50) else Color.Red
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    // リセットしてゲームプレイ画面（"GAME"）に戻す
                                    engine.resetGame()
                                    engine.currentScreen = "GAME"
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