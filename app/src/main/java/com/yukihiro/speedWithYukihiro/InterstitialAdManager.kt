package com.yukihiro.speedWithYukihiro

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val activity: Activity) {

    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "InterstitialAdManager"

    // テスト用インタースティシャル広告ユニットID
    private val adUnitId: String by lazy {
        activity.getString(R.string.admob_unit_id)
    }

    /**
     * 広告をバックグラウンドでロード（準備）する
     */
    fun loadAd() {
        // 💡 広告IDが本当に取得できているかログに出力する
        Log.d("AdCheck", "現在読み込もうとしている広告IDはこれです: $adUnitId")
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(activity, adUnitId, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "広告の読み込みに失敗しました: ${adError.message}")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "広告の読み込みに成功しました。")
                    mInterstitialAd = interstitialAd
                }
            })
    }

    /**
     * 広告を表示する
     * @param onAdClosed 広告が閉じられた、または表示できなかった時に実行したい本来の処理（コールバック）
     */
    fun showAd(onAdClosed: () -> Unit) {
        if (mInterstitialAd != null) {
            // 広告のイベントを設定
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "広告が閉じられました。")
                    mInterstitialAd = null
                    loadAd() // 次のために再ロード
                    onAdClosed() // 本来の処理を実行
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "広告の表示に失敗しました。")
                    mInterstitialAd = null
                    onAdClosed() // 失敗してもアプリが止まらないように次の処理へ
                }
            }
            // 広告を表示
            mInterstitialAd?.show(activity)
        } else {
            Log.d(TAG, "❌ 広告を表示しようとしましたが、まだダウンロードが完了していません（nullです）。")
            onAdClosed() // 広告を出さずに次の処理へ
        }
    }
}