plugins { id("com.android.application") }
android { namespace = "ch.nova.iptv"; compileSdk = 35
 defaultConfig { applicationId = "ch.nova.iptv"; minSdk = 26; targetSdk = 28; versionCode = 1; versionName = "0.1" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("androidx.media3:media3-exoplayer:1.5.1"); implementation("androidx.media3:media3-exoplayer-hls:1.5.1"); implementation("androidx.media3:media3-ui:1.5.1") }
