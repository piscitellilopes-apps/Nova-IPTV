package ch.piscis.iptvpro;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
  web=new WebView(this);
  web.setFocusable(true);
  web.setFocusableInTouchMode(true);
  web.setWebViewClient(new WebViewClient());
  web.setWebChromeClient(new WebChromeClient());
  WebSettings s=web.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setMediaPlaybackRequiresUserGesture(false);
  s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
  s.setAllowFileAccess(true);
  s.setAllowFileAccessFromFileURLs(true);
  s.setAllowUniversalAccessFromFileURLs(true);
  setContentView(web);
  web.loadUrl("file:///android_asset/index.html");
  web.requestFocus();
 }
 @Override public boolean onKeyDown(int keyCode,KeyEvent event) {
  if(keyCode==KeyEvent.KEYCODE_BACK && web.canGoBack()){web.goBack();return true;}
  return super.onKeyDown(keyCode,event);
 }
 @Override protected void onDestroy(){web.destroy();super.onDestroy();}
}