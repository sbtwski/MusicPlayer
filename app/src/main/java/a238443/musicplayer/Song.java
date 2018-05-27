package a238443.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

class Song implements Parcelable, Serializable{
    private String title;
    private String author;
    private String filename;
    private int minutes, seconds;

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    Song(String title, String author, String filename) {
        this.title = title;
        this.author = author;
        this.filename = filename;
        minutes = 0;
        seconds = 0;
    }

    private Song(Parcel in){
        this.title = in.readString();
        this.author = in.readString();
        this.filename =  in.readString();
        this.minutes = in.readInt();
        this.seconds = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(filename);
        dest.writeInt(minutes);
        dest.writeInt(seconds);
    }

    String getTitle() {
        return title;
    }

    String getAuthor() {
        return author;
    }

    String getFilename() {
        return filename;
    }

    int getMinutes() {
        return minutes;
    }

    int getSeconds() {
        return seconds;
    }

    void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    void setSeconds(int seconds) {
        this.seconds = seconds;
    }
}
