package com.test.widget;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class MusicPlayerService extends Service {

    private MediaPlayer mediaPlayer;
    private String mediaFilePath;
    private String mediaName;
    private String mediaMetaInfo;

    public MusicPlayerService() { }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(havePermission()){
                loadSong(this);
                updateAppWidget(this,  Constants.sWidgetUnspecifiedAction);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startSelfForground(mediaName);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(){
        String NOTIFICATION_CHANNEL_ID = "com.test.widget";
        String channelName = "MusicService";
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(getResources().getColor(R.color.colorPrimary));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startSelfForground(String title){
        String NOTIFICATION_CHANNEL_ID = "com.test.widget";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setColor(getResources().getColor(R.color.colorPrimary));
        startForeground(1, notificationBuilder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean havePermission() {
        return  checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int actionId        = Constants.sWidgetUnspecifiedAction;
        int permissionState = Constants.sPermissionUnspecified;

        if(intent != null) {
            actionId = intent.getIntExtra(Constants.sActionEvent, Constants.sWidgetUnspecifiedAction);
            permissionState = intent.getIntExtra(Constants.sPermissionEventKey, Constants.sPermissionUnspecified);
        }

        if(mediaFilePath==null && permissionState != Constants.sPermissionUnspecified)
            loadSong(this);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(havePermission()){
                loadSong(this);
            }
        }

        if(mediaFilePath!=null){
            if(mediaPlayer!=null && mediaPlayer.isPlaying())
                updateAppWidget(this,  Constants.sWidgetPlayButtonAction);
            else
                updateAppWidget(this,  Constants.sWidgetStopButtonAction);
        }

        if(actionId!=Constants.sWidgetUnspecifiedAction) {
            updateAppWidget(this,  actionId);
            performAction(actionId);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    protected void performAction(int actionId) {

        switch(actionId) {

            case Constants.sWidgetPlayButtonAction:
                try {
                    if(mediaFilePath!=null){
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                updateAppWidget(MusicPlayerService.this, Constants.sWidgetStopButtonAction);
                                mediaPlayer.stop();
                            }
                        });
                        mediaPlayer.setDataSource(mediaFilePath);
                        mediaPlayer.prepare();
                        mediaPlayer.setLooping(true);
                        mediaPlayer.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case Constants.sWidgetStopButtonAction:
                if(mediaPlayer!=null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                break;
        }

    }

    void updateAppWidget(Context context, int actionId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.music_player_widget);

        if(mediaName==null) {
            updateViews.setTextViewText(R.id.titletv, "--");
            updateViews.setTextViewText(R.id.metatv, "--");
        } else {
            updateViews.setTextViewText(R.id.titletv, mediaName);
            updateViews.setTextViewText(R.id.metatv, mediaMetaInfo);
        }

        Intent intent = new Intent(context, MusicPlayerWidget.class);
        intent.putExtra(Constants.sActionEvent, Constants.sWidgetStopButtonAction);
        updateViews.setOnClickPendingIntent(R.id.stop_button, PendingIntent.getBroadcast(context, Constants.sWidgetStopButtonAction, intent, 0));

        intent = new Intent(context, MusicPlayerWidget.class);
        intent.putExtra(Constants.sActionEvent, Constants.sWidgetPlayButtonAction);
        updateViews.setOnClickPendingIntent(R.id.play_button, PendingIntent.getBroadcast(context, Constants.sWidgetPlayButtonAction, intent, 0));


        switch (actionId) {
            case Constants.sWidgetPlayButtonAction:
                updateViews.setViewVisibility(R.id.play_button, View.INVISIBLE);
                updateViews.setViewVisibility(R.id.stop_button, View.VISIBLE);
                break;

            case Constants.sWidgetStopButtonAction:
                updateViews.setViewVisibility(R.id.play_button, View.VISIBLE);
                updateViews.setViewVisibility(R.id.stop_button, View.INVISIBLE);
                break;
        }

        ComponentName thisWidget = new ComponentName(context, MusicPlayerWidget.class);
        appWidgetManager.updateAppWidget(thisWidget, updateViews);
    }


    private void loadSong(Context context)  {

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM};
        Cursor cursor = context.getContentResolver().query(uri, projection, null , null, null);
        if (cursor!=null && cursor.moveToNext()) {
            mediaFilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
            mediaName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
            mediaMetaInfo = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
            if(mediaMetaInfo==null)
                mediaMetaInfo = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startSelfForground(mediaName);
            }
        }
        cursor.close();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}