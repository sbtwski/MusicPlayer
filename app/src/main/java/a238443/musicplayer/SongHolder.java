package a238443.musicplayer;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SongHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    ClickHandler songClickHandler;
    TextView titleText, authorText, lengthText;
    Button playButton;

    SongHolder(View itemView) {
        super(itemView);

        titleText = itemView.findViewById(R.id.list_title);
        authorText = itemView.findViewById(R.id.list_author);
        lengthText = itemView.findViewById(R.id.list_song_length);
        playButton = itemView.findViewById(R.id.play_pause_list);

        playButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(songClickHandler != null)
            songClickHandler.onButtonClicked(v, getAdapterPosition());
    }
}
