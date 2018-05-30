package a238443.musicplayer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import static a238443.musicplayer.Constants.FUNCTIONAL.FORWARD_COOLDOWN;

public class MusicService extends Service {
    private MediaPlayer player;
    private ArrayList<Song> database;
    private Handler durationHandler;
    LocalBroadcastManager manager;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notificationManager;
    IntentFilter localBroadcastFilter;
    BroadcastReceiver localBroadcastReceiver;
    PendingIntent previousPendingIntent, playPendingIntent, nextPendingIntent, appPendingIntent;
    private int playedPosition = -1;
    private int rewindAmount = Constants.FUNCTIONAL.REWIND_AMOUNT;
    private int currentDuration = 0;
    private int fullTime = 0;
    private boolean shuffle = false;
    public static boolean IS_SERVICE_RUNNING = false;
    public static boolean IS_MUSIC_STARTED = false;
    public static boolean IS_PLAYING = false;

    @Override
    public void onCreate() {
        super.onCreate();
        localBroadcastReceiver = new MyBroadcastReceiver();
        localBroadcastFilter = new IntentFilter();
        localBroadcastFilter.addAction(Constants.ACTION.PLAY_PAUSE);
        localBroadcastFilter.addAction(Constants.ACTION.NEXT);
        localBroadcastFilter.addAction(Constants.ACTION.PREV);
        registerReceiver(localBroadcastReceiver, localBroadcastFilter);
        durationHandler = new Handler();
        player = new MediaPlayer();
        manager = LocalBroadcastManager.getInstance(this);
        notificationManager = NotificationManagerCompat.from(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if(action != null) {
            switch (action) {
                case Constants.ACTION.START_SERVICE:
                    database = intent.getParcelableArrayListExtra(Constants.EXTRAS.DATABASE);
                    playedPosition = intent.getIntExtra(Constants.EXTRAS.NEW_POSITION, -1);
                    rewindAmount = intent.getIntExtra(Constants.EXTRAS.REWIND_AMOUNT, Constants.FUNCTIONAL.REWIND_AMOUNT);
                    shuffle = intent.getBooleanExtra(Constants.EXTRAS.SHUFFLE_USE, false);
                    currentDuration = intent.getIntExtra(Constants.EXTRAS.CURRENT_DURATION, 0);
                    IS_SERVICE_RUNNING = true;
                    builder = new NotificationCompat.Builder(this, "music_player");

                    if (playedPosition != -1) {
                        builder.setContentTitle(database.get(playedPosition).getTitle());
                        builder.setContentText(database.get(playedPosition).getAuthor());
                    } else {
                        builder.setContentTitle("");
                        builder.setContentText("");
                    }

                    addListener();
                    setupIntents();
                    baseNotificationBuild();
                    startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());

                    if(playedPosition != -1)
                        specialStart();
                    if(player.isPlaying())
                        changeIcon();
                    break;

                case Constants.ACTION.CHOICE:
                    int position = intent.getIntExtra(Constants.EXTRAS.CLICKED_POSITION, 0);
                    Song clicked = database.get(position);
                    updateNotification(clicked);

                    if(!player.isPlaying()) {
                        IS_MUSIC_STARTED = true;
                        playerSetup(clicked, position);
                        updateDuration.run();
                        changeIcon();
                    }
                    else {
                        if(playedPosition == position)
                            playPause();
                        else
                            changeTrack(position);
                    }
                    break;

                case Constants.ACTION.PLAY_PAUSE: playPause(); break;
                case Constants.ACTION.FORWARD: goForward(); break;
                case Constants.ACTION.NEXT: startNext(); break;
                case Constants.ACTION.PREV: startPrevious(); break;
                case Constants.ACTION.REWIND: goRewind(); break;

                case Constants.ACTION.MOVE_DURATION:
                    int progress = intent.getIntExtra(Constants.EXTRAS.PROGRESS,0);
                    player.seekTo(progress);
                    basicBroadcast();
                    break;

                case Constants.ACTION.SETTINGS_UPDATE:
                    shuffle = intent.getBooleanExtra(Constants.EXTRAS.SHUFFLE_USE, false);
                    rewindAmount = intent.getIntExtra(Constants.EXTRAS.REWIND_AMOUNT, Constants.FUNCTIONAL.REWIND_AMOUNT);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(player.isPlaying())
            player.stop();
        player.release();
        unregisterReceiver(localBroadcastReceiver);
    }

    private void setupIntents() {
        Intent previousIntent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
        previousIntent.setAction(Constants.ACTION.PREV);
        previousPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, previousIntent, 0);

        Intent playIntent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
        playIntent.setAction(Constants.ACTION.PLAY_PAUSE);
        playPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, playIntent, 0);

        Intent nextIntent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
        nextIntent.setAction(Constants.ACTION.NEXT);
        nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, nextIntent, 0);

