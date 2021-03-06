package com.mikhaellopez.saveinsta.activity;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.mikhaellopez.saveinsta.BuildConfig;
import com.mikhaellopez.saveinsta.R;
import com.mikhaellopez.saveinsta.activity.generic.ABaseActivity;
import com.mikhaellopez.saveinsta.model.EventChangeDominantColor;
import com.mikhaellopez.saveinsta.model.EventInstaPictureLoad;
import com.mikhaellopez.saveinsta.model.InstaData;
import com.mikhaellopez.saveinsta.model.InstaMedia;
import com.mikhaellopez.saveinsta.model.InstaOwner;
import com.mikhaellopez.saveinsta.utils.DominantImageColor;
import com.mikhaellopez.saveinsta.utils.PermissionsUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import butterknife.Bind;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Copyright (C) 2016 Mikhael LOPEZ
 * Licensed under the Apache License Version 2.0
 */
public class MainActivity extends ABaseActivity {

    @Bind(R.id.toolbar_title)
    protected TextView mToolbarTitle;
    @Bind(R.id.coordinator_layout)
    protected View mCoordinatorLayout;

    @Bind(R.id.progressBar)
    protected View mProgressBar;

    @Bind(R.id.card_view)
    protected View mCardView;
    @Bind(R.id.image_to_download)
    protected ImageView mImageToDownload;
    @Bind(R.id.icon_profile)
    protected ImageView mIconProfil;
    @Bind(R.id.text_user_name)
    protected TextView mTextUserName;
    @Bind(R.id.video_to_download)
    protected VideoView mVideoToDownload;
    @Bind(R.id.image_action_play)
    protected View mImageActionPlay;

    @Bind(R.id.layout_input)
    protected View mLayoutInput;
    @Bind(R.id.edit_insta_url)
    protected EditText editInstaUrl;

