package com.app.bloggerwallpaperapp.utils;

import static com.app.bloggerwallpaperapp.utils.Constant.DOWNLOAD;
import static com.app.bloggerwallpaperapp.utils.Constant.PAGER_NUMBER;
import static com.app.bloggerwallpaperapp.utils.Constant.SET_GIF;
import static com.app.bloggerwallpaperapp.utils.Constant.SET_WITH;
import static com.app.bloggerwallpaperapp.utils.Constant.SHARE;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.app.bloggerwallpaperapp.BuildConfig;
import com.app.bloggerwallpaperapp.Config;
import com.app.bloggerwallpaperapp.R;
import com.app.bloggerwallpaperapp.activities.ActivityNotificationDetail;
import com.app.bloggerwallpaperapp.activities.ActivityWebView;
import com.app.bloggerwallpaperapp.database.prefs.SharedPref;
import com.app.bloggerwallpaperapp.database.sqlite.DbFavorite;
import com.app.bloggerwallpaperapp.services.SetGIFAsWallpaperService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@SuppressWarnings("deprecation")
public class Tools {

    private Activity activity;
    MenuItem prevMenuItem;
    SharedPref sharedPref;
    public static final String EXTRA_OBJC = "key.EXTRA_OBJC";
    DbFavorite dbFavorite;
    private BottomSheetDialog mBottomSheetDialog;

    public Tools(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
    }

    public static void getTheme(Activity activity) {
        SharedPref sharedPref = new SharedPref(activity);
        if (sharedPref.getIsDarkTheme()) {
            activity.setTheme(R.style.AppDarkTheme);
        } else {
            activity.setTheme(R.style.AppTheme);
        }
    }

