package com.ak.hdwallpaperapp.callbacks;

import com.ak.hdwallpaperapp.models.Author;
import com.ak.hdwallpaperapp.models.Blog;

import java.io.Serializable;

public class CallbackPageDetail implements Serializable {

    public String kind;
    public String id;
    public Blog blog = null;
    public String published;
    public String updated;
    public String url;
    public String selflink;
    public String title;
    public String content;
    public Author author = null;
    public String etag;

}
