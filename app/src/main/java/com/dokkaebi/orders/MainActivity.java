package com.dokkaebi.orders;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * 도깨비 주문관리: the Google Apps Script web app shown full-screen.
 * All data stays in the Google Sheet; this app only opens the page.
 */
public class MainActivity extends Activity {

    /**
     * 웹 앱 주소. Apps Script [배포] > [배포 관리]에 나오는 "웹 앱 URL"(…/exec)과 같아야 합니다.
     * 주소가 바뀌면 여기만 고치고 다시 빌드하세요.
     */
    private static final String APP_URL =
            "https://script.google.com/macros/s/AKfycbwc5jucA2BSqYMk-otGBu2zxOqf-5EwHrbsFcs2BdNcGClA3W431mMUQ4rUHsYakti8mA/exec";

    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private boolean showingError = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // 기기에 비밀번호·임시 저장 보관
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);

        // Apps Script 화면은 googleusercontent.com 안쪽 틀에서 돌아가므로 제3자 쿠키를 허용해야 저장이 됩니다.
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isAppHost(uri)) return false;          // 프로그램 화면은 앱 안에서
                openOutside(uri);                           // 그 밖의 링크는 바깥 브라우저로
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                showingError = false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showOfflinePage();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // "엑셀 파일 가져오기"의 파일 선택
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/csv",
                        "application/octet-stream"});
                try {
                    startActivityForResult(Intent.createChooser(pick, "엑셀 파일 선택"), FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "파일을 고를 앱이 없습니다.", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        // 엑셀 저장(내려받기)은 Apps Script 안쪽 틀에서 만들어져 앱이 받을 수 없습니다.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) ->
                Toast.makeText(this, "엑셀 저장은 PC에서 해 주세요.", Toast.LENGTH_LONG).show());

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(APP_URL);
        }
    }

    private static boolean isAppHost(Uri uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) return false;
        if (!scheme.equals("https")) return false;
        return host.equals("script.google.com")
                || host.endsWith(".googleusercontent.com")
                || host.equals("accounts.google.com");
    }

    private void openOutside(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOfflinePage() {
        showingError = true;
        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='font-family:sans-serif;padding:40px 24px;text-align:center;color:#1d2420;background:#f4f2ec'>"
                + "<h2 style='font-size:20px'>연결할 수 없습니다</h2>"
                + "<p style='color:#68716b'>인터넷 연결을 확인한 뒤 다시 시도해 주세요.<br>입력하던 주문은 연결되면 다시 올라갑니다.</p>"
                + "<button onclick=\"location.href='" + APP_URL + "'\" "
                + "style='font-size:16px;padding:12px 24px;border:0;border-radius:8px;background:#9a6b12;color:#fff'>다시 시도</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private void goBackOrExit() {
        if (!showingError && webView.canGoBack()) webView.goBack();
        else finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        goBackOrExit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileCallback == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            result = new Uri[]{data.getData()};
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.setVisibility(View.GONE);
            webView.destroy();
        }
        super.onDestroy();
    }
}
