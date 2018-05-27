package a238443.musicplayer;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import static a238443.musicplayer.Constants.FUNCTIONAL.MUSIC_REFRESH_DELAY;

public class MainActivity extends AppCompatActivity{
    RecyclerAdapter mainAdapter;
    RecyclerView recyclerView;
    ClickHandler handler;
    Button playOnListButton, playMenu, forwardMenu, rewindMenu;
    RelativeLayout forRecycler;
    View mediaControl;
    TextView durationMenu, titleMenu, fullTimeMenu;
    SeekBar seekMenu;
    Handler durationHandler;
    Handler seekBarHandler;
    IntentFilter localBroadcastFilter;
    LocalBroadcastManager manager;
    PreCachingLayoutManager layoutManager;
    int playedPosition = -1;
    int fullTime, currentDuration;
    boolean notMoved = true;
    boolean doubleClickInverted = false;
    boolean shuffle = false;
    private SharedPreferences sharedPref;
    private int rewindAmount = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel();

        findAll();
        addListeners();

        mainAdapter = new RecyclerAdapter(handler, getApplicationContext());
        parsingData();
        recyclerView.setAdapter(mainAdapter);

        layoutManager = new PreCachingLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(getScreenHeight(getApplicationContext()));

        sharedPref = getSharedPreferences(getString(R.string.user_saves), Activity.MODE_PRIVATE);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Toolbar mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        readUsersData();

        durationHandler = new Handler();
        seekBarHandler = new Handler();
        seekMenu.setClickable(true);
        manageSeekBar();
        manageService();

        localBroadcastFilter = new IntentFilter();
        localBroadcastFilter.addAction(Constants.BROADCASTS.BASIC);
        localBroadcastFilter.addAction(Constants.BROADCASTS.PLAY_PAUSE);
        localBroadcastFilter.addAction(Constants.BROADCASTS.TRACK_CHANGE);
        localBroadcastFilter.addAction(Constants.BROADCASTS.FULLTIME);

        manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(localBroadcastReceiver, localBroadcastFilter);

