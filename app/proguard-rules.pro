# --- WorkManager の難読化回避 ---
-keep class androidx.work.** { *; }

# --- 【重要】AdMob (Google Mobile Ads SDK) の難読化回避 ---
# 前回お話しした広告が表示されない問題もこれで一緒に予防・解決します
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}

# 念のため、Android Startupライブラリも保護
-keep class androidx.startup.** { *; }