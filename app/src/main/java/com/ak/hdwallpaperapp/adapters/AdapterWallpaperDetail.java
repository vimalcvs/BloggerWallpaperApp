package com.ak.hdwallpaperapp.adapters;

import static com.ak.hdwallpaperapp.Config.NATIVE_AD_WALLPAPER_LIST;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.ak.hdwallpaperapp.R;
import com.ak.hdwallpaperapp.activities.ActivityWallpaperDetail;
import com.ak.hdwallpaperapp.database.prefs.AdsPref;
import com.ak.hdwallpaperapp.database.prefs.SharedPref;
import com.ak.hdwallpaperapp.models.Post;
import com.ak.hdwallpaperapp.utils.AdsManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.List;

public class AdapterWallpaperDetail extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_ITEM = 1;
    private final int VIEW_AD = 2;
    private List<Post> items;
    private Context context;
    private OnItemClickListener mOnItemClickListener;
    private boolean loading;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;

    public interface OnItemClickListener {
        void onItemClick(View view, Post obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterWallpaperDetail(Context context, List<Post> items) {
        this.items = items;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
        this.adsManager = new AdsManager((Activity) context);
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public PhotoView wallpaperImage;
        public ProgressBar progressBar;

        public OriginalViewHolder(View v) {
            super(v);
            wallpaperImage = v.findViewById(R.id.wallpaperImage);
            progressBar = v.findViewById(R.id.progressBar);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_AD) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_large, parent, false);
            vh = new NativeAdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_detail, parent, false);
            vh = new OriginalViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Post p = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;
            Document htmlData = Jsoup.parse(p.content);
            Elements elements = htmlData.select("img");
            if (elements.hasAttr("src")) {

                vItem.wallpaperImage.setOnClickListener(v -> ((ActivityWallpaperDetail) context).showFullScreen());

                if (sharedPref.isCenterCropWallpaper().equals("true")) {
                    vItem.wallpaperImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }

                Glide.with(context)
                        .load(elements.get(0).attr("src").replace(" ", "%20"))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.bg_button_transparent)
                        .thumbnail(0.3f)
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

        } else if (holder instanceof NativeAdViewHolder) {
            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;
            final AdsPref adsPref = new AdsPref(context);
            final SharedPref sharedPref = new SharedPref(context);
            vItem.loadNativeAd(context,
                    adsPref.getAdStatus(),
                    NATIVE_AD_WALLPAPER_LIST,
                    adsPref.getAdType(),
                    adsPref.getBackupAds(),
                    adsPref.getAdMobNativeId(),
                    sharedPref.getIsDarkTheme(),
                    true
            );

            vItem.setNativeAdPadding(
                    context.getResources().getDimensionPixelOffset(R.dimen.spacing_middle),
                    context.getResources().getDimensionPixelOffset(R.dimen.spacing_middle),
                    context.getResources().getDimensionPixelOffset(R.dimen.spacing_middle),
                    context.getResources().getDimensionPixelOffset(R.dimen.spacing_middle)
            );

            vItem.itemView.findViewById(R.id.progress_bar_ad).setVisibility(View.GONE);
        }
    }

    public void insertData(List<Post> items) {
        setLoaded();
        int positionStart = getItemCount();
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
            return VIEW_ITEM;
        }
    }

}