        /*if(playedPosition != -1) {
            showMediaControl();
            menuChange(mainAdapter.getItem(playedPosition), currentDuration);
            indicatePlayed(playedPosition);
        }*/
    }

    //TODO pauza nie zmienia się na play na ekranie blokady

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            deleteChannel();
        /*Intent service = new Intent(this, MusicService.class);
        stopService(service);*/
        manager.unregisterReceiver(localBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveUsersData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(playedPosition != -1) {
            showMediaControl();
            menuChange(mainAdapter.getItem(playedPosition), currentDuration);
            //simplifiedIndicate(playedPosition);
        }
        updateSeekBarTime.run();
    }

    //TODO nie pogrubia po obrocie

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("database", mainAdapter.getDatabase());
        savedInstanceState.putBoolean("isInverted", doubleClickInverted);
        savedInstanceState.putBoolean("shuffle", shuffle);
        savedInstanceState.putInt("rewindAmount", rewindAmount);
        if(playedPosition != -1) {
            savedInstanceState.putInt("currentPosition", playedPosition);
            savedInstanceState.putInt("currentDuration", currentDuration);
            savedInstanceState.putInt("fullTime",fullTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mainAdapter.addDatabase((ArrayList<Song>)savedInstanceState.getSerializable("database"));
        doubleClickInverted = savedInstanceState.getBoolean("isInverted");
        shuffle = savedInstanceState.getBoolean("shuffle");
        rewindAmount = savedInstanceState.getInt("rewindAmount");
        playedPosition = savedInstanceState.getInt("currentPosition", -1);
        currentDuration = savedInstanceState.getInt("currentDuration",0);
        fullTime = savedInstanceState.getInt("fullTime",0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings_menu) {
            Intent toSettings = new Intent(this, SettingsActivity.class);
            toSettings.putExtra("inverted",doubleClickInverted);
            toSettings.putExtra("shuffle",shuffle);
            toSettings.putExtra("rewind",rewindAmount/1000);
            startActivityForResult(toSettings,1);
        }
        if(item.getItemId() == R.id.about_menu) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1) {
            if(resultCode == Activity.RESULT_OK) {
                shuffle = data.getBooleanExtra("shuffle",false);
                doubleClickInverted = data.getBooleanExtra("inverted", false);
                rewindAmount = data.getIntExtra("rewind", 10000);
                updateServiceSettings();
            }
        }
    }

    private void findAll() {
        recyclerView = findViewById(R.id.main_recyclerView);
        playOnListButton = findViewById(R.id.play_pause_list);
        mediaControl = findViewById(R.id.media_control);
        forRecycler = findViewById(R.id.for_recycler);
        durationMenu = findViewById(R.id.current_duration);
        playMenu = findViewById(R.id.play_pause_menu);
        forwardMenu = findViewById(R.id.forward_button);
        rewindMenu = findViewById(R.id.rewind_button);
        seekMenu = findViewById(R.id.seek_menu);
        titleMenu = findViewById(R.id.title_menu);
        fullTimeMenu = findViewById(R.id.full_time);
    }

    private void addListeners() {
        handler = new ClickHandler() {
            @Override
            public void onButtonClicked(View itemView, int position) {
                Song clicked = mainAdapter.getItem(position);

                if(!MusicService.IS_MUSIC_STARTED) {
                    Intent service = new Intent(MainActivity.this, MusicService.class);
                    service.setAction(Constants.ACTION.CHOICE);
                    service.putExtra(Constants.EXTRAS.CLICKED_POSITION, position);
                    ContextCompat.startForegroundService(getApplicationContext(),service);

                    showMediaControl();
                    menuChange(clicked, 0);
                    playedPosition = position;
                    indicatePlayed(position);

                    playOnListButton = itemView.findViewById(R.id.play_pause_list);
                    playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));

                    durationHandler.postDelayed(updateSeekBarTime, MUSIC_REFRESH_DELAY);
                }
                else {
                    if(playedPosition == position)
                        basicServiceUpdate(Constants.ACTION.PLAY_PAUSE);
                    else
                        changeTrack(position, false);
                }
            }
        };

        playMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                basicServiceUpdate(Constants.ACTION.PLAY_PAUSE);
            }
        });

        forwardMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doubleClickInverted)
                    basicServiceUpdate(Constants.ACTION.NEXT);
                else
                    basicServiceUpdate(Constants.ACTION.FORWARD);
            }
        });

        forwardMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (doubleClickInverted)
                    basicServiceUpdate(Constants.ACTION.FORWARD);
                else
                    basicServiceUpdate(Constants.ACTION.NEXT);
                return true;
            }
        });

        rewindMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doubleClickInverted)
                    basicServiceUpdate(Constants.ACTION.PREV);
                else
                    basicServiceUpdate(Constants.ACTION.REWIND);
            }
        });

        rewindMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (doubleClickInverted)
                    basicServiceUpdate(Constants.ACTION.REWIND);
                else
                    basicServiceUpdate(Constants.ACTION.PREV);
                return true;
            }
        });
    }

    private void basicServiceUpdate(String actionCode) {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        service.setAction(actionCode);
        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    private void parsingData() {
        CustomXmlParser songParser = new CustomXmlParser(getApplicationContext());
        try {
            mainAdapter.addDatabase(songParser.parse(getResources().openRawResource(R.raw.data)));
        } catch (FileNotFoundException e) {
            Log.e("xml_opening", "File not found while trying to parse XML");
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            Log.e("parsing", "XML parser problem");
        } catch (IOException e) {
            Log.e("io", "IO problem while trying to parse XML");
        }
    }

    private void showMediaControl() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)recyclerView.getLayoutParams();
        params.setMargins(0,0,0,dpToPx((int)getResources().getDimension(R.dimen.media_menu_size)));
        recyclerView.setLayoutParams(params);
        mediaControl.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp){
        Resources resources = this.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT) / 2;
    }

    private void playPause(boolean isPlaying) {
        View newTrackView = recyclerView.getLayoutManager().findViewByPosition(playedPosition);
        playOnListButton = newTrackView.findViewById(R.id.play_pause_list);
        if(!isPlaying) {
            playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
            playMenu.setBackground(getDrawable(R.drawable.ic_play));
        }
        else {
            playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
            playMenu.setBackground(getDrawable(R.drawable.ic_pause));
        }
    }

    private void changeTrack(int newPosition, boolean fromService) {
        Song newTrack = mainAdapter.getItem(newPosition);
        View newTrackView = recyclerView.getLayoutManager().findViewByPosition(playedPosition);

        if(newTrackView != null) {
            playOnListButton = newTrackView.findViewById(R.id.play_pause_list);
            indicatePlayed(newPosition);
            playedPosition = newPosition;
            if(!fromService)
                updateServiceTrack(newPosition);

            newTrackView = recyclerView.getLayoutManager().findViewByPosition(playedPosition);
            playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
            playOnListButton = newTrackView.findViewById(R.id.play_pause_list);
            playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
            playMenu.setBackground(getDrawable(R.drawable.ic_pause));

            menuChange(newTrack, 0);
        }
        durationHandler.postDelayed(updateSeekBarTime, MUSIC_REFRESH_DELAY);
    }

    private void updateServiceTrack(int newPosition) {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        service.setAction(Constants.ACTION.CHOICE);
        service.putExtra(Constants.EXTRAS.CLICKED_POSITION, newPosition);
        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            if(notMoved)
                seekMenu.setProgress(currentDuration);

            durationMenu.setText(getTimeString(currentDuration));
            durationHandler.postDelayed(this, MUSIC_REFRESH_DELAY);
        }
    };

    private void manageSeekBar() {
        seekMenu.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int seekedProgess;

            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
                seekedProgess = progress;

                if (fromUser) {
                    notMoved = false;
                    Runnable runnable = new Runnable() {

                        @Override
                        public void run() {
                            if (MusicService.IS_SERVICE_RUNNING) {
                                int currentPosition = seekMenu.getProgress();
                                durationMenu.setText(getTimeString(currentPosition));
                            }
                            seekBarHandler.postDelayed(this, MUSIC_REFRESH_DELAY);
                        }
                    };
                    runnable.run();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                moveDuration(seekedProgess);
                currentDuration = seekedProgess;
                notMoved = true;
            }
        });
    }

    private void setSeekMenuProgress(final int progress) {
        seekMenu.post(new Runnable() {
            @Override
            public void run() {
                seekMenu.setProgress(progress);
            }
        });
    }

    private void moveDuration(int progress) {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        service.setAction(Constants.ACTION.MOVE_DURATION);
        service.putExtra(Constants.EXTRAS.PROGRESS, progress);
        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    private String getTimeString(int duration) {
        int min = duration/60000;
        int sec = (duration/1000)%60;
        return String.format(Locale.ENGLISH,"%d%s%02d", min,":", sec);
    }

    private void menuChange(Song newTrack, int progress) {
        String menuText = newTrack.getTitle()+" ● "+newTrack.getAuthor();
        titleMenu.setText(menuText);

        fullTimeMenu.setText(getTimeString(fullTime));

        durationMenu.setText(getTimeString(progress));
        setSeekMenuProgress(progress);
        seekMenu.setMax(fullTime);

        currentDuration = progress;
    }

    //TODO pierwszy puszczony utwór nie ma pełnego czasu

    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    private void indicatePlayed(int newPosition) {
        View trackView;
        TextView title;

        trackView = recyclerView.getLayoutManager().findViewByPosition(playedPosition);
        if(trackView != null) {
            title = trackView.findViewById(R.id.list_title);
            title.setTypeface(Typeface.DEFAULT);
        }

        trackView = recyclerView.getLayoutManager().findViewByPosition(newPosition);
        if(trackView != null) {
            title = trackView.findViewById(R.id.list_title);
            title.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    /*private void simplifiedIndicate(int newPosition) {
        View trackView = recyclerView.getLayoutManager().findViewByPosition(newPosition);
        TextView title = trackView.findViewById(R.id.list_title);
        title.setTypeface(Typeface.DEFAULT_BOLD);
    }*/

    private void saveUsersData() {
        SharedPreferences.Editor sp_editor = sharedPref.edit();
        sp_editor.putInt("rewind", rewindAmount);
        sp_editor.apply();
        sp_editor.putBoolean("shuffle", shuffle);
        sp_editor.apply();
        sp_editor.putBoolean("inverted", doubleClickInverted);
        sp_editor.apply();
        if(playedPosition != -1) {
            sp_editor.putInt("currentPosition", playedPosition);
            sp_editor.apply();
            sp_editor.putInt("currentDuration", currentDuration);
            sp_editor.apply();
            sp_editor.putInt("fullTime", fullTime);
            sp_editor.apply();
        }
    }

    private void readUsersData() {
        rewindAmount = sharedPref.getInt("rewind",10000);
        shuffle = sharedPref.getBoolean("shuffle", false);
        doubleClickInverted = sharedPref.getBoolean("inverted", false);
        playedPosition = sharedPref.getInt("currentPosition", -1);
        currentDuration = sharedPref.getInt("currentDuration",0);
        fullTime = sharedPref.getInt("fullTime",0);
    }

    private void manageService() {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        if (!MusicService.IS_SERVICE_RUNNING) {
            service.setAction(Constants.ACTION.START_SERVICE);
            service.putExtra(Constants.EXTRAS.PLAYED_POSITION, playedPosition);
            service.putExtra(Constants.EXTRAS.REWIND_AMOUNT, rewindAmount);
            service.putExtra(Constants.EXTRAS.SHUFFLE_USE, shuffle);
            service.putExtra(Constants.EXTRAS.DATABASE, mainAdapter.getDatabase());
            MusicService.IS_SERVICE_RUNNING = true;
        } else {
            service.setAction(Constants.ACTION.STOP_SERVICE);
            MusicService.IS_SERVICE_RUNNING = false;
        }
        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_LOW;
        CharSequence name = "Music Service";
        String id = "music_player";
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription("Music playback service");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void deleteChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String id = "music_player";
        mNotificationManager.deleteNotificationChannel(id);
    }

    private void updateServiceSettings() {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        service.setAction(Constants.ACTION.SETTINGS_UPDATE);
        service.putExtra(Constants.EXTRAS.SHUFFLE_USE, shuffle);
        service.putExtra(Constants.EXTRAS.REWIND_AMOUNT, rewindAmount);
        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action != null) {
                if (action.equals(Constants.BROADCASTS.BASIC)) {
                    currentDuration = intent.getIntExtra(Constants.EXTRAS.CURRENT_DURATION, 0);
                }
                else if (action.equals(Constants.BROADCASTS.PLAY_PAUSE)) {
                    playPause(intent.getBooleanExtra(Constants.EXTRAS.IS_PLAYING, false));
                }
                else if (action.equals(Constants.BROADCASTS.TRACK_CHANGE)) {
                    fullTime = intent.getIntExtra(Constants.EXTRAS.FULL_TIME, 0);
                    changeTrack(intent.getIntExtra(Constants.EXTRAS.PLAYED_POSITION, 0), true);
                }
                else if(action.equals(Constants.BROADCASTS.FULLTIME)) {
                    fullTime = intent.getIntExtra(Constants.EXTRAS.FULL_TIME, 0);
                    seekMenu.setMax(fullTime);
                }
            }
        }
    };
}


