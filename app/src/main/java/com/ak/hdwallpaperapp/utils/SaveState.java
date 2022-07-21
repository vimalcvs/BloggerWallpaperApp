package com.ak.hdwallpaperapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SaveState   {
    private final SharedPreferences sharedPreferences;

    public SaveState(Context context) {
        sharedPreferences = context.getSharedPreferences("preferences",Context.MODE_PRIVATE);
    }
    public void setState(boolean bValue){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("DayNight",bValue);
        editor.apply();
    }
    public boolean getState(){
        return sharedPreferences.getBoolean("DayNight",false);
    }
}
