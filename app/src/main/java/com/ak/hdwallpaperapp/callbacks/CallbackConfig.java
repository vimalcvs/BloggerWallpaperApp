package com.ak.hdwallpaperapp.callbacks;

import com.ak.hdwallpaperapp.models.Ads;
import com.ak.hdwallpaperapp.models.App;
import com.ak.hdwallpaperapp.models.Blog;
import com.ak.hdwallpaperapp.models.Category;
import com.ak.hdwallpaperapp.models.Notification;

import java.util.ArrayList;
import java.util.List;

public class CallbackConfig {

    public Blog blog = null;
    public App app = null;
    public Notification notification = null;
    public Ads ads = null;
    public List<Category> labels = new ArrayList<>();

}
