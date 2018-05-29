package a238443.musicplayer;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import android.widget.Button;
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
    View mediaControl;
    TextView durationMenu, titleMenu, fullTimeMenu, titleToBold;
    SeekBar seekMenu;
    Handler durationHandler;
    Handler seekBarHandler;
    Handler updateUIHandler;
    IntentFilter localBroadcastFilter;
    LocalBroadcastManager manager;
    PreCachingLayoutManager layoutManager;
    int playedPosition = -1;
    int fullTime, currentDuration;
    boolean notMoved = true;
    boolean doubleClickInverted = false;
    boolean shuffle = false;
    private SharedPreferences sharedPref;
    private int rewindAmount = Constants.FUNCTIONAL.REWIND_AMOUNT;
    private boolean callStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        findAll();
        addListeners();

        mainAdapter = new RecyclerAdapter(handler, getApplicationContext());
        parsingData();
        recyclerView.setAdapter(mainAdapter);

        layoutManager = new PreCachingLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(getScreenHeight());

        sharedPref = getSharedPreferences(getString(R.string.user_saves), Activity.MODE_PRIVATE);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setDrawingCacheEnabled(true);

        Toolbar mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        readUsersData();

        durationHandler = new Handler();
        seekBarHandler = new Handler();
        updateUIHandler = new Handler();
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

        if(playedPosition != -1) {
            quickSetupAfterRestoration(savedInstanceState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isFinishing()) {
            deleteChannel();
            Intent service = new Intent(this, MusicService.class);
            stopService(service);
            manager.unregisterReceiver(localBroadcastReceiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveUsersData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSeekBarTime.run();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("database", mainAdapter.getDatabase());
        savedInstanceState.putBoolean("isInverted", doubleClickInverted);
        savedInstanceState.putBoolean("shuffle", shuffle);
        savedInstanceState.putInt("rewindAmount", rewindAmount);
        savedInstanceState.putBoolean("callStart", callStart);
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
                rewindAmount = data.getIntExtra("rewind", Constants.FUNCTIONAL.REWIND_AMOUNT);
                updateServiceSettings();
            }
        }
    }

    private void quickSetupAfterRestoration(Bundle savedInstanceState) {
        showMediaControl();
        menuChange(mainAdapter.getItem(playedPosition), currentDuration);
        simplifiedIndicate(playedPosition);
        if(savedInstanceState != null)
            callStart = savedInstanceState.getBoolean("callStart", true);
        if(callStart) {
            Runnable playChange = new Runnable() {
                int counter = Constants.FUNCTIONAL.CHANGE_ATTEMPTS;
                @Override
                public void run() {
                    if(counter > 0) {
                        playMenu.setBackground(getDrawable(R.drawable.ic_play));
                        counter--;
                        updateUIHandler.postDelayed(this, Constants.FUNCTIONAL.UI_UPDATE_REFRESH_DELAY);
                    }
                    else
                        updateUIHandler.removeCallbacks(this);
                }
            };
            playChange.run();
            callStart = false;
        }
        else
            invertPlayIcon(playedPosition,true);
    }

    private void findAll() {
        recyclerView = findViewById(R.id.main_recyclerView);
        playOnListButton = findViewById(R.id.play_pause_list);
        titleToBold = findViewById(R.id.list_title);
        mediaControl = findViewById(R.id.media_control);
        durationMenu = findViewById(R.id.current_duration);
        playMenu = findViewById(R.id.play_pause_menu);
        forwardMenu = findViewById(R.id.forward_button);
        rewindMenu = findViewById(R.id.rewind_button);
        seekMenu = findViewById(R.id.seek_menu);
        titleMenu = findViewById(R.id.title_menu);
        fullTimeMenu = findViewById(R.id.full_time);
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

    public int getScreenHeight() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
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
                    indicatePlayed(position, playedPosition);
                    invertPlayIcon(playedPosition, true);

                    updateSeekBarTime.run();
                }
                else {
                    if(playedPosition == position)
                        basicServiceUpdate(Constants.ACTION.PLAY_PAUSE);
                    else
                        changeTrack(position, playedPosition, false);
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

    private void showMediaControl() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)recyclerView.getLayoutParams();
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            params.setMargins(0,0,0,dpToPx((int)getResources().getDimension(R.dimen.media_menu_portrait_size)));
        else
            params.setMargins(0,0,dpToPx((int)getResources().getDimension(R.dimen.media_menu_landscape_size)),0);
        recyclerView.setLayoutParams(params);
        mediaControl.setVisibility(View.VISIBLE);
    }

    private int dpToPx(int dp){
        Resources resources = this.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT) / 2;
    }

    private void invertPlayIcon(int position, boolean playOn) {
        menuIconUpdate(playOn);
        listIconUpdate(position,playOn);
    }

    private boolean loadTitleView(int position) {
        SongHolder newTrackView = (SongHolder)recyclerView.findViewHolderForAdapterPosition(position);
        if(newTrackView != null) {
            titleToBold = newTrackView.titleText;
            return true;
        }
        return false;
    }

    private boolean loadIconView(int position) {
        SongHolder newTrackView = (SongHolder)recyclerView.findViewHolderForAdapterPosition(position);
        if(newTrackView != null) {
            playOnListButton = newTrackView.playButton;
            return true;
        }
        return false;
    }

    private void changeTrack(int newPosition, int previousPosition,  boolean fromService) {
        Song newTrack = mainAdapter.getItem(newPosition);

        invertPlayIcon(previousPosition, false);
        indicatePlayed(newPosition, previousPosition);
        playedPosition = newPosition;
        invertPlayIcon(playedPosition, true);
        if(!fromService)
            updateServiceTrack(newPosition);

        menuChange(newTrack, 0);

        updateSeekBarTime.run();
    }

    private void updateServiceTrack(int newPosition) {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        service.setAction(Constants.ACTION.CHOICE);
        service.putExtra(Constants.EXTRAS.CLICKED_POSITION, newPosition);
        ContextCompat.startForegroundService(getApplicationContext(),service);
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

    private String getTimeString(int duration) {
        int min = duration/60000;
        int sec = (duration/1000)%60;
        return String.format(Locale.ENGLISH,"%d%s%02d", min,":", sec);
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
            int setProgress;

            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
                setProgress = progress;

                if (fromUser) {
                    notMoved = false;
                    Runnable runnable = new Runnable() {

                        @Override
                        public void run() {
                            if (MusicService.IS_SERVICE_RUNNING) {
                                int currentPosition = seekMenu.getProgress();
                                durationMenu.setText(getTimeString(currentPosition));
                            }
                            seekBarHandler.postDelayed(this, Constants.FUNCTIONAL.REWIND_MENU_REFRESH_DELAY);
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
                moveDuration(setProgress);
                currentDuration = setProgress;
                notMoved = true;
            }
        });
    }

    private void moveDuration(int progress) {
        Intent service = new Intent(MainActivity.this, MusicService.class);
        service.setAction(Constants.ACTION.MOVE_DURATION);
        service.putExtra(Constants.EXTRAS.PROGRESS, progress);
        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    private void setSeekMenuProgress(final int progress) {
        seekMenu.post(new Runnable() {
            @Override
            public void run() {
                seekMenu.setProgress(progress);
            }
        });
    }

    private void menuIconUpdate(boolean playOn) {
        if(!playOn)
            playMenu.setBackground(getDrawable(R.drawable.ic_play));
        else
            playMenu.setBackground(getDrawable(R.drawable.ic_pause));
    }

    private void listIconUpdate(final int position, final boolean playState) {
        final Runnable iconUpdater = new Runnable() {
            int positionToUpdate = position;
            boolean playOn = playState;
            @Override
            public void run() {
                if(loadIconView(positionToUpdate)){
                    if (!playOn)
                        playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
                    else
                        playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
                    updateUIHandler.removeCallbacks(this);
                }
                else
                    updateUIHandler.postDelayed(this,Constants.FUNCTIONAL.MINOR_ELEMENTS_REFRESH_DELAY);
            }
        };
        iconUpdater.run();
    }

    private void indicatePlayed(int newPosition, int previouslyPlayed) {
        if(previouslyPlayed != -1)
            typefaceUpdate(previouslyPlayed,Typeface.DEFAULT);
        typefaceUpdate(newPosition, Typeface.DEFAULT_BOLD);
    }

    private void simplifiedIndicate(int newPosition) {
        typefaceUpdate(newPosition,Typeface.DEFAULT_BOLD);
    }

    private void typefaceUpdate(final int position, final Typeface typeface) {
        Runnable typefaceUpdater = new Runnable() {
            int positionToUpdate = position;
            Typeface typefaceToUpdate = typeface;
            @Override
            public void run() {
                if(loadTitleView(positionToUpdate)) {
                    titleToBold.setTypeface(typefaceToUpdate);
                    updateUIHandler.removeCallbacks(this);
                }
                else
                    updateUIHandler.postDelayed(this,Constants.FUNCTIONAL.MINOR_ELEMENTS_REFRESH_DELAY);
            }
        };
        typefaceUpdater.run();
    }

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
        rewindAmount = sharedPref.getInt("rewind",Constants.FUNCTIONAL.REWIND_AMOUNT);
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
            if(playedPosition != -1) {
                service.putExtra(Constants.EXTRAS.NEW_POSITION, playedPosition);
                service.putExtra(Constants.EXTRAS.CURRENT_DURATION, currentDuration);
            }
            service.putExtra(Constants.EXTRAS.REWIND_AMOUNT, rewindAmount);
            service.putExtra(Constants.EXTRAS.SHUFFLE_USE, shuffle);
            service.putExtra(Constants.EXTRAS.DATABASE, mainAdapter.getDatabase());
            MusicService.IS_SERVICE_RUNNING = true;
        }

        ContextCompat.startForegroundService(getApplicationContext(),service);
    }

    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_LOW;
        CharSequence name = "Music Service";
        String id = "music_player";
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription("Music playback service");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if(notificationManager != null)
            notificationManager.createNotificationChannel(channel);
    }

    private void deleteChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String id = "music_player";
        if(notificationManager != null)
            notificationManager.deleteNotificationChannel(id);
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
                switch (action) {
                    case Constants.BROADCASTS.BASIC:
                        currentDuration = intent.getIntExtra(Constants.EXTRAS.CURRENT_DURATION, 0);
                        break;
                    case Constants.BROADCASTS.PLAY_PAUSE:
                        invertPlayIcon(playedPosition, intent.getBooleanExtra(Constants.EXTRAS.IS_PLAYING,false));
                        break;
                    case Constants.BROADCASTS.TRACK_CHANGE:
                        fullTime = intent.getIntExtra(Constants.EXTRAS.FULL_TIME, 0);
                        changeTrack(intent.getIntExtra(Constants.EXTRAS.NEW_POSITION, 0),intent.getIntExtra(Constants.EXTRAS.PLAYED_POSITION, 0), true);
                        break;
                    case Constants.BROADCASTS.FULLTIME:
                        fullTime = intent.getIntExtra(Constants.EXTRAS.FULL_TIME, 0);
                        seekMenu.setMax(fullTime);
                }
            }
        }
    };
}


