package com.app.bloggerwallpaperapp.activities;

import static com.app.bloggerwallpaperapp.Config.BANNER_WALLPAPER_DETAIL;
import static com.app.bloggerwallpaperapp.Config.INTERSTITIAL_WALLPAPER_DETAIL;
import static com.app.bloggerwallpaperapp.utils.Constant.BOTH;
import static com.app.bloggerwallpaperapp.utils.Constant.HOME_SCREEN;
import static com.app.bloggerwallpaperapp.utils.Constant.LOCK_SCREEN;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.app.bloggerwallpaperapp.R;
import com.app.bloggerwallpaperapp.adapters.AdapterWallpaperDetail;
import com.app.bloggerwallpaperapp.database.prefs.AdsPref;
import com.app.bloggerwallpaperapp.database.prefs.SharedPref;
import com.app.bloggerwallpaperapp.database.sqlite.DbFavorite;
import com.app.bloggerwallpaperapp.models.Post;
import com.app.bloggerwallpaperapp.rests.ApiInterface;
import com.app.bloggerwallpaperapp.rests.RestAdapter;
import com.app.bloggerwallpaperapp.utils.AdsManager;
import com.app.bloggerwallpaperapp.utils.Constant;
import com.app.bloggerwallpaperapp.utils.Tools;
import com.app.bloggerwallpaperapp.utils.WallpaperHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityWallpaperDetail extends AppCompatActivity {

    public static final String TAG = "ActivityViewPager2";
    ViewPager2 viewPager2;
    int position;
    List<Post> wallpapers = new ArrayList<>();
    AdapterWallpaperDetail adapterRecent;
    Toolbar toolbar;
    ActionBar actionBar;
    private String single_choice_selected;
    CoordinatorLayout parentView;
    SharedPref sharedPref;
    DbFavorite dbFavorite;
    AdsPref adsPref;
    boolean flag = true;
    LinearLayout lyt_bottom;
    RelativeLayout bg_shadow_top;
    RelativeLayout bg_shadow_bottom;
    AdsManager adsManager;
    WallpaperHelper wallpaperHelper;
    ProgressDialog progressDialog;
    String wallpaperUrl = "";
    ImageView img_favorite;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        dbFavorite = new DbFavorite(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        if (BANNER_WALLPAPER_DETAIL != 0) {
            Tools.transparentStatusBar(this);
            if (sharedPref.getIsDarkTheme()) {
                Tools.darkNavigation(this);
            }
        } else {
            Tools.transparentStatusBarNavigation(this);
        }
        setContentView(R.layout.activity_viewpager2);
        Tools.setLayoutDirection(this);
        progressDialog = new ProgressDialog(this);
        wallpaperHelper = new WallpaperHelper(this);

        parentView = findViewById(R.id.coordinatorLayout);
        lyt_bottom = findViewById(R.id.lyt_bottom);
        bg_shadow_top = findViewById(R.id.bg_shadow_top);
        bg_shadow_bottom = findViewById(R.id.bg_shadow_bottom);
        viewPager2 = findViewById(R.id.viewPager2);
        img_favorite = findViewById(R.id.img_favorite);

        position = getIntent().getIntExtra(Constant.POSITION, 0);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra(Constant.BUNDLE);
        wallpapers = (List<Post>) bundle.getSerializable(Constant.ARRAY_LIST);

        setupToolbar();
        loadView(wallpapers, position);
        setupViewPager(wallpapers);

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_WALLPAPER_DETAIL);
        adsManager.loadInterstitialAd(INTERSTITIAL_WALLPAPER_DETAIL, 1);

    }

    public void setupViewPager(final List<Post> wallpapers) {
        viewPager2 = findViewById(R.id.viewPager2);
        adapterRecent = new AdapterWallpaperDetail(this, wallpapers);
        viewPager2.setAdapter(adapterRecent);
        viewPager2.setOffscreenPageLimit(wallpapers.size());
        viewPager2.setCurrentItem(position, false);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                loadView(wallpapers, position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

    }

    public void loadView(final List<Post> wallpapers, int position) {

        if (wallpapers.get(position).title != null) {

            Document htmlData = Jsoup.parse(wallpapers.get(position).content);
            Elements elements = htmlData.select("img");
            if (elements.hasAttr("src")) {
                wallpaperUrl = elements.get(0).attr("src").replace(" ", "%20");
            }

            TextView title_toolbar = findViewById(R.id.title_toolbar);

            if (!sharedPref.getDisplayWallpaperName().equals("true")) {
                title_toolbar.setVisibility(View.GONE);
            }

            title_toolbar.setText(wallpapers.get(position).title);

            findViewById(R.id.btn_save).setOnClickListener(view -> {
                if (!verifyPermissions()) {
                    return;
                }
                Log.d(TAG, "Url : " + wallpaperUrl);
                String wallpaperName = wallpapers.get(position).title.toLowerCase().replace(" ", "_") + "_" + wallpapers.get(position).id;
                wallpaperHelper.downloadWallpaper(progressDialog, adsManager, wallpaperName, wallpaperUrl);
            });

            findViewById(R.id.btn_share).setOnClickListener(view -> {
                if (!verifyPermissions()) {
                    return;
                }
                String wallpaperName = wallpapers.get(position).title.toLowerCase().replace(" ", "_") + "_" + wallpapers.get(position).id;
                wallpaperHelper.shareWallpaper(progressDialog, wallpaperName, wallpaperUrl);
            });

            findViewById(R.id.btn_set_wallpaper).setOnClickListener(view -> {
                if (!verifyPermissions()) {
                    return;
                }
                loadFile(wallpapers, progressDialog, position, wallpaperUrl);
            });

            findViewById(R.id.btn_favorite).setOnClickListener(view -> {
                List<Post> data = dbFavorite.getFavRow(wallpapers.get(position).id);
                if (data.size() == 0) {
                    dbFavorite.AddToFavorite(new Post(wallpapers.get(position).id, wallpapers.get(position).title, wallpapers.get(position).labels, wallpapers.get(position).content, wallpapers.get(position).published));
                    Snackbar.make(parentView, R.string.msg_favorite_added, Snackbar.LENGTH_SHORT).show();
                    img_favorite.setImageResource(R.drawable.ic_menu_favorite);
                } else {
                    if (data.get(0).getId().equals(wallpapers.get(position).id)) {
                        dbFavorite.RemoveFav(new Post(wallpapers.get(position).id));
                        Snackbar.make(parentView, R.string.msg_favorite_removed, Snackbar.LENGTH_SHORT).show();
                        img_favorite.setImageResource(R.drawable.ic_menu_favorite_outline);
                    }
                }
            });
            favToggle(wallpapers, position);

            lyt_bottom.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            fullScreenMode(false);
            showShadow(true);
        } else {
            fullScreenMode(false);
            lyt_bottom.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            if (!sharedPref.getIsDarkTheme()) {
                Tools.darkNavigationStatusBar(ActivityWallpaperDetail.this);
            }
            showShadow(false);
        }

    }

    public void favToggle(final List<Post> wallpapers, int position) {
        List<Post> data = dbFavorite.getFavRow(wallpapers.get(position).id);
        if (data.size() == 0) {
            img_favorite.setImageResource(R.drawable.ic_menu_favorite_outline);
        } else {
            if (data.get(0).getId().equals(wallpapers.get(position).id)) {
                img_favorite.setImageResource(R.drawable.ic_menu_favorite);
            }
        }
    }

    private void loadFile(final List<Post> wallpapers, ProgressDialog progressDialog, int position, String fileUrl) {

        progressDialog.setMessage(getString(R.string.msg_loading_wallpaper));
        progressDialog.setCancelable(false);
        progressDialog.show();

        ApiInterface apiInterface = RestAdapter.createDownloadApi();
        Call<ResponseBody> call = apiInterface.downloadFileWithDynamicUrl(fileUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Got the body for the file");
                    if (response.body() != null) {
                        try {
                            InputStream inputStream = new BufferedInputStream(response.body().byteStream());
                            String mimeType = URLConnection.guessContentTypeFromStream(inputStream);

                            if (mimeType.equals("image/gif") || mimeType.equals("image/GIF")) {
                                String imageName = wallpapers.get(position).title.toLowerCase().replace(" ", "_") + "_" + wallpapers.get(position).id;
                                wallpaperHelper.setGif(parentView, progressDialog, imageName, wallpaperUrl);
                            } else {
                                if (Build.VERSION.SDK_INT >= 24) {
                                    dialogOptionSetWallpaper(wallpaperUrl);
                                } else {
                                    wallpaperHelper.setWallpaper(parentView, progressDialog, adsManager, wallpaperUrl);
                                }
                            }
                            Log.d(TAG, "mime type : " + mimeType);
                        } catch (IOException e) {
                            e.printStackTrace();
                            progressDialog.dismiss();
                        }
                    } else {
                        progressDialog.dismiss();
                    }
                } else {
                    Log.d(TAG, "Connection failed " + response.errorBody());
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, t.getMessage());
                progressDialog.dismiss();
            }
        });
    }

    private void showShadow(boolean show) {
        if (show) {
            bg_shadow_top.setVisibility(View.VISIBLE);
            bg_shadow_bottom.setVisibility(View.VISIBLE);
        } else {
            bg_shadow_top.setVisibility(View.GONE);
            bg_shadow_bottom.setVisibility(View.GONE);
        }
    }

    public void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void dialogOptionSetWallpaper(String imageURL) {
        String[] items = getResources().getStringArray(R.array.dialog_set_wallpaper);
        single_choice_selected = items[0];
        int itemSelected = 0;
        new AlertDialog.Builder(ActivityWallpaperDetail.this)
                .setTitle(R.string.dialog_set_title)
                .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {

                    progressDialog.setMessage(getString(R.string.msg_preparing_wallpaper));
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> Glide.with(this)
                            .load(imageURL.replace(" ", "%20"))
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                                    if (single_choice_selected.equals(getResources().getString(R.string.set_home_screen))) {
                                        wallpaperHelper.setWallpaper(parentView, progressDialog, adsManager, bitmap, HOME_SCREEN);
                                        progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_lock_screen))) {
                                        wallpaperHelper.setWallpaper(parentView, progressDialog, adsManager, bitmap, LOCK_SCREEN);
                                        progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_both))) {
                                        wallpaperHelper.setWallpaper(parentView, progressDialog, adsManager, bitmap, BOTH);
                                        progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_crop))) {
                                        Intent intent = new Intent(getApplicationContext(), ActivityCropWallpaper.class);
                                        intent.putExtra("image_url", wallpaperUrl);
                                        startActivity(intent);
                                        progressDialog.dismiss();
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_with))) {
                                        wallpaperHelper.setWallpaperFromOtherApp(wallpaperUrl);
                                        progressDialog.dismiss();
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                    progressDialog.dismiss();
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    Snackbar.make(parentView, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                }
                            }), Constant.DELAY_SET);

                })
                .setNegativeButton(R.string.dialog_option_cancel, (dialog, which) -> progressDialog.dismiss())
                .setCancelable(false)
                .show();
    }

    public Boolean verifyPermissions() {
        int permissionExternalMemory = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionExternalMemory != PackageManager.PERMISSION_GRANTED) {
            String[] STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, STORAGE_PERMISSIONS, 1);
            return false;
        }
        return true;
    }

    public void showFullScreen() {
        if (flag) {
            fullScreenMode(true);
            flag = false;
        } else {
            fullScreenMode(false);
            flag = true;
        }
    }

    public void fullScreenMode(boolean on) {
        if (on) {
            toolbar.setVisibility(View.GONE);
            toolbar.animate().translationY(-112);
            lyt_bottom.setVisibility(View.GONE);
            lyt_bottom.animate().translationY(lyt_bottom.getHeight());

            bg_shadow_top.setVisibility(View.GONE);
            bg_shadow_top.animate().translationY(-112);

            bg_shadow_bottom.setVisibility(View.GONE);
            bg_shadow_bottom.animate().translationY(lyt_bottom.getHeight());

            Tools.transparentStatusBarNavigation(this);

            hideSystemUI();

        } else {
            toolbar.setVisibility(View.VISIBLE);
            toolbar.animate().translationY(0);
            lyt_bottom.setVisibility(View.VISIBLE);
            lyt_bottom.animate().translationY(0);

            bg_shadow_top.setVisibility(View.VISIBLE);
            bg_shadow_top.animate().translationY(0);

            bg_shadow_bottom.setVisibility(View.VISIBLE);
            bg_shadow_bottom.animate().translationY(0);

            if (BANNER_WALLPAPER_DETAIL != 0) {
                Tools.transparentStatusBar(this);
            } else {
                Tools.transparentStatusBarNavigation(this);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
