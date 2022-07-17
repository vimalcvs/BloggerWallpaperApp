package com.app.bloggerwallpaperapp.callbacks;

import com.app.bloggerwallpaperapp.models.Ads;
import com.app.bloggerwallpaperapp.models.App;
import com.app.bloggerwallpaperapp.models.Blog;
import com.app.bloggerwallpaperapp.models.Category;
import com.app.bloggerwallpaperapp.models.Notification;

import java.util.ArrayList;
import java.util.List;

public class CallbackConfig {

    public Blog blog = null;
    public App app = null;
    public Notification notification = null;
    public Ads ads = null;
    public List<Category> labels = new ArrayList<>();

}
