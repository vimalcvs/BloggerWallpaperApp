package com.ak.hdwallpaperapp.callbacks;

import com.ak.hdwallpaperapp.models.Post;

import java.util.ArrayList;
import java.util.List;

public class CallbackPost {

    public String kind;
    public String nextPageToken;
    public List<Post> items = new ArrayList<>();
    public String etag;

}
