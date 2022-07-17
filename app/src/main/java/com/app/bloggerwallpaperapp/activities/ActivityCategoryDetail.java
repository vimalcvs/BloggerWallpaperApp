package com.app.bloggerwallpaperapp.activities;

import static com.app.bloggerwallpaperapp.Config.BANNER_CATEGORY_DETAIL;
import static com.app.bloggerwallpaperapp.Config.INTERSTITIAL_WALLPAPER_LIST;
import static com.app.bloggerwallpaperapp.utils.Constant.DISPLAY_WALLPAPER_ORDER;
import static com.app.bloggerwallpaperapp.utils.Tools.EXTRA_OBJC;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.bloggerwallpaperapp.R;
import com.app.bloggerwallpaperapp.adapters.AdapterWallpaper;
import com.app.bloggerwallpaperapp.callbacks.CallbackPost;
import com.app.bloggerwallpaperapp.database.prefs.AdsPref;
import com.app.bloggerwallpaperapp.database.prefs.SharedPref;
import com.app.bloggerwallpaperapp.models.Post;
import com.app.bloggerwallpaperapp.rests.RestAdapter;
import com.app.bloggerwallpaperapp.utils.AdsManager;
import com.app.bloggerwallpaperapp.utils.Constant;
import com.app.bloggerwallpaperapp.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCategoryDetail extends AppCompatActivity {

    private static final String TAG = "ActivityCategoryDetail";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdapterWallpaper adapterWallpaper;
    private ShimmerFrameLayout lytShimmer;
    private Call<CallbackPost> callbackCall = null;
    List<Post> items = new ArrayList<>();
    SharedPref sharedPref;
    String category;
    CoordinatorLayout lytParent;
    AdsPref adsPref;
    AdsManager adsManager;
    Tools tools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_category_detail);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        tools = new Tools(this);

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_CATEGORY_DETAIL);
        adsManager.loadInterstitialAd(INTERSTITIAL_WALLPAPER_LIST, adsPref.getInterstitialAdInterval());

        Tools.setNavigation(this, sharedPref);
        sharedPref.resetCategoryDetailToken();

        category = getIntent().getStringExtra(EXTRA_OBJC);

        lytParent = findViewById(R.id.coordinatorLayout);
        recyclerView = findViewById(R.id.recycler_view);
        lytShimmer = findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(this, recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);

        adapterWallpaper.setOnLoadMoreListener(current_page -> {
            if (sharedPref.getCategoryDetailToken() != null) {
                requestAction();
            } else {
                adapterWallpaper.setLoaded();
            }
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterWallpaper.resetListData();
            sharedPref.resetCategoryDetailToken();
            requestAction();
        });

        requestAction();
        setupToolbar();
        initShimmerLayout();

    }

    private void setOnWallpaperClickListener(boolean click) {
        if (click) {
            adapterWallpaper.setOnItemClickListener((view, obj, position) -> {
                Intent intent = new Intent(getApplicationContext(), ActivityWallpaperDetail.class);
                intent.putExtra(Constant.POSITION, position);
                Bundle bundle = new Bundle();
                bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) items);
                intent.putExtra(Constant.BUNDLE, bundle);
                startActivity(intent);
                adsManager.showInterstitialAd();
            });
        } else {
            adapterWallpaper.setOnItemClickListener((view, obj, position) -> {
                //do nothing
            });
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        showNoItemView(false);
        if (sharedPref.getCategoryDetailToken() == null) {
            swipeProgress(true);
            setOnWallpaperClickListener(false);
        } else {
            adapterWallpaper.setLoading();
            setOnWallpaperClickListener(false);
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::requestPostAPI, Constant.DELAY_REFRESH);
    }

    private void requestPostAPI() {
        this.callbackCall = RestAdapter.createApiPosts(sharedPref.getBloggerId()).getCategoryDetail(category, DISPLAY_WALLPAPER_ORDER, sharedPref.getAPIKey(), sharedPref.getCategoryDetailToken());
        this.callbackCall.enqueue(new Callback<CallbackPost>() {
            public void onResponse(Call<CallbackPost> call, Response<CallbackPost> response) {
                CallbackPost resp = response.body();
                if (resp != null) {
                    setOnWallpaperClickListener(true);
                    displayApiResult(resp.items);
                    String token = resp.nextPageToken;
                    if (token != null) {
                        sharedPref.updateCategoryDetailToken(token);
                        Log.d("PAGE_TOKEN", token);
                    } else {
                        sharedPref.resetCategoryDetailToken();
                        Log.d("PAGE_TOKEN", "Last page there is no token");
                    }
                } else {
                    onFailRequest();
                }
            }

            public void onFailure(Call<CallbackPost> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
                if (!call.isCanceled()) {
                    onFailRequest();
                }
            }
        });
    }

    private void displayApiResult(final List<Post> items) {
        adapterWallpaper.insertData(items);
        swipeProgress(false);
        if (items.size() == 0) {
            showNoItemView(true);
        }
    }

    private void onFailRequest() {
        adapterWallpaper.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void showFailedView(boolean flag, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_category_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lytShimmer.stopShimmer();
    }

    public void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        Tools.setupToolbar(this, toolbar, category, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (menuItem.getItemId() == R.id.action_search) {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void initShimmerLayout() {
        ViewStub stub = findViewById(R.id.lytShimmerView);
        if (sharedPref.getWallpaperColumns() == Constant.WALLPAPER_THREE_COLUMNS) {
            stub.setLayoutResource(R.layout.shimmer_wallpaper_3_columns_rectangle);
        } else {
            stub.setLayoutResource(R.layout.shimmer_wallpaper_2_columns_rectangle);
        }
        stub.inflate();
    }

}