    private MenuItem mMenuItem;
    private String mUserName;
    private String mIdInstaContent;
    private boolean mIsVideo;
    private String mUrlVideo;
    private String mCurrentUrlRemove = null;
    private int mCurrentDominantColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCurrentDominantColor = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Init View
        initView(pastClipboard());
    }

    private void initView(String instaUrl) {
        // Init View
        mCardView.setVisibility(View.GONE);
        mLayoutInput.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        // Check Insta URL
        if (instaUrl != null && instaUrl.contains("https://www.instagram.com/p/") && !instaUrl.equals(mCurrentUrlRemove)) {
            // Load Image
            loadImageData(instaUrl);
        } else {
            // Show Input
            mProgressBar.setVisibility(View.GONE);
            mLayoutInput.setVisibility(View.VISIBLE);
            // Clear Theme Color
            setThemeColor(mCurrentDominantColor, ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }

    //region Action Click
    @OnClick({R.id.text_user_name, R.id.icon_profile})
    protected void onClickUserName() {
        Uri uri = Uri.parse("http://instagram.com/_u/" + mUserName);
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);
        likeIng.setPackage("com.instagram.android");
        try {
            startActivity(likeIng);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/" + mUserName)));
        }
    }

    @OnClick(R.id.image_action_close)
    protected void onClickClose() {
        // Save URL to remove
        mCurrentUrlRemove = pastClipboard();
        // Init View
        initView(null);
    }

    @OnClick(R.id.image_action_download)
    protected void onClickDownload() {
        if (PermissionsUtils.checkAndRequest(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                PermissionsUtils.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE,
                getString(R.string.dialog_message_why_permission), null)) {
            saveInstaContent();
        }
    }

    @OnClick(R.id.btn_open_insta)
    protected void onClickOpenInstagram() {
        // Close Keyboard
        closeKeyboard();
        // Start Instagram App or Open Market
        String packageName = "com.instagram.android";
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(mCoordinatorLayout, R.string.snackbar_no_instagram, Snackbar.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_load_it)
    protected void onClickLoadIt() {
        String editVal = editInstaUrl.getText().toString();
        if (editVal != null) {
            if (editVal.contains("https://www.instagram.com/p/")) {
                // Clear current Url Remove
                mCurrentUrlRemove = null;
                // Clear editText
                editInstaUrl.setText(null);
                // Close Keyboard
                closeKeyboard();
                // Init View
                initView(editVal);
            } else {
                // Is Not Insta URL
                Snackbar.make(mCoordinatorLayout, R.string.snackbar_no_insta_url, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            // Empty
            Snackbar.make(mCoordinatorLayout, R.string.snackbar_input_empty, Snackbar.LENGTH_SHORT).show();
        }
    }

    @OnClick({R.id.image_action_play, R.id.video_to_download})
    protected void onClickPlayVideo() {
        if (!mVideoToDownload.isPlaying()) {
            Uri uri = Uri.parse(mUrlVideo);
            mVideoToDownload.setVideoURI(uri);
            mVideoToDownload.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });
            mVideoToDownload.start();
            mVideoToDownload.setVisibility(View.VISIBLE);
        } else {
            mVideoToDownload.setVisibility(View.GONE);
            mVideoToDownload.stopPlayback();
        }
    }
    //endregion

    //region Load Image & User information
    private void loadImageData(final String instaUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get User and Profil Picture
                try {
                    URL url = new URL(instaUrl);
                    InputStream is = url.openStream();
                    int ptr;
                    StringBuffer buffer = new StringBuffer();
                    while ((ptr = is.read()) != -1) {
                        buffer.append((char) ptr);
                    }

                    String html = buffer.toString();

                    String startJson = "<script type=\"text/javascript\">window._sharedData = ";
                    String endJson = ";</script>";

                    String json = html.substring(html.indexOf(startJson) + startJson.length(),
                            html.indexOf(endJson));

                    InstaData instaData = new Gson().fromJson(json, InstaData.class);
                    InstaMedia instaMedia = instaData.getEntryData().getPostPage().get(0).getMedia();
                    InstaOwner instaOwner = instaMedia.getOwner();
                    String userName = instaOwner.getUsername();
                    String fullName = instaOwner.getFullname();
                    String urlProfile = instaOwner.getProfilePicUrl();

                    EventBus.getDefault().post(new EventInstaPictureLoad(userName, fullName, instaUrl + "media/?size=l", urlProfile, instaMedia.getId(), instaMedia.isVideo(), instaMedia.getVideoUrl()));

                } catch (Exception ex) {
                    // Send Fail
                    EventBus.getDefault().post(new EventInstaPictureLoad(false));
                }

            }
        }).start();
    }

    public void onEventMainThread(final EventInstaPictureLoad eventInstaPictureLoad) {
        if (eventInstaPictureLoad != null && eventInstaPictureLoad.isLoadWell()) {
            // Save userName and id image
            mUserName = eventInstaPictureLoad.getUserName();
            mIdInstaContent = eventInstaPictureLoad.getIdContent();

            // Load User Name to View
            mTextUserName.setText(eventInstaPictureLoad.getUserFullName());

            // If is video, Save URL
            mVideoToDownload.setVisibility(View.GONE);
            if (eventInstaPictureLoad.isVideo()) {
                mIsVideo = true;
                mUrlVideo = eventInstaPictureLoad.getUrlVideo();
                mImageActionPlay.setVisibility(View.VISIBLE);
            } else {
                mIsVideo = false;
                mImageActionPlay.setVisibility(View.GONE);
            }

            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Load Image to View
                            Picasso.with(MainActivity.this).load(eventInstaPictureLoad.getUrlImage())
                                    .into(mImageToDownload, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            EventBus.getDefault().post(new EventChangeDominantColor());

                                            // Show Result
                                            mProgressBar.setVisibility(View.GONE);
                                            mCardView.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onError() {
                                        }
                                    });

                            // Load Profil Icon to View
                            Picasso.with(MainActivity.this).load(eventInstaPictureLoad.getUrlIconProfil())
                                    .into(mIconProfil);
                        }
                    });
                }
            }).start();
        } else {
            initView(null);
            // Show erreur
            Snackbar.make(mCoordinatorLayout, R.string.snackbar_load_photo_failed, Snackbar.LENGTH_SHORT).show();
        }
    }
    //endregion

    //region Private Method
    private String pastClipboard() {
        String textToPaste = null;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                textToPaste = clip.getItemAt(0).getText().toString();
            }
        }
        return textToPaste;
    }

    private void saveInstaContent() {
        if (!mIsVideo) {
            // Save Picture
            Bitmap bitmap = ((BitmapDrawable) mImageToDownload.getDrawable()).getBitmap();
            if (downloadPicture(bitmap, mIdInstaContent + ".jpg")) {
                Snackbar.make(mCoordinatorLayout, R.string.snackbar_photo_saved, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mCoordinatorLayout, R.string.snackbar_save_photo_failed, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            // Save Video
            if (mVideoToDownload != null && mVideoToDownload.isPlaying()) {
                mVideoToDownload.stopPlayback();
                mVideoToDownload.setVisibility(View.GONE);
            }

            startDownloadVideo(mUrlVideo, mIdInstaContent + ".mp4");
        }
    }

    private boolean downloadPicture(Bitmap bitmap, String fileName) {
        boolean result = false;
        OutputStream output = null;
        try {
            File root = new File(Environment.getExternalStorageDirectory() + File.separator + getResources().getString(R.string.app_name) + File.separator);
            root.mkdirs();
            File sdImageMainDirectory = new File(root, fileName);
            output = new FileOutputStream(sdImageMainDirectory);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.flush();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private void startDownloadVideo(final String urlVideo, final String fileName) {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isDownload = downloadVideo(urlVideo, fileName);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isDownload) {
                            Snackbar.make(mCoordinatorLayout, R.string.snackbar_video_saved, Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(mCoordinatorLayout, R.string.snackbar_save_video_failed, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }).start();
    }

    private boolean downloadVideo(String urlVideo, String fileName) {
        boolean result = false;
        OutputStream output = null;
        InputStream input = null;
        try {
            File root = new File(Environment.getExternalStorageDirectory() + File.separator + getResources().getString(R.string.app_name) + File.separator);
            root.mkdirs();
            File sdImageMainDirectory = new File(root, fileName);

            // Download Video
            URL url = new URL(urlVideo);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.connect();

            input = urlConnection.getInputStream();
            output = new FileOutputStream(sdImageMainDirectory);

            byte[] data = new byte[input.available()];
            input.read(data);
            output.write(data);

            byte[] buffer = new byte[1024];
            int len1;
            while ((len1 = input.read(buffer)) > 0) {
                output.write(buffer, 0, len1);
            }

            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private void closeKeyboard() {
        // Check if no view has focus:
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    //endregion

    //region Theme & Color
    public void onEventMainThread(EventChangeDominantColor eventChangeDominantColor) {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get Dominant Color
                final int color = getDominantColor(mImageToDownload);
                if (color != 0) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCardView.getVisibility() == View.VISIBLE) {
                                // Set Theme color with Dominant color of Image
                                setThemeColor(mCurrentDominantColor, color);
                            }
                        }
                    });
                } else if (BuildConfig.DEBUG) {
                    Log.e(getClass().getName(), "Color Unknown");
                }
            }
        }).start();
    }

    private int getDominantColor(ImageView imageView) {
        int color = 0;
        try {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            String dominantColor = DominantImageColor.getDominantColorOfImage(bitmap);
            color = Color.parseColor(dominantColor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return color;
    }

    private void setThemeColor(int colorFrom, final int colorTo) {
        // Set Toolbar and NavigationBar color
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(),
                colorFrom, colorTo);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                mToolbar.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();

        // Set Status Bar color
        ValueAnimator colorAnimationDark = ValueAnimator.ofObject(new ArgbEvaluator(),
                darkerColor(colorFrom), darkerColor(colorTo));
        colorAnimationDark.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor((int) animator.getAnimatedValue());
                    getWindow().setNavigationBarColor((int) animator.getAnimatedValue());
                }
            }
        });
        colorAnimationDark.start();

        // Set Text and icon color
        int textColorFrom = getTextColorByBackground(colorFrom);
        int textColorTo = getTextColorByBackground(colorTo);

        if (textColorFrom != colorTo) {
            ValueAnimator colorAnimationText = ValueAnimator.ofObject(new ArgbEvaluator(),
                    textColorFrom, textColorTo);
            colorAnimationText.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    mToolbarTitle.setTextColor((int) animator.getAnimatedValue());
                    if (mMenuItem != null) {
                        mMenuItem.setIcon(isColorDark(colorTo) ? ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_info) :
                                ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_info_dark));
                    }
                }
            });
            colorAnimationText.start();
        }
        mCurrentDominantColor = colorTo;
    }

    private int getTextColorByBackground(int backgroundColor) {
        int textColor;
        if (isColorDark(backgroundColor)) {
            textColor = ContextCompat.getColor(this, android.R.color.white);
        } else {
            textColor = ContextCompat.getColor(this, R.color.colorAccent);
        }
        return textColor;
    }

    private int darkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    public boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        if (darkness < 0.5) {
            return false; // It's a light color
        } else {
            return true; // It's a dark color
        }
    }
    //endregion

    //region Menu Implememtatiom
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenuItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                // Show Dialog Information
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_launcher))
                        .setTitle(getResources().getString(R.string.app_name))
                        .setMessage(getResources().getString(R.string.info_message))
                        .setCancelable(false)
                        .setNeutralButton(getString(R.string.dialog_button_rate_app), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                                } catch (ActivityNotFoundException e) {
                                    Snackbar.make(mCoordinatorLayout, R.string.snackbar_no_market, Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setPositiveButton(getResources().getString(android.R.string.ok), null)
                        .create();

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                    }
                });

                dialog.show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionsUtils.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    saveInstaContent();
                }
                break;
        }
    }
    //endregion

}
