package com.sakuramanager;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
  private WebView webView;
  private ValueCallback<Uri[]> uploadCallback;
  private static final int FILE_CHOOSER_REQUEST = 9001;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    webView = new WebView(this);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setAllowFileAccess(false);
    webView.getSettings().setAllowContentAccess(true);
    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        uploadCallback = callback;
        Intent intent = params.createIntent();
        try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
        catch (Exception e) { uploadCallback.onReceiveValue(null); uploadCallback = null; return false; }
        return true;
      }
    });
    setContentView(webView);
    if (savedInstanceState == null) webView.loadUrl("https://sakura-cosmetics-v2-manager-eu3dshi8g-farouk-moustafa.vercel.app/");
    else webView.restoreState(savedInstanceState);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == FILE_CHOOSER_REQUEST && uploadCallback != null) {
      Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
      uploadCallback.onReceiveValue(result);
      uploadCallback = null;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    webView.saveState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override public void onBackPressed() {
    if (webView != null && webView.canGoBack()) webView.goBack();
    else super.onBackPressed();
  }
}
