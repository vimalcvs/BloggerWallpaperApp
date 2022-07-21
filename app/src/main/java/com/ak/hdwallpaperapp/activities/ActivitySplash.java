package com.ak.hdwallpaperapp.activities;

import static com.ak.hdwallpaperapp.Config.GOOGLE_DRIVE_JSON_FILE_ID;
import static com.ak.hdwallpaperapp.Config.JSON_FILE_HOST_TYPE;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ak.hdwallpaperapp.Config;
import com.ak.hdwallpaperapp.R;
import com.ak.hdwallpaperapp.callbacks.CallbackConfig;
import com.ak.hdwallpaperapp.callbacks.CallbackLabel;
import com.ak.hdwallpaperapp.database.prefs.AdsPref;
import com.ak.hdwallpaperapp.database.prefs.SharedPref;
import com.ak.hdwallpaperapp.database.sqlite.DbLabel;
import com.ak.hdwallpaperapp.models.Ads;
import com.ak.hdwallpaperapp.models.App;
import com.ak.hdwallpaperapp.models.Blog;
import com.ak.hdwallpaperapp.models.Category;
import com.ak.hdwallpaperapp.rests.RestAdapter;
import com.ak.hdwallpaperapp.utils.AdsManager;
import com.ak.hdwallpaperapp.utils.Constant;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.solodroid.ads.sdk.format.AppOpenAd;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "ActivitySplash";
    Call<CallbackConfig> callbackConfigCall = null;
    AppOpenAd appOpenAdManager;
    private boolean isAdShown = false;
    private boolean isAdDismissed = false;
    private boolean isLoadCompleted = false;
    AdsManager adsManager;
    SharedPref sharedPref;
    AdsPref adsPref;
    App app;
    Blog blog;
    Ads ads;
    List<Category> labels = new ArrayList<>();
    DbLabel dbLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        dbLabel = new DbLabel(this);
        sharedPref = new SharedPref(this);
        adsManager = new AdsManager(this);
        adsPref = new AdsPref(this);

        requestConfig();

    }

    private void requestConfig() {
        if (JSON_FILE_HOST_TYPE == 0) {
            callbackConfigCall = RestAdapter.createApiGoogleDrive().getDriveJsonFileId(GOOGLE_DRIVE_JSON_FILE_ID);
            Log.d(TAG, "Request API from Google Drive");
        } else {
            callbackConfigCall = RestAdapter.createApiJsonUrl().getJsonUrl(Config.JSON_URL);
            Log.d(TAG, "Request API from Json Url");
        }
        callbackConfigCall.enqueue(new Callback<CallbackConfig>() {
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                displayApiResults(resp);
            }

            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable th) {
                Log.e(TAG, "initialize failed");
                onSplashFinished();
            }
        });
    }

    private void displayApiResults(CallbackConfig resp) {

        if (resp != null) {
            app = resp.app;
            ads = resp.ads;
            blog = resp.blog;
            labels = resp.labels;

            if (app.status.equals("1")) {
                sharedPref.saveBlogCredentials(blog.blogger_id, blog.api_key);
                adsManager.saveConfig(sharedPref, app);
                adsManager.saveAds(adsPref, ads);
                requestLabel();
                Log.d(TAG, "App status is live");
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.redirect_url)));
                finish();
                Log.d(TAG, "App status is suspended");
            }
            Log.d(TAG, "initialize success");
        } else {
            Log.d(TAG, "initialize failed");
            onSplashFinished();
        }

    }

    private void requestLabel() {
        Call<CallbackLabel> callbackLabelCall = RestAdapter.createApiCategory(sharedPref.getBloggerId()).getLabel();
        callbackLabelCall.enqueue(new Callback<CallbackLabel>() {
            public void onResponse(Call<CallbackLabel> call, Response<CallbackLabel> response) {
                CallbackLabel resp = response.body();
                if (resp == null) {
                    onSplashFinished();
                    return;
                }

                dbLabel.truncateTableCategory(DbLabel.TABLE_LABEL);
                if (sharedPref.getCustomLabelList().equals("true")) {
                    dbLabel.addListCategory(labels, DbLabel.TABLE_LABEL);
                } else {
                    dbLabel.addListCategory(resp.feed.category, DbLabel.TABLE_LABEL);
                }

                onSplashFinished();
                Log.d(TAG, "Success initialize label with count " + resp.feed.category.size() + " items");
            }

            public void onFailure(Call<CallbackLabel> call, Throwable th) {
                Log.e("onFailure", th.getMessage());
                if (!call.isCanceled()) {
                    onSplashFinished();
                }
            }
        });
    }

    private void onSplashFinished() {
        if (adsPref.getAdType().equals(ADMOB) && adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                launchAppOpenAd();
            } else {
                launchMainScreen();
            }
        } else {
            launchMainScreen();
        }
    }

    private void launchMainScreen() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, Constant.SPLASH_DURATION);
    }

    private void launchAppOpenAd() {
        appOpenAdManager = ((MyApplication) getApplication()).getAppOpenAdManager();
        loadResources();
        appOpenAdManager.showAdIfAvailable(adsPref.getAdMobAppOpenAdId(), new FullScreenContentCallback() {

            @Override
            public void onAdShowedFullScreenContent() {
                isAdShown = true;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                isAdDismissed = true;
                if (isLoadCompleted) {
                    launchMainScreen();
                    Log.d(TAG, "isLoadCompleted and launch main screen...");
                } else {
                    Log.d(TAG, "Waiting resources to be loaded...");
                }
            }
        });
    }

    private void loadResources() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isLoadCompleted = true;
            if (isAdShown) {
                if (isAdDismissed) {
                    launchMainScreen();
                    Log.d(TAG, "isAdDismissed and launch main screen...");
                } else {
                    Log.d(TAG, "Waiting for ad to be dismissed...");
                }
                Log.d(TAG, "Ad shown...");
            } else {
                launchMainScreen();
                Log.d(TAG, "Ad not shown...");
            }
        }, Constant.SPLASH_DURATION);
    }

}
