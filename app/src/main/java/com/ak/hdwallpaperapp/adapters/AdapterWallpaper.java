package com.ak.hdwallpaperapp.adapters;

import static com.ak.hdwallpaperapp.Config.NATIVE_AD_WALLPAPER_LIST;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.MOPUB;
import static com.solodroid.ads.sdk.util.Constant.UNITY;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.ak.hdwallpaperapp.Config;
import com.ak.hdwallpaperapp.R;
import com.ak.hdwallpaperapp.database.prefs.AdsPref;
import com.ak.hdwallpaperapp.database.prefs.SharedPref;
import com.ak.hdwallpaperapp.models.Post;
import com.ak.hdwallpaperapp.utils.AdsManager;
import com.ak.hdwallpaperapp.utils.Constant;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;

public class AdapterWallpaper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_PROG = 0;
    private final int VIEW_ITEM = 1;
    private final int VIEW_AD = 2;
    private List<Post> items;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;
    boolean scrolling = false;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;

    public interface OnItemClickListener {
        void onItemClick(View view, Post obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.onItemClickListener = mItemClickListener;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterWallpaper(Context context, RecyclerView view, List<Post> items) {
        this.items = items;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
        this.adsManager = new AdsManager((Activity) context);
        lastItemViewDetector(view);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrolling = false;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView wallpaperName;
        public ImageView wallpaperImage;
        public ProgressBar progressBar;
        public FrameLayout lytParent;

        public OriginalViewHolder(View v) {
            super(v);
            wallpaperName = v.findViewById(R.id.wallpaperName);
            wallpaperImage = v.findViewById(R.id.wallpaperImage);
            progressBar = v.findViewById(R.id.progressBar);
            lytParent = v.findViewById(R.id.lytParent);
        }
    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.progressBar);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            if (Config.WALLPAPER_GRID_STYLE == 1) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_square, parent, false);
                vh = new OriginalViewHolder(v);
            } else if (Config.WALLPAPER_GRID_STYLE == 2) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_dynamic, parent, false);
                vh = new OriginalViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_rectangle, parent, false);
                vh = new OriginalViewHolder(v);
            }
        } else if (viewType == VIEW_AD) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_medium, parent, false);
            vh = new NativeAdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_load_more, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Post p = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.wallpaperName.setText(p.title);
            if (!sharedPref.getDisplayWallpaperName().equals("true")) {
                vItem.wallpaperName.setVisibility(View.GONE);
            }

            Document htmlData = Jsoup.parse(p.content);
            Elements elements = htmlData.select("img");
            if (elements.hasAttr("src")) {
                if (Config.WALLPAPER_GRID_STYLE == 2) {
                    Glide.with(context)
                            .load(elements.get(0).attr("src").replace(" ", "%20"))
                            .thumbnail(0.3f)
                            .apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.bg_button_transparent)
                            .addListener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    vItem.progressBar.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    vItem.progressBar.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(vItem.wallpaperImage);
                } else {
                    Glide.with(context)
                            .load(elements.get(0).attr("src").replace(" ", "%20"))
                            .thumbnail(0.3f)
                            .apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.bg_button_transparent)
                            .centerCrop()
                            .addListener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    vItem.progressBar.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    vItem.progressBar.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(vItem.wallpaperImage);
                }
            }

            vItem.lytParent.setOnClickListener(view -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(view, p, position);
                }
            });

        } else if (holder instanceof NativeAdViewHolder) {

            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;
            final SharedPref sharedPref = new SharedPref(context);

            vItem.loadNativeAd(context,
                    adsPref.getAdStatus(),
                    NATIVE_AD_WALLPAPER_LIST,
                    adsPref.getAdType(),
                    adsPref.getBackupAds(),
                    adsPref.getAdMobNativeId(),
                    sharedPref.getIsDarkTheme(),
                    Config.LEGACY_GDPR
            );

            vItem.setNativeAdPadding(
                    context.getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper),
                    context.getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper),
                    context.getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper),
                    context.getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper)
            );

        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }

        if (getItemViewType(position) == VIEW_PROG || getItemViewType(position) == VIEW_AD) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        } else {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(false);
        }

    }

    public void insertData(List<Post> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void insertDataWithNativeAd(List<Post> items) {
        setLoaded();
        int positionStart = getItemCount();

        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && NATIVE_AD_WALLPAPER_LIST != 0) {
            if (sharedPref.getWallpaperColumns() == 3) {
                if (items.size() >= Constant.NATIVE_AD_INDEX_3_COLUMNS)
                    items.add(Constant.NATIVE_AD_INDEX_3_COLUMNS, new Post());
                Log.d("INSERT_DATA", "3 columns");
            } else {
                if (items.size() >= Constant.NATIVE_AD_INDEX_2_COLUMNS)
                    items.add(Constant.NATIVE_AD_INDEX_2_COLUMNS, new Post());
                Log.d("INSERT_DATA", "2 columns");
            }
        }

        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.items.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Post post = items.get(position);
        if (post != null) {
            if (post.title == null) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_PROG;
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {
        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null));
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        //int current_page = getItemCount() / 10;
                        //onLoadMoreListener.onLoadMore(current_page);
                        if (sharedPref.getWallpaperColumns() == 3) {
                            if (adsPref.getAdStatus().equals(AD_STATUS_ON) && NATIVE_AD_WALLPAPER_LIST != 0) {
                                switch (adsPref.getAdType()) {
                                    case UNITY:
                                    case APPLOVIN:
                                    case MOPUB: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_3_COLUMNS);
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                    default: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_3_COLUMNS + 1); //posts per page plus 1 Ad
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                }
                            } else {
                                int current_page = getItemCount() / (Constant.LOAD_MORE_3_COLUMNS);
                                onLoadMoreListener.onLoadMore(current_page);
                            }
                        } else {
                            if (adsPref.getAdStatus().equals(AD_STATUS_ON) && NATIVE_AD_WALLPAPER_LIST != 0) {
                                switch (adsPref.getAdType()) {
                                    case UNITY:
                                    case APPLOVIN:
                                    case MOPUB: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_2_COLUMNS);
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                    default: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_2_COLUMNS + 1); //posts per page plus 1 Ad
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                }
                            } else {
                                int current_page = getItemCount() / (Constant.LOAD_MORE_2_COLUMNS);
                                onLoadMoreListener.onLoadMore(current_page);
                            }
                        }
                        loading = true;
                    }
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

    private int getLastVisibleItem(int[] into) {
        int lastIdx = into[0];
        for (int i : into) {
            if (lastIdx < i) lastIdx = i;
        }
        return lastIdx;
    }

}