        Intent appIntent = new Intent(this, MainActivity.class);
        appPendingIntent = PendingIntent.getActivity(this, 0, appIntent, 0);
    }

    private void baseNotificationBuild() {
        builder
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .addAction(R.drawable.ic_rewind, "previous", previousPendingIntent)
                .addAction(R.drawable.ic_play_borderless, "pause", playPendingIntent)
                .addAction(R.drawable.ic_forward, "next", nextPendingIntent)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,1,2))
                .setContentIntent(appPendingIntent)
                .setOngoing(true);
    }

    private void specialStart() {
        Song alreadyStarted = database.get(playedPosition);
        specialPlayerSetup(alreadyStarted);
        player.seekTo(currentDuration);
        IS_MUSIC_STARTED = true;
        updateDuration.run();
    }

    private void specialPlayerSetup(Song clicked) {
        player.reset();

        try { player.setDataSource(getApplicationContext(), Uri.parse(clicked.getFilename())); }
        catch (Exception e) { Log.e("player_start","File not found"); }

        try { player.prepare(); }
        catch (Exception e) { Log.e("player_prepare","Preparation failed"); }

        player.start();
        player.pause();
        fullTime = player.getDuration();
    }

    private void playerSetup(Song clicked, int playedPosition) {
        player.reset();

        try { player.setDataSource(getApplicationContext(), Uri.parse(clicked.getFilename())); }
        catch (Exception e) { Log.e("player_start","File not found"); }

        try { player.prepare(); }
        catch (Exception e) { Log.e("player_prepare","Preparation failed"); }

        player.start();
        IS_PLAYING = true;
        fullTime = player.getDuration();
        fullTimeBroadcast();
        this.playedPosition = playedPosition;
    }

    private void playPause() {
        if(player.isPlaying()) {
            player.pause();
            changeIcon();
        }
        else {
            player.start();
            changeIcon();
        }
        playPauseBroadcast();
    }

    private void changeIcon() {
        int currentIcon = builder.mActions.get(1).icon;

        if(currentIcon == R.drawable.ic_pause_borderless) {
            builder.mActions.get(1).icon = R.drawable.ic_play_borderless;
            IS_PLAYING = false;
        }
        else {
            builder.mActions.get(1).icon = R.drawable.ic_pause_borderless;
            IS_PLAYING = true;
        }
        notificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());
    }

    private void goForward() {
        if(currentDuration > FORWARD_COOLDOWN) {
            if (currentDuration + rewindAmount < fullTime) {
                currentDuration += rewindAmount;
                player.seekTo(currentDuration);
            } else
                player.seekTo(fullTime);
        }
        basicBroadcast();
    }

    private void goRewind() {
        if(currentDuration - rewindAmount >= 0) {
            currentDuration -= rewindAmount;
            player.seekTo(currentDuration);
        }
        else
            player.seekTo(0);
        basicBroadcast();
    }

    private void startNext() {
        if(!player.isPlaying())
            changeIcon();
        if(shuffle) useShuffle();
        else changeTrack(playedPosition + 1);
    }

    private void startPrevious() {
        if(!player.isPlaying())
            changeIcon();
        if(shuffle) useShuffle();
        else changeTrack(playedPosition - 1);
    }

    private void useShuffle() {
        Random gen = new Random();
        int range = database.size();
        int shuffledPosition = (gen.nextInt(range-1)+1+playedPosition)%range;
        changeTrack(shuffledPosition);
    }

    private void changeTrack(int newPosition) {
        if(newPosition < 0 || newPosition >= database.size()) {
            if(newPosition < 0)
                newPosition = database.size() - 1;
            else
                newPosition = 0;
        }
        Song newTrack = database.get(newPosition);
        int previous = playedPosition;
        playerSetup(newTrack, newPosition);
        trackChangeBroadcast(previous);
        updateDuration.run();
        updateNotification(newTrack);
    }

    private void updateNotification(Song source) {
        builder.setContentTitle(source.getTitle());
        builder.setContentText(source.getAuthor());
        notificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());
    }

    public void basicBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.BASIC);
        intent.putExtra(Constants.EXTRAS.CURRENT_DURATION, currentDuration);
        manager.sendBroadcast(intent);
    }

    public void playPauseBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.PLAY_PAUSE);
        intent.putExtra(Constants.EXTRAS.IS_PLAYING, player.isPlaying());
        manager.sendBroadcast(intent);
    }

    public void trackChangeBroadcast(int previouslyPlayed) {
        Intent intent = new Intent(Constants.BROADCASTS.TRACK_CHANGE);
        intent.putExtra(Constants.EXTRAS.NEW_POSITION, playedPosition);
        intent.putExtra(Constants.EXTRAS.PLAYED_POSITION, previouslyPlayed);
        intent.putExtra(Constants.EXTRAS.FULL_TIME, player.getDuration());
        manager.sendBroadcast(intent);
    }

    public void fullTimeBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.FULLTIME);
        intent.putExtra(Constants.EXTRAS.FULL_TIME, player.getDuration());
        manager.sendBroadcast(intent);
    }

    private Runnable updateDuration = new Runnable() {
        public void run() {
            currentDuration = player.getCurrentPosition();
            durationHandler.postDelayed(this, Constants.FUNCTIONAL.DURATION_BROADCAST_REFRESH_DELAY);
            basicBroadcast();
        }
    };

    private void addListener() {
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(shuffle) useShuffle();
                else {
                    if (playedPosition < database.size() - 1) {
                        changeTrack(playedPosition + 1);
                    }
                    else {
                        player.seekTo(0);
                        player.pause();
                        changeIcon();
                        playPauseBroadcast();
                    }
                }
            }
        });
    }
}
