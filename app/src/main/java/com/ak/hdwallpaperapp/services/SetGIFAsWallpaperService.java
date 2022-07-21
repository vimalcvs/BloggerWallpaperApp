package com.ak.hdwallpaperapp.services;

import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.ak.hdwallpaperapp.database.prefs.SharedPref;
import com.ak.hdwallpaperapp.utils.Constant;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SetGIFAsWallpaperService extends WallpaperService {

    private class GIFWallpaperEngine extends Engine {

        private final Runnable drawGIF = GIFWallpaperEngine.this::draw;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private SurfaceHolder holder;
        private Movie movie;
        private float scaleRatio, x, y;
        private boolean visible;

        public GIFWallpaperEngine() {
            super();
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.holder = surfaceHolder;
        }

        public void onDestroy() {
            super.onDestroy();
            this.handler.removeCallbacks(this.drawGIF);
        }

        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            try {
                File imageFile = new File(Constant.GIF_PATH, Constant.GIF_NAME);
                final int readLimit = 16 * 1024;
                InputStream mInputStream = new BufferedInputStream(new FileInputStream(imageFile), readLimit);
                mInputStream.mark(readLimit);
                movie = Movie.decodeStream(mInputStream);
            } catch (FileNotFoundException e) {
                try {
                    SharedPref sharedPref = new SharedPref(getApplicationContext());
                    File imageFile = new File(sharedPref.getPath() + "/" + sharedPref.getGifName());
                    final int readLimit = 16 * 1024;
                    InputStream mInputStream = new BufferedInputStream(new FileInputStream(imageFile), readLimit);
                    mInputStream.mark(readLimit);
                    movie = Movie.decodeStream(mInputStream);
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                    try {
                        String LOCAL_GIF = "images/default_image.gif";
                        movie = Movie.decodeStream(getResources().getAssets().open(LOCAL_GIF));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                e.printStackTrace();
            }
        }

        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            super.onSurfaceChanged(surfaceHolder, i, i2, i3);
            super.onSurfaceChanged(surfaceHolder, i, i2, i3);
            int mMovieWidth = this.movie.width();
            int mMovieHeight = this.movie.height();
            this.scaleRatio = Math.max(((float) i2) / ((float) mMovieWidth), ((float) i3) / ((float) mMovieHeight));
            this.x = (((float) i2) - (((float) mMovieWidth) * this.scaleRatio)) / 2.0f;
            this.y = (((float) i3) - (((float) mMovieHeight) * this.scaleRatio)) / 2.0f;
            this.x /= this.scaleRatio;
            this.y /= this.scaleRatio;
        }

        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
        }

        public void draw() {
            if (this.visible) {
                Canvas lockCanvas = this.holder.lockCanvas();
                lockCanvas.save();
                lockCanvas.scale(this.scaleRatio, this.scaleRatio);
                this.movie.draw(lockCanvas, this.x, this.y);
                lockCanvas.restore();
                this.holder.unlockCanvasAndPost(lockCanvas);
                this.movie.setTime((int) (System.currentTimeMillis() % ((long) this.movie.duration())));
                this.handler.removeCallbacks(this.drawGIF);
                this.handler.postDelayed(this.drawGIF, 0);
            }
        }

        public void onVisibilityChanged(boolean z) {
            this.visible = z;
            if (z) {
                this.handler.post(this.drawGIF);
            } else {
                this.handler.removeCallbacks(this.drawGIF);
            }
        }
    }

    public Engine onCreateEngine() {
        return new GIFWallpaperEngine();
    }

    public void onDestroy() {
        super.onDestroy();
    }
}
