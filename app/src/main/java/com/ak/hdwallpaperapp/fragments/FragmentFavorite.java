package com.ak.hdwallpaperapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.ak.hdwallpaperapp.R;
import com.ak.hdwallpaperapp.activities.ActivityWallpaperDetail;
import com.ak.hdwallpaperapp.activities.MainActivity;
import com.ak.hdwallpaperapp.adapters.AdapterWallpaper;
import com.ak.hdwallpaperapp.database.prefs.SharedPref;
import com.ak.hdwallpaperapp.database.sqlite.DbFavorite;
import com.ak.hdwallpaperapp.models.Post;
import com.ak.hdwallpaperapp.utils.Constant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FragmentFavorite extends Fragment {

    private List<Post> posts = new ArrayList<>();
    private View rootView;
    LinearLayoutCompat lytNoFavorite;
    private RecyclerView recyclerView;
    DbFavorite dbFavorite;
    SharedPref sharedPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_favorite, container, false);
        sharedPref = new SharedPref(getActivity());
        recyclerView = rootView.findViewById(R.id.recyclerView);
        lytNoFavorite = rootView.findViewById(R.id.lyt_no_favorite);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));

        loadDataFromDatabase();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataFromDatabase();
    }

    public void loadDataFromDatabase() {
        dbFavorite = new DbFavorite(getActivity());
        posts = dbFavorite.getAllData();

        //set data and list adapter
        AdapterWallpaper adapterWallpaper = new AdapterWallpaper(getActivity(), recyclerView, posts);
        recyclerView.setAdapter(adapterWallpaper);

        showNoItemView(posts.size() == 0);

        // on item list clicked
        adapterWallpaper.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) posts);
            intent.putExtra(Constant.BUNDLE, bundle);
            startActivity(intent);
            if (getActivity() != null)
            ((MainActivity) getActivity()).showInterstitialAd();
        });

    }

    private void showNoItemView(boolean show) {
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.no_favorite_found);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lytNoFavorite.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lytNoFavorite.setVisibility(View.GONE);
        }
    }

}
