package a238443.musicplayer;

import android.content.res.Resources;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{
    RecyclerAdapter mainAdapter;
    RecyclerView recyclerView;
    ClickHandler handler;
    MediaPlayer player;
    Button playOnListButton, playMenu, forwardMenu, rewindMenu;
    RelativeLayout forRecycler;
    View mediaControl;
    TextView duration, titleMenu;
    SeekBar seekMenu;
    Handler durationHandler;
    Handler seekBarHandler;
    int playedPosition = -1;
    int fullTime, currentDuration;
    boolean notMoved = true;
    private static final int DURATION_CHANGE = 10000;
    private static final int FORWARD_COOLDOWN = 1500;
    private static final int REFRESH_DELAY = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findAll();
        addListeners();
        mainAdapter = new RecyclerAdapter(handler, getApplicationContext());
        recyclerView.setAdapter(mainAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        Toolbar mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        parsingData();

        player = new MediaPlayer();
        durationHandler = new Handler();
        seekBarHandler = new Handler();
        manageSeekBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
            if (isFinishing()) {
                player.stop();
                player.release();
            }
        }
    }

    private void addListeners() {
        handler = new ClickHandler() {
            @Override
            public void onButtonClicked(View itemView, int position) {
                Song clicked = mainAdapter.getItem(position);

                if(playedPosition <0) {

                    playerSetup(clicked, position);
                    showMediaControl();

                    playOnListButton = itemView.findViewById(R.id.play_pause_list);
                    playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
                    String menuText = clicked.getTitle()+" ● "+clicked.getAuthor();
                    titleMenu.setText(menuText);

                    fullTime = player.getDuration();
                    currentDuration = 0;
                    seekMenu.setProgress(0);
                    seekMenu.setMax(fullTime);
                    seekMenu.setClickable(true);
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
                if(currentDuration > FORWARD_COOLDOWN) {
                    if (currentDuration + DURATION_CHANGE < fullTime) {
                        currentDuration += DURATION_CHANGE;
                        player.seekTo(currentDuration);
                    } else
                        player.seekTo(fullTime);
                }
            }
        });

        forwardMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(playedPosition < mainAdapter.getItemCount() - 1) {
                    int newPosition = playedPosition + 1;
                    changeTrack(mainAdapter.getItem(newPosition), newPosition);
                }
                return false;
            }
        });

        rewindMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentDuration - DURATION_CHANGE >= 0) {
                    currentDuration -= DURATION_CHANGE;
                    player.seekTo(currentDuration);
                }
                else
                    player.seekTo(0);
            }
        });

        rewindMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(playedPosition > 0) {
                    int newPosition = playedPosition - 1;
                    changeTrack(mainAdapter.getItem(newPosition), newPosition);
                }
                return false;
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("database", mainAdapter.getDatabase());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mainAdapter.addDatabase((ArrayList<Song>)savedInstanceState.getSerializable("database"));
    }

    private void playerSetup(Song clicked, int playedPosition) {
        try { player.setDataSource(getApplicationContext(), Uri.parse(clicked.getFilename())); }
        catch (Exception e) { Log.e("player_start","File not found"); }

        try { player.prepare(); }
        catch (Exception e) { Log.e("player_prepare","Preparation failed"); }

        player.start();
        this.playedPosition = playedPosition;
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

        }
        if(item.getItemId() == R.id.about_menu) {

        }
        return super.onOptionsItemSelected(item);
    }

    private int dpToPx(int dp){
        Resources resources = this.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private void showMediaControl() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)recyclerView.getLayoutParams();
        params.setMargins(0,0,0,dpToPx(170));
        recyclerView.setLayoutParams(params);
        mediaControl.setVisibility(View.VISIBLE);
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

    private void parsingData() {

        CustomXmlParser songParser = new CustomXmlParser(getApplicationContext());
        try {
            mainAdapter.addDatabase(songParser.parse(getResources().openRawResource(R.raw.data)));
        }
        catch (FileNotFoundException e) {
            Log.e("xml_opening","File not found while trying to parse XML");
        }
        catch (org.xmlpull.v1.XmlPullParserException e) {
            Log.e("parsing","XML parser problem");
        }
        catch (IOException e) {
            Log.e("io", "IO problem while trying to parse XML");
        }
    }

    private void findAll() {
        recyclerView = findViewById(R.id.main_recyclerView);
        playOnListButton = findViewById(R.id.play_pause_list);
        mediaControl = findViewById(R.id.media_control);
        forRecycler = findViewById(R.id.for_recycler);
        duration = findViewById(R.id.current_duration);
        playMenu = findViewById(R.id.play_pause_menu);
        forwardMenu = findViewById(R.id.forward_button);
        rewindMenu = findViewById(R.id.rewind_button);
        seekMenu = findViewById(R.id.seek_menu);
        titleMenu = findViewById(R.id.title_menu);
    }

    private void changeTrack(Song newTrack, int newPosition) {
        View newTrackView = recyclerView.getLayoutManager().findViewByPosition(newPosition);
        player.reset();
        playerSetup(newTrack, newPosition);

        playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
        playOnListButton = newTrackView.findViewById(R.id.play_pause_list);
        playOnListButton.setBackground(getDrawable(R.drawable.ic_pause));
        playMenu.setBackground(getDrawable(R.drawable.ic_pause));

        String menuText = newTrack.getTitle()+" ● "+newTrack.getAuthor();
        titleMenu.setText(menuText);
        seekMenu.setProgress(0);
        fullTime = player.getDuration();
        currentDuration = 0;
        seekMenu.setMax(fullTime);
        durationHandler.postDelayed(updateSeekBarTime,REFRESH_DELAY);
    }

    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            currentDuration = player.getCurrentPosition();

            if(notMoved)
                seekMenu.setProgress(currentDuration);

            int min = currentDuration/60000;
            int sec = (currentDuration/1000)%60;
            duration.setText(String.format(Locale.ENGLISH,"%d%s%02d", min,":", sec));
            durationHandler.postDelayed(this, REFRESH_DELAY);

            if(currentDuration >= fullTime - REFRESH_DELAY) {
                if(playedPosition < mainAdapter.getItemCount() - 1) {
                    playedPosition++;
                    changeTrack(mainAdapter.getItem(playedPosition), playedPosition);
                }
                else {
                    player.seekTo(0);
                    player.pause();
                    playMenu.setBackground(getDrawable(R.drawable.ic_play));
                    playOnListButton.setBackground(getDrawable(R.drawable.ic_play));
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

                                int min = currentPosition / 60000;
                                int sec = (currentPosition / 1000) % 60;

                                duration.setText(String.format(Locale.ENGLISH,"%d%s%02d", min,":", sec));

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
}


