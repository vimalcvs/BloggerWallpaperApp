package com.app.bloggerwallpaperapp.fragments;

import static com.app.bloggerwallpaperapp.utils.Constant.DISPLAY_WALLPAPER_ORDER;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.bloggerwallpaperapp.R;
import com.app.bloggerwallpaperapp.activities.ActivityWallpaperDetail;
import com.app.bloggerwallpaperapp.activities.MainActivity;
import com.app.bloggerwallpaperapp.adapters.AdapterWallpaper;
import com.app.bloggerwallpaperapp.callbacks.CallbackPost;
import com.app.bloggerwallpaperapp.database.prefs.SharedPref;
import com.app.bloggerwallpaperapp.database.sqlite.DbFavorite;
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

public class FragmentWallpaper extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdapterWallpaper adapterWallpaper;
    private ShimmerFrameLayout lytShimmer;
    private Call<CallbackPost> callbackCall = null;
    List<Post> items = new ArrayList<>();
    SharedPref sharedPref;
    AdsManager adsManager;
    DbFavorite dbFavorite;
    Tools tools;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_post, container, false);

        if (getActivity() != null) {
            sharedPref = new SharedPref(getActivity());
            dbFavorite = new DbFavorite(getActivity());
            adsManager = new AdsManager(getActivity());
            tools = new Tools(getActivity());
        }

        recyclerView = rootView.findViewById(R.id.recycler_view);
        lytShimmer = rootView.findViewById(R.id.shimmer_view_container);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(getActivity(), recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
            }
        });

        adapterWallpaper.setOnLoadMoreListener(current_page -> {
            if (sharedPref.getPostToken() != null) {
                requestAction();
            } else {
                adapterWallpaper.setLoaded();
            }
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterWallpaper.resetListData();
            sharedPref.resetPostToken();
            requestAction();
        });

        requestAction();
        initShimmerLayout();

        return rootView;
    }

    private void setOnWallpaperClickListener(boolean click) {
        if (click) {
            adapterWallpaper.setOnItemClickListener((view, obj, position) -> {
                Intent intent = new Intent(getActivity(), ActivityWallpaperDetail.class);
                intent.putExtra(Constant.POSITION, position);
                Bundle bundle = new Bundle();
                bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) items);
                intent.putExtra(Constant.BUNDLE, bundle);
                startActivity(intent);

                if (getActivity() != null)
                    ((MainActivity) getActivity()).showInterstitialAd();
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
        if (sharedPref.getPostToken() == null) {
            swipeProgress(true);
            setOnWallpaperClickListener(false);
        } else {
            adapterWallpaper.setLoading();
            setOnWallpaperClickListener(false);
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::requestPostAPI, Constant.DELAY_REFRESH);
    }

    private void requestPostAPI() {
        if (sharedPref.getWallpaperColumns() == 3) {
            callbackCall = RestAdapter.createApiPosts(sharedPref.getBloggerId()).getPosts(DISPLAY_WALLPAPER_ORDER, sharedPref.getAPIKey(), Constant.LOAD_MORE_3_COLUMNS, sharedPref.getPostToken());
        } else {
            callbackCall = RestAdapter.createApiPosts(sharedPref.getBloggerId()).getPosts(DISPLAY_WALLPAPER_ORDER, sharedPref.getAPIKey(), Constant.LOAD_MORE_2_COLUMNS, sharedPref.getPostToken());
        }
        callbackCall.enqueue(new Callback<CallbackPost>() {
            public void onResponse(@NonNull Call<CallbackPost> call, @NonNull Response<CallbackPost> response) {
                CallbackPost resp = response.body();
                if (resp != null) {
                    setOnWallpaperClickListener(true);
                    displayApiResult(resp.items);
                    String token = resp.nextPageToken;
                    if (token != null) {
                        sharedPref.updatePostToken(token);
                        Log.d("PAGE_TOKEN", token);
                    } else {
                        sharedPref.resetPostToken();
                        Log.d("PAGE_TOKEN", "Last page there is no token");
                    }
                } else {
                    onFailRequest();
                }
            }

            public void onFailure(@NonNull Call<CallbackPost> call, @NonNull Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
                if (!call.isCanceled()) {
                    onFailRequest();
                }
            }
        });
    }

    private void displayApiResult(final List<Post> items) {
        adapterWallpaper.insertDataWithNativeAd(items);
        swipeProgress(false);
        if (items.size() == 0) {
            showNoItemView(true);
        }
    }

    private void onFailRequest() {
        adapterWallpaper.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void showFailedView(boolean flag, String message) {
        View lytFailed = rootView.findViewById(R.id.lyt_failed);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lytFailed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lytFailed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lytNoItem = rootView.findViewById(R.id.lyt_no_item);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.no_category_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lytNoItem.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lytNoItem.setVisibility(View.GONE);
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

    private void initShimmerLayout() {
        ViewStub stub = rootView.findViewById(R.id.lytShimmerView);
        if (sharedPref.getWallpaperColumns() == Constant.WALLPAPER_THREE_COLUMNS) {
            stub.setLayoutResource(R.layout.shimmer_wallpaper_3_columns_rectangle);
        } else {
            stub.setLayoutResource(R.layout.shimmer_wallpaper_2_columns_rectangle);
        }
        stub.inflate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lytShimmer.stopShimmer();
    }

}