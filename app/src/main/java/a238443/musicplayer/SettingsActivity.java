package a238443.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity{
    Toolbar settingsToolbar;
    TextView minSeek, maxSeek, currentSeek;
    SeekBar rewindSeek;
    Switch shuffleSwitch, doubleTrackChange, doubleRewind;
    Handler seekBarHandler;
    boolean doubleClickInverted = false;
    boolean useShuffle = false;
    int rewindAmount = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);
        getData();

        findAll();
        settingsToolbar.setNavigationIcon(R.drawable.ic_back);
        addListeners();

        seekBarHandler = new Handler();
        rewindSeek.setMax(59);
        rewindSeek.setProgress(rewindAmount);
        rewindSeek.setClickable(true);
        shuffleSwitch.setChecked(useShuffle);
        doubleRewind.setChecked(doubleClickInverted);
        doubleTrackChange.setChecked(!doubleClickInverted);
        manageSeekBar();
        manageTextViews();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("isInverted", doubleClickInverted);
        savedInstanceState.putBoolean("shuffle", useShuffle);
        savedInstanceState.putInt("rewindAmount", rewindAmount);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        doubleClickInverted = savedInstanceState.getBoolean("isInverted");
        useShuffle = savedInstanceState.getBoolean("shuffle");
        rewindAmount = savedInstanceState.getInt("rewindAmount");
        manageTextViews();
    }

    private void findAll() {
        settingsToolbar = findViewById(R.id.main_toolbar);
        minSeek = findViewById(R.id.min_rewind_seek);
        maxSeek = findViewById(R.id.max_rewind_seek);
        currentSeek = findViewById(R.id.current_rewind_seek);
        rewindSeek = findViewById(R.id.rewind_seekbar);
        shuffleSwitch = findViewById(R.id.shuffle_switch);
        doubleTrackChange = findViewById(R.id.nexttrack_switch);
        doubleRewind = findViewById(R.id.rewind_switch);
    }

    private void addListeners() {
        settingsToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnToMain = new Intent();
                returnToMain.putExtra("inverted", doubleClickInverted);
                returnToMain.putExtra("rewind", rewindAmount * 1000);
                returnToMain.putExtra("shuffle",useShuffle);
                setResult(Activity.RESULT_OK, returnToMain);
                finish();
            }
        });

        doubleTrackChange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doubleRewind.setChecked(!isChecked);
                doubleClickInverted = !isChecked;
            }
        });

        doubleRewind.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                doubleTrackChange.setChecked(!isChecked);
                doubleClickInverted = isChecked;
            }
        });

        shuffleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                useShuffle = isChecked;
            }
        });
    }

    private void manageSeekBar() {
        rewindSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int seekedAmount;

            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
                seekedAmount = progress;

                if (fromUser) {
                    Runnable runnable = new Runnable() {

                        @Override
                        public void run() {
                            int currentPosition = 1+rewindSeek.getProgress();
                            String temp = currentPosition + "";
                            currentSeek.setText(temp);
                            seekBarHandler.postDelayed(this, Constants.FUNCTIONAL.REWIND_SET_REFRESH_DELAY);
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
                rewindAmount = 1+seekedAmount;
            }
        });
    }

    private void manageTextViews() {
        String temp = rewindAmount + "";
        currentSeek.setText(temp);
        temp = 1 + "";
        minSeek.setText(temp);
        temp = 60 + "";
        maxSeek.setText(temp);
    }

    private void getData() {
        Intent fromMain = getIntent();
        rewindAmount = fromMain.getIntExtra("rewind",10);
        doubleClickInverted = fromMain.getBooleanExtra("inverted", false);
        useShuffle = fromMain.getBooleanExtra("shuffle", false);
    }

}

