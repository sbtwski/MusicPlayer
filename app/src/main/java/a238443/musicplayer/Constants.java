package a238443.musicplayer;

public class Constants {

    public interface ACTION {
        String MAIN_ACTION = "a238443.musicplayer.foregroundservice.action.main";
        String INIT_ACTION = "a238443.musicplayer.foregroundservice.action.init";
        String PREV_ACTION = "a238443.musicplayer.foregroundservice.action.prev";
        String PLAY_ACTION = "a238443.musicplayer.foregroundservice.action.play";
        String NEXT_ACTION = "a238443.musicplayer.foregroundservice.action.next";
        String STARTFOREGROUND_ACTION = "a238443.musicplayer.foregroundservice.action.foregroundstop";
        String STOPFOREGROUND_ACTION = "a238443.musicplayer.foregroundservice.action.foregroundstart";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 50;
    }

    public interface FUNCTIONAL {
        int FORWARD_COOLDOWN = 1500;
        int MUSIC_REFRESH_DELAY = 500;
        int REWIND_SET_REFRESH_DELAY = 100;
    }
}
