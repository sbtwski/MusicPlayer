package a238443.musicplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;

public class AboutActivity extends AppCompatActivity{
    Toolbar aboutToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_about);

        findAll();
        aboutToolbar.setNavigationIcon(R.drawable.ic_back);
        addListeners();
    }

    private void findAll() {
        aboutToolbar = findViewById(R.id.main_toolbar);
    }

    private void addListeners() {
        aboutToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
