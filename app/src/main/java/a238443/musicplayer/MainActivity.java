package a238443.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity{
    RecyclerAdapter mainAdapter;
    RecyclerView recyclerView;
    ClickHandler handler;
    MediaPlayer player;
    Button playOnListButton, playMenu, forwardMenu, rewindMenu;
    RelativeLayout forRecycler;
    View mediaControl;
    TextView durationMenu, titleMenu, fullTimeMenu;
    SeekBar seekMenu;
    Handler durationHandler;
    Handler seekBarHandler;
    int playedPosition = -1;
    int fullTime, currentDuration;
    boolean notMoved = true;
    boolean doubleClickInverted = false;
    boolean shuffle = false;
    private SharedPreferences sharedPref;
    private int rewindAmount = 10000;
    private static final int FORWARD_COOLDOWN = 1500;
    private static final int REFRESH_DELAY = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findAll();
        addListeners();

        mainAdapter = new RecyclerAdapter(handler, getApplicationContext());
        parsingData();
        recyclerView.setAdapter(mainAdapter);

        PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(getScreenHeight(getApplicationContext()));

        sharedPref = getSharedPreferences(getString(R.string.user_saves), Activity.MODE_PRIVATE);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        Toolbar mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        readUsersData();
        player = new MediaPlayer();

        durationHandler = new Handler();
        seekBarHandler = new Handler();
        seekMenu.setClickable(true);
        manageSeekBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            if(player.isPlaying()) {
                player.pause();
                playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
                playMenu.setBackground(getDrawable(R.drawable.ic_play));
            }
            if (isFinishing()) {
                player.stop();
                player.release();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveUsersData();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("database", mainAdapter.getDatabase());
        savedInstanceState.putBoolean("isInverted", doubleClickInverted);
        savedInstanceState.putBoolean("shuffle", shuffle);
        savedInstanceState.putInt("rewindAmount", rewindAmount);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mainAdapter.addDatabase((ArrayList<Song>)savedInstanceState.getSerializable("database"));
        doubleClickInverted = savedInstanceState.getBoolean("isInverted");
        shuffle = savedInstanceState.getBoolean("shuffle");
        rewindAmount = savedInstanceState.getInt("rewindAmount");
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

                if(playedPosition <0) {
                    playerSetup(clicked, position);
                    showMediaControl();
                    menuChange(clicked);
                    indicatePlayed(position);

                    playOnListButton = itemView.findViewById(R.id.play_pause_list);
                    playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));

                    durationHandler.postDelayed(updateSeekBarTime,REFRESH_DELAY);
                }
                else {
                    if(playedPosition == position)
                        playPause();
                    else
                        changeTrack(clicked,position);
                }
            }
        };

        playMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPause();
            }
        });

        forwardMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doubleClickInverted)
                    startNext();
                else
                    goForward();
            }
        });

        forwardMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (doubleClickInverted)
                    goForward();
                else
                    startNext();
                return true;
            }
        });

        rewindMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doubleClickInverted)
                    startPrevious();
                else
                    goRewind();
            }
        });

        rewindMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (doubleClickInverted)
                    goRewind();
                else
                    startPrevious();
                return true;
            }
        });
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

    private void playerSetup(Song clicked, int playedPosition) {
        player.reset();

        try { player.setDataSource(getApplicationContext(), Uri.parse(clicked.getFilename())); }
        catch (Exception e) { Log.e("player_start","File not found"); }

        try { player.prepare(); }
        catch (Exception e) { Log.e("player_prepare","Preparation failed"); }

        player.start();
        this.playedPosition = playedPosition;
    }

    private void playPause() {
        if(player.isPlaying()) {
            player.pause();
            playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
            playMenu.setBackground(getDrawable(R.drawable.ic_play));
        }
        else {
            player.start();
            playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
            playMenu.setBackground(getDrawable(R.drawable.ic_pause));
        }
    }

    private void changeTrack(Song newTrack, int newPosition) {
        View newTrackView = recyclerView.getLayoutManager().findViewByPosition(newPosition);
        if(newTrackView != null) {
            indicatePlayed(newPosition);
            playerSetup(newTrack, newPosition);

            playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
            playOnListButton = newTrackView.findViewById(R.id.play_pause_list);
            playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
            playMenu.setBackground(getDrawable(R.drawable.ic_pause));

            menuChange(newTrack);
        }
        durationHandler.postDelayed(updateSeekBarTime,REFRESH_DELAY);
    }

    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            currentDuration = player.getCurrentPosition();

            if(notMoved)
                seekMenu.setProgress(currentDuration);

            durationMenu.setText(getTimeString(currentDuration));
            durationHandler.postDelayed(this, REFRESH_DELAY);

            if(currentDuration >= fullTime - REFRESH_DELAY) {
                if(shuffle) useShuffle();
                else {
                    if (playedPosition < mainAdapter.getItemCount() - 1) {
                        changeTrack(mainAdapter.getItem(playedPosition + 1), playedPosition + 1);
                    } else {
                        player.seekTo(0);
                        player.pause();
                        playMenu.setBackground(getDrawable(R.drawable.ic_play));
                        playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
                    }
                }
            }

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
                            if (player != null) {
                                int currentPosition = seekMenu.getProgress();
                                durationMenu.setText(getTimeString(currentPosition));
                            }
                            seekBarHandler.postDelayed(this, REFRESH_DELAY);
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
                player.seekTo(seekedProgess);
                notMoved = true;
            }
        });
    }

    private String getTimeString(int duration) {
        int min = duration/60000;
        int sec = (duration/1000)%60;
        return String.format(Locale.ENGLISH,"%d%s%02d", min,":", sec);
    }

    private void menuChange(Song newTrack) {
        String menuText = newTrack.getTitle()+" ● "+newTrack.getAuthor();
        titleMenu.setText(menuText);

        fullTime = player.getDuration();
        fullTimeMenu.setText(getTimeString(fullTime));

        seekMenu.setProgress(0);
        seekMenu.setMax(fullTime);

        currentDuration = 0;
    }

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
        title = trackView.findViewById(R.id.list_title);
        title.setTypeface(Typeface.DEFAULT);

        trackView = recyclerView.getLayoutManager().findViewByPosition(newPosition);
        title = trackView.findViewById(R.id.list_title);
        title.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void goForward() {
        if(currentDuration > FORWARD_COOLDOWN) {
            if (currentDuration + rewindAmount < fullTime) {
                currentDuration += rewindAmount;
                player.seekTo(currentDuration);
            } else
                player.seekTo(fullTime);
        }
    }

    private void startNext() {
        if(shuffle) useShuffle();
        else {
            if (playedPosition < mainAdapter.getItemCount() - 1) {
                int newPosition = playedPosition + 1;
                changeTrack(mainAdapter.getItem(newPosition), newPosition);
            }
        }
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
        else {
            if (playedPosition > 0) {
                int newPosition = playedPosition - 1;
                changeTrack(mainAdapter.getItem(newPosition), newPosition);
            }
        }
    }

    private void saveUsersData() {
        SharedPreferences.Editor sp_editor = sharedPref.edit();
        sp_editor.putInt("rewind", rewindAmount);
        sp_editor.apply();
        sp_editor.putBoolean("shuffle", shuffle);
        sp_editor.apply();
        sp_editor.putBoolean("inverted", doubleClickInverted);
        sp_editor.apply();
    }

    private void readUsersData() {
        rewindAmount = sharedPref.getInt("rewind",10000);
        shuffle = sharedPref.getBoolean("shuffle", false);
        doubleClickInverted = sharedPref.getBoolean("inverted", false);
    }

    private void useShuffle() {
        Random gen = new Random();
        int range = mainAdapter.getItemCount();
        int shuffledPosition = (gen.nextInt(range-1)+1+playedPosition)%range;
        changeTrack(mainAdapter.getItem(shuffledPosition), shuffledPosition);
    }
}


