package com.ak.hdwallpaperapp.fragments;

import static com.ak.hdwallpaperapp.Config.CATEGORY_COLUMN_COUNT;
import static com.ak.hdwallpaperapp.utils.Tools.EXTRA_OBJC;

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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ak.hdwallpaperapp.Config;
import com.ak.hdwallpaperapp.R;
import com.ak.hdwallpaperapp.activities.ActivityCategoryDetail;
import com.ak.hdwallpaperapp.activities.MainActivity;
import com.ak.hdwallpaperapp.adapters.AdapterCategory;
import com.ak.hdwallpaperapp.callbacks.CallbackLabel;
import com.ak.hdwallpaperapp.database.prefs.SharedPref;
import com.ak.hdwallpaperapp.database.sqlite.DbLabel;
import com.ak.hdwallpaperapp.models.Category;
import com.ak.hdwallpaperapp.models.Feed;
import com.ak.hdwallpaperapp.rests.RestAdapter;
import com.ak.hdwallpaperapp.utils.Constant;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCategory extends Fragment {

    private View rootView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdapterCategory adapterLabel;
    private ShimmerFrameLayout lytShimmer;
    private Call<CallbackLabel> callbackCall = null;
    SharedPref sharedPref;
    DbLabel dbLabel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_category, container, false);

        if (getActivity() != null) {
            sharedPref = new SharedPref(getActivity());
        }

        dbLabel = new DbLabel(getActivity());

        recyclerView = rootView.findViewById(R.id.recycler_view);
        lytShimmer = rootView.findViewById(R.id.shimmer_view_container);

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        

        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), CATEGORY_COLUMN_COUNT));
        adapterLabel = new AdapterCategory(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(adapterLabel);

        loadLabelFromDatabase();

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            adapterLabel.resetListData();
            swipeProgress(true);
            new Handler().postDelayed(this::loadLabelFromDatabase, 1000);
        });

        initShimmerLayout();

        return rootView;
    }

    public void loadLabelFromDatabase() {

        swipeProgress(false);
        List<Category> categories = dbLabel.getAllCategory(DbLabel.TABLE_LABEL);
        adapterLabel.setListData(categories);

        if (categories.size() == 0) {
            showNoItemView(true);
        }

        // on item list clicked
        adapterLabel.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityCategoryDetail.class);
            intent.putExtra(EXTRA_OBJC, obj.term);
            startActivity(intent);

            if (getActivity() != null) {
                ((MainActivity) getActivity()).showInterstitialAd();
            }
        });

    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        showNoItemView(false);
        new Handler(Looper.getMainLooper()).postDelayed(this::requestAPI, Constant.DELAY_REFRESH);
    }

    private void requestAPI() {
        this.callbackCall = RestAdapter.createApiCategory(sharedPref.getBloggerId()).getLabel();
        this.callbackCall.enqueue(new Callback<CallbackLabel>() {
            public void onResponse(Call<CallbackLabel> call, Response<CallbackLabel> response) {
                CallbackLabel resp = response.body();
                if (resp == null) {
                    onFailRequest();
                    return;
                }
                displayAllData(resp);
                swipeProgress(false);
                recyclerView.setVisibility(View.VISIBLE);
            }

            public void onFailure(Call<CallbackLabel> call, Throwable th) {
                Log.e("onFailure", th.getMessage());
                if (!call.isCanceled()) {
                    onFailRequest();
                }
            }
        });
    }

    private void displayAllData(CallbackLabel resp) {
        displayData(resp.feed);
    }

    public void displayData(final Feed feed) {

        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), CATEGORY_COLUMN_COUNT));
        adapterLabel = new AdapterCategory(getActivity(), feed.category);

        recyclerView.setAdapter(adapterLabel);
        adapterLabel.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityCategoryDetail.class);
            intent.putExtra(EXTRA_OBJC, obj.term);
            startActivity(intent);

            if (getActivity() != null) {
                ((MainActivity) getActivity()).showInterstitialAd();
            }
        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        showFailedView(true, getString(R.string.failed_text));
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
            swipeRefreshLayout.setRefreshing(false);
            recyclerView.setVisibility(View.VISIBLE);
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(true);
            recyclerView.setVisibility(View.GONE);
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

    private void initShimmerLayout() {
        ViewStub stub = rootView.findViewById(R.id.lytShimmerView);
        if (Config.CATEGORY_COLUMN_COUNT == 2) {
            stub.setLayoutResource(R.layout.shimmer_category_grid);
        } else {
            stub.setLayoutResource(R.layout.shimmer_category_list);
        }
        stub.inflate();
    }

}
