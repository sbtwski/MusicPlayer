package a238443.musicplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.SongHolder> {
    private ArrayList<Song> database = new ArrayList<>();
    private ClickHandler adapterClickHandler;
    private Context appContext;

    class SongHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ClickHandler songClickHandler;
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

    RecyclerAdapter(ClickHandler adapterClickHandler, Context appContext) {
        database = new ArrayList<>();
        this.adapterClickHandler = adapterClickHandler;
        this.appContext = appContext;
    }

    @Override
    public void onBindViewHolder(RecyclerAdapter.SongHolder songHolder, int i) {
        TextView title = songHolder.titleText;
        TextView author = songHolder.authorText;
        TextView length = songHolder.lengthText;

        Song toBind = database.get(i);

        title.setText(toBind.getTitle());
        author.setText(toBind.getAuthor());
        setupLength(toBind);
        length.setText(processLength(toBind));

        songHolder.songClickHandler = adapterClickHandler;
    }

    @Override
    public RecyclerAdapter.SongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_item, parent, false);
        return new SongHolder(view);
    }

    void addItem(final Song newSong) {
        database.add(newSong);
        notifyDataSetChanged();
    }

    Song getItem(int position) {
        return database.get(position);
    }

    @Override
    public int getItemCount() {
        return database.size();
    }

    void addDatabase(ArrayList<Song> database) {
        this.database = database;
        notifyDataSetChanged();
    }

    ArrayList<Song> getDatabase() {
        return database;
    }

    void removeItem(int position) {
        database.remove(position);
        notifyDataSetChanged();
    }

    private void setupLength(Song toSetup) {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        Uri toFile = Uri.parse(toSetup.getFilename());
        metaRetriever.setDataSource(appContext, toFile);

        String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int dur = Integer.parseInt(duration);
        toSetup.setSeconds((dur % 60000) / 1000);
        toSetup.setMinutes(dur / 60000);

        metaRetriever.release();
    }

    private String processLength(Song toProcess) {
        String length = toProcess.getMinutes() + ":";
        if(toProcess.getSeconds() < 10)
            length += "0" + toProcess.getSeconds();
        else
            length += toProcess.getSeconds();

        return length;
    }
}

