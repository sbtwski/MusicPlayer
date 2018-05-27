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
import static a238443.musicplayer.Constants.FUNCTIONAL.MUSIC_REFRESH_DELAY;

public class MusicService extends Service {
    private MediaPlayer player;
    private ArrayList<Song> database;
    private Handler durationHandler;
    LocalBroadcastManager manager;
    NotificationCompat.Builder builder;
    NotificationManagerCompat notifManager;
    IntentFilter localBroadcastFilter;
    BroadcastReceiver localBroadcastReceiver;
    private int playedPosition = -1;
    private int rewindAmount = 10000;
    private int currentDuration = 0;
    private int fullTime = 0;
    private boolean shuffle = false;
    private static final String LOG_TAG = "MusicService";
    public static boolean IS_SERVICE_RUNNING = false;
    public static boolean IS_MUSIC_STARTED = false;

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
        notifManager = NotificationManagerCompat.from(this);
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
            if (action.equals(Constants.ACTION.START_SERVICE)) {
                database = intent.getParcelableArrayListExtra(Constants.EXTRAS.DATABASE);
                playedPosition = intent.getIntExtra(Constants.EXTRAS.PLAYED_POSITION, -1);
                rewindAmount = intent.getIntExtra(Constants.EXTRAS.REWIND_AMOUNT, 10000);
                shuffle = intent.getBooleanExtra(Constants.EXTRAS.SHUFFLE_USE, false);
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

                Intent previousIntent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                previousIntent.setAction(Constants.ACTION.PREV);
                PendingIntent previousPendingIntent =
                        PendingIntent.getBroadcast(getApplicationContext(), 0, previousIntent, 0);

                Intent playIntent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                playIntent.setAction(Constants.ACTION.PLAY_PAUSE);
                PendingIntent playPendingIntent =
                        PendingIntent.getBroadcast(getApplicationContext(), 0, playIntent, 0);

                Intent nextIntent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                nextIntent.setAction(Constants.ACTION.NEXT);
                PendingIntent nextPendingIntent =
                        PendingIntent.getBroadcast(getApplicationContext(), 0, nextIntent, 0);

                Intent appIntent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent appPendingIntent = PendingIntent.getActivity(this, 0, appIntent, 0);

                builder
                        .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP))
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                        .addAction(R.drawable.ic_rewind, "previous", previousPendingIntent)
                        .addAction(R.drawable.ic_pause_borderless, "pause", playPendingIntent)
                        .addAction(R.drawable.ic_forward, "next", nextPendingIntent)
                        .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0,1,2))
                        .setContentIntent(appPendingIntent)
                        .setOngoing(true);


                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());
            }
            if(action.equals(Constants.ACTION.CHOICE)) {
                int position = intent.getIntExtra(Constants.EXTRAS.CLICKED_POSITION, 0);
                Song clicked = database.get(position);
                updateNotification(clicked);

                if(!player.isPlaying()) {
                    IS_MUSIC_STARTED = true;
                    playerSetup(clicked, position);
                    durationHandler.postDelayed(updateDuration, MUSIC_REFRESH_DELAY);
                }
                else {
                    if(playedPosition == position)
                        playPause();
                    else
                        changeTrack(position);
                }
            }
            if(action.equals(Constants.ACTION.PLAY_PAUSE))
                playPause();
            if(action.equals(Constants.ACTION.FORWARD))
                goForward();
            if(action.equals(Constants.ACTION.NEXT))
                startNext();
            if(action.equals(Constants.ACTION.PREV))
                startPrevious();
            if(action.equals(Constants.ACTION.REWIND))
                goRewind();
            if(action.equals(Constants.ACTION.MOVE_DURATION)) {
                int progress = intent.getIntExtra(Constants.EXTRAS.PROGRESS,0);
                player.seekTo(progress);
                basicBroadcast();
            }
            if(action.equals(Constants.ACTION.SETTINGS_UPDATE)) {
                shuffle = intent.getBooleanExtra(Constants.EXTRAS.SHUFFLE_USE, false);
                rewindAmount = intent.getIntExtra(Constants.EXTRAS.REWIND_AMOUNT, 10000);
            }
        }
        return START_STICKY;
    }

    //TODO Nie ogarnia widoków i crashuje jak się wróci po zminimalizowaniu apki i próbuje wybrać utwór

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(player.isPlaying())
            player.stop();
        player.release();
        unregisterReceiver(localBroadcastReceiver);
        Log.i(LOG_TAG, "In onDestroy");
    }

    private void playerSetup(Song clicked, int playedPosition) {
        player.reset();

        try { player.setDataSource(getApplicationContext(), Uri.parse(clicked.getFilename())); }
        catch (Exception e) { Log.e("player_start","File not found"); }

        try { player.prepare(); }
        catch (Exception e) { Log.e("player_prepare","Preparation failed"); }

        player.start();
        fullTime = player.getDuration();
        fulltimeBroadcast();
        this.playedPosition = playedPosition;
    }

    private void playPause() {
        if(player.isPlaying()) {
            player.pause();
        }
        else {
            player.start();
        }
        playpauseBroadcast();
    }

    private void goForward() {
        Log.d("FORWARD","Forward at all");
        if(currentDuration > FORWARD_COOLDOWN) {
            if (currentDuration + rewindAmount < fullTime) {
                Log.d("FORWARD","Forward second if");
                currentDuration += rewindAmount;
                player.seekTo(currentDuration);
            } else
                player.seekTo(fullTime);
        }
    }

    private void startNext() {
        if(shuffle) useShuffle();
        else changeTrack(playedPosition + 1);
    }

    private void goRewind() {
        if(currentDuration - rewindAmount >= 0) {
            currentDuration -= rewindAmount;
            player.seekTo(currentDuration);
        }
        else
            player.seekTo(0);
    }

    private void startPrevious() {
        if(shuffle) useShuffle();
        else changeTrack(playedPosition - 1);
    }

    private void useShuffle() {
        Random gen = new Random();
        int range = database.size();
        int shuffledPosition = (gen.nextInt(range-1)+1+playedPosition)%range;
        changeTrack(shuffledPosition);
    }

    public void basicBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.BASIC);
        intent.putExtra(Constants.EXTRAS.CURRENT_DURATION, currentDuration);
        manager.sendBroadcast(intent);
    }

    public void playpauseBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.PLAY_PAUSE);
        intent.putExtra(Constants.EXTRAS.IS_PLAYING, player.isPlaying());
        manager.sendBroadcast(intent);
    }

    public void trackChangeBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.TRACK_CHANGE);
        intent.putExtra(Constants.EXTRAS.PLAYED_POSITION, playedPosition);
        intent.putExtra(Constants.EXTRAS.FULL_TIME, player.getDuration());
        manager.sendBroadcast(intent);
    }

    public void fulltimeBroadcast() {
        Intent intent = new Intent(Constants.BROADCASTS.FULLTIME);
        intent.putExtra(Constants.EXTRAS.FULL_TIME, player.getDuration());
        manager.sendBroadcast(intent);
    }

    private Runnable updateDuration = new Runnable() {
        public void run() {
            currentDuration = player.getCurrentPosition();
            durationHandler.postDelayed(this, MUSIC_REFRESH_DELAY);
            basicBroadcast();
        }
    };

    private void changeTrack(int newPosition) {
        if(newPosition >= 0 && newPosition < database.size()) {
            Song newTrack = database.get(newPosition);
            playerSetup(newTrack, newPosition);
            durationHandler.postDelayed(updateDuration, MUSIC_REFRESH_DELAY);
            trackChangeBroadcast();
            updateNotification(newTrack);
        }
        else {
            player.pause();
        }
    }

    private void updateNotification(Song source) {
        builder.setContentTitle(source.getTitle());
        builder.setContentText(source.getAuthor());
        notifManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, builder.build());
    }

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
                        playPause();
                    }
                }
            }
        });
    }
}