    public static void setupToolbar(AppCompatActivity activity, Toolbar toolbar, String title, boolean backButton) {
        SharedPref sharedPref = new SharedPref(activity);
        activity.setSupportActionBar(toolbar);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorPrimary));
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(backButton);
            activity.getSupportActionBar().setHomeButtonEnabled(backButton);
            activity.getSupportActionBar().setTitle(title);
        }
    }

    public static void darkToolbar(Activity activity, Toolbar toolbar) {
        toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorToolbarDark));
    }

    public static void transparentStatusBar(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setWindowFlag(activity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        //activity.getWindow().setNavigationBarColor(Color.BLACK);
        activity.getWindow().setNavigationBarColor(activity.getResources().getColor(R.color.black));
    }

    public static void darkNavigationStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getWindow().setNavigationBarColor(activity.getResources().getColor(R.color.colorToolbarDark));
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    public void setupViewPager(AppCompatActivity activity, ViewPager viewPager, BottomNavigationView navigation, Toolbar toolbar, SharedPref sharedPref) {
        viewPager.setVisibility(View.VISIBLE);
        viewPager.setAdapter(new NavigationAdapter.BottomNavigationAdapter(activity.getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(PAGER_NUMBER);
        navigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_recent) {
                viewPager.setCurrentItem(0);
            } else if (itemId == R.id.navigation_category) {
                viewPager.setCurrentItem(1);
            } else if (itemId == R.id.navigation_favorite) {
                viewPager.setCurrentItem(2);
            } else {
                viewPager.setCurrentItem(0);
            }
            return false;
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    navigation.getMenu().getItem(0).setChecked(false);
                }
                navigation.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(position);

                int currentItem = viewPager.getCurrentItem();
                if (currentItem == 0) {
                    toolbar.setTitle(activity.getResources().getString(R.string.app_name));
                } else if (currentItem == 1) {
                    toolbar.setTitle(activity.getResources().getString(R.string.title_nav_category));
                } else if (currentItem == 2) {
                    toolbar.setTitle(activity.getResources().getString(R.string.title_nav_favorite));
                } else {
                    toolbar.setTitle(activity.getResources().getString(R.string.app_name));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void setupViewPagerRTL(AppCompatActivity activity, RtlViewPager viewPager, BottomNavigationView navigation, Toolbar toolbar, SharedPref sharedPref) {
        viewPager.setVisibility(View.VISIBLE);
        viewPager.setAdapter(new NavigationAdapter.BottomNavigationAdapter(activity.getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(PAGER_NUMBER);
        navigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_recent) {
                viewPager.setCurrentItem(0);
            } else if (itemId == R.id.navigation_category) {
                viewPager.setCurrentItem(1);
            } else if (itemId == R.id.navigation_favorite) {
                viewPager.setCurrentItem(2);
            } else {
                viewPager.setCurrentItem(0);
            }
            return false;
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    navigation.getMenu().getItem(0).setChecked(false);
                }
                navigation.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(position);

                int currentItem = viewPager.getCurrentItem();
                if (currentItem == 0) {
                    toolbar.setTitle(activity.getResources().getString(R.string.app_name));
                } else if (currentItem == 1) {
                    toolbar.setTitle(activity.getResources().getString(R.string.title_nav_category));
                } else if (currentItem == 2) {
                    toolbar.setTitle(activity.getResources().getString(R.string.title_nav_favorite));
                } else {
                    toolbar.setTitle(activity.getResources().getString(R.string.app_name));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public static void openAssetActivity(Context context, String title, String fileName) {
        Intent intent = new Intent(context, ActivityWebView.class);
        intent.putExtra("title", title);
        intent.putExtra("file_name", fileName);
        context.startActivity(intent);
    }

    public static String parseHtml(String htmlData) {
        if (htmlData != null && !htmlData.trim().equals("")) {
            return htmlData.replace("", "");
        } else {
            return "";
        }
    }

    public static void notificationOpenHandler(Context context, Intent getIntent) {

        String uniqueId = getIntent.getStringExtra("unique_id");
        String postId = getIntent.getStringExtra("post_id");
        String title = getIntent.getStringExtra("title");
        String link = getIntent.getStringExtra("link");

        if (getIntent.hasExtra("unique_id")) {

            if (postId != null && !postId.equals("")) {
                if (!postId.equals("0")) {
                    Intent intent = new Intent(context, ActivityNotificationDetail.class);
                    intent.putExtra("post_id", postId);
                    context.startActivity(intent);
                }
            }

            if (link != null && !link.equals("")) {
                Intent intent = new Intent(context, ActivityWebView.class);
                intent.putExtra("title", title);
                intent.putExtra("url", link);
                context.startActivity(intent);
            }

        }

    }

    public static void setNavigation(Activity activity, SharedPref sharedPref) {
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(activity);
        } else {
            Tools.lightNavigation(activity);
        }
        setLayoutDirection(activity);
    }

    public static void setLayoutDirection(Activity activity) {
        if (Config.ENABLE_RTL_MODE) {
            activity.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
    }

    public static void darkNavigation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(activity.getResources().getColor(R.color.black));
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.black));
            activity.getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    public static void lightNavigation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(activity.getResources().getColor(R.color.gnt_white));
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.colorPrimaryDark));
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    public static void transparentStatusBarNavigation(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setWindowFlag(activity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    public static String withSuffix(long count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format("%.1f%c", count / Math.pow(1000, exp), "KMGTPE".charAt(exp - 1));
    }

    public static long timeStringtoMilis(String time) {
        long milis = 0;
        try {
            SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sd.parse(time);
            milis = date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return milis;
    }

    public static int dpToPx(Context c, int dp) {
        Resources r = c.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public static boolean isConnect(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return activeNetworkInfo.isConnected() || activeNetworkInfo.isConnectedOrConnecting();
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static CharSequence getTimeAgo(String dateStr) {
        if (dateStr != null && !dateStr.trim().equals("")) {
            //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            sdf.setTimeZone(TimeZone.getTimeZone("CET"));
            try {
                long time = sdf.parse(dateStr).getTime();
                long now = System.currentTimeMillis();
                return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
            } catch (ParseException e) {
                return "";
            }
        } else {
            return "";
        }
    }

    public static String getFormatedDate(String dateStr) {
        if (dateStr != null && !dateStr.trim().equals("")) {
            SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            oldFormat.setTimeZone(TimeZone.getTimeZone("CET"));
            SimpleDateFormat newFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm");
            try {
                String newStr = newFormat.format(oldFormat.parse(dateStr));
                return newStr;
            } catch (ParseException e) {
                return "";
            }
        } else {
            return "";
        }
    }

    public static void openWebPage(Activity context, String title, String url) {
        Intent intent = new Intent(context, ActivityWebView.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void setAction(Context context, byte[] bytes, String imgName, String action) {
        try {
            File dir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + context.getString(R.string.app_name));
            } else {
                dir = new File(Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name));
            }
            boolean success = true;
            if (!dir.exists()) {
                success = dir.mkdirs();
            }
            if (success) {
                File imageFile = new File(dir, imgName);
                FileOutputStream fileWriter = new FileOutputStream(imageFile);
                fileWriter.write(bytes);
                fileWriter.flush();
                fileWriter.close();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File file = new File(imageFile.getAbsolutePath());
                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);
                context.sendBroadcast(mediaScanIntent);

                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                switch (action) {
                    case DOWNLOAD:
                        //do nothing
                        break;

                    case SHARE:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("image/*");
                        share.putExtra(Intent.EXTRA_TEXT,context.getResources().getString(R.string.share_text) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
                        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + imageFile.getAbsolutePath()));
                        context.startActivity(Intent.createChooser(share, "Share Image"));
                        break;

                    case SET_WITH:
                        Intent setWith = new Intent(Intent.ACTION_ATTACH_DATA);
                        setWith.addCategory(Intent.CATEGORY_DEFAULT);
                        setWith.setDataAndType(Uri.parse("file://" + file.getAbsolutePath()), "image/*");
                        setWith.putExtra("mimeType", "image/*");
                        context.startActivity(Intent.createChooser(setWith, "Set as:"));
                        break;

                    case SET_GIF:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Constant.GIF_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + context.getString(R.string.app_name);
                        } else {
                            Constant.GIF_PATH = Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name);
                        }
                        Constant.GIF_NAME = file.getName();

                        SharedPref sharedPref = new SharedPref(context);
                        sharedPref.saveGif(Constant.GIF_PATH, Constant.GIF_NAME);

                        try {
                            WallpaperManager.getInstance(context).clear();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Intent setGif = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                        setGif.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(context, SetGIFAsWallpaperService.class));
                        context.startActivity(setGif);

                        Log.d("GIF_PATH", Constant.GIF_PATH);
                        Log.d("GIF_NAME", Constant.GIF_NAME);
                        break;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String createName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("File is too large!");
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        InputStream is = new FileInputStream(file);
        try {
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
        } finally {
            is.close();
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        return bytes;
    }

    public static void downloadImage(Activity activity, String filename, String downloadUrlOfImage, String mimeType) {
        try {
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(downloadUrlOfImage);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType(mimeType) // Your file type. You can use this code to download other file types also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + filename + ".jpg");
            dm.enqueue(request);
            //Toast.makeText(activity, "Image download started.", Toast.LENGTH_SHORT).show();
            Snackbar.make(activity.findViewById(android.R.id.content), "Image download started.", Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            //Toast.makeText(activity, "", Toast.LENGTH_SHORT).show();
            Snackbar.make(activity.findViewById(android.R.id.content), "Image download failed.", Snackbar.LENGTH_SHORT).show();
        }
    }

}
