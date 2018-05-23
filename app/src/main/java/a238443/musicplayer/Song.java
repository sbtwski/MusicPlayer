package a238443.musicplayer;

import java.io.Serializable;

class Song implements Serializable{
    private String title;
    private String author;
    private String filename;
    private int minutes, seconds;

    Song(String title, String author, String filename) {
        this.title = title;
        this.author = author;
        this.filename = filename;
        minutes = 0;
        seconds = 0;
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
