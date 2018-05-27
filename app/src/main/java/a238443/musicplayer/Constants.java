package a238443.musicplayer;

class Constants {

    public interface ACTION {
        String PREV = "a238443.musicplayer.musicservice.action.prev";
        String CHOICE = "a238443.musicplayer.musicservice.action.choice";
        String PLAY_PAUSE = "a238443.musicplayer.musicservice.broadcasts.status";
        String NEXT = "a238443.musicplayer.musicservice.action.next";
        String REWIND = "a238443.musicplayer.musicservice.action.rewind";
        String FORWARD = "a238443.musicplayer.musicservice.action.forward";
        String MOVE_DURATION = "a238443.musicplayer.musicservice.action.move";
        String SETTINGS_UPDATE = "a238443.musicplayer.musicservice.action.settings";
        String START_SERVICE = "a238443.musicplayer.musicservice.action.stop";
        String STOP_SERVICE = "a238443.musicplayer.musicservice.action.start";
    }

    public interface EXTRAS {
        String PLAYED_POSITION = "a238443.musicplayer.musicservice.extras.position";
        String SHUFFLE_USE = "a238443.musicplayer.musicservice.extras.shuffle";
        String REWIND_AMOUNT = "a238443.musicplayer.musicservice.extras.rewind";
        String DATABASE = "a238443.musicplayer.musicservice.extras.database";
        String CLICKED_POSITION = "a238443.musicplayer.musicservice.extras.clicked";

        String CURRENT_DURATION = "a238443.musicplayer.musicservice.extras.duration";

        String IS_PLAYING = "a238443.musicplayer.musicservice.extras.playing";

        String FULL_TIME = "a238443.musicplayer.musicservice.extras.full";

        String PROGRESS = "a238443.musicplayer.musicservice.extras.progress";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 50;
    }

    public interface FUNCTIONAL {
        int FORWARD_COOLDOWN = 1500;
        int MUSIC_REFRESH_DELAY = 100;
        int REWIND_SET_REFRESH_DELAY = 100;
    }

    public interface BROADCASTS {
        String BASIC = "a238443.musicplayer.musicservice.broadcasts.basic";
        String TRACK_CHANGE = "a238443.musicplayer.musicservice.broadcasts.track";
        String PLAY_PAUSE = "a238443.musicplayer.musicservice.broadcasts.play";
        String FULLTIME = "a238443.musicplayer.musicservice.broadcasts.fulltime";
    }
}
