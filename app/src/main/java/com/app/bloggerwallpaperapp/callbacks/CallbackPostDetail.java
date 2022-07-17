package com.app.bloggerwallpaperapp.callbacks;

import com.app.bloggerwallpaperapp.models.Author;
import com.app.bloggerwallpaperapp.models.Blog;
import com.app.bloggerwallpaperapp.models.Replies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CallbackPostDetail implements Serializable {

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
    public Replies replies = null;
    public List<String> labels = new ArrayList<>();
    public String etag;

}
