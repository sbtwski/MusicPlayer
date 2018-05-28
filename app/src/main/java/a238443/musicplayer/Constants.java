package a238443.musicplayer;

class Constants {

    public interface ACTION {
        String PREV = "action.prev";
        String CHOICE = "action.choice";
        String PLAY_PAUSE = "action.status";
        String NEXT = "action.next";
        String REWIND = "action.rewind";
        String FORWARD = "action.forward";
        String MOVE_DURATION = "action.move";
        String SETTINGS_UPDATE = "action.settings";
        String START_SERVICE = "action.stop";
    }

    public interface EXTRAS {
        String PLAYED_POSITION = "extras.position";
        String SHUFFLE_USE = "extras.shuffle";
        String REWIND_AMOUNT = "extras.rewind";
        String DATABASE = "extras.database";
        String CLICKED_POSITION = "extras.clicked";
        String CURRENT_DURATION = "extras.duration";
        String IS_PLAYING = "extras.playing";
        String FULL_TIME = "extras.full";
        String PROGRESS = "extras.progress";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 50;
    }

    public interface FUNCTIONAL {
        int FORWARD_COOLDOWN = 1500;
        int REWIND_MENU_REFRESH_DELAY = 200;
        int MUSIC_REFRESH_DELAY = 100;
        int REWIND_SET_REFRESH_DELAY = 200;
        int UI_UPDATE_REFRESH_DELAY = 300;
        int REWIND_AMOUNT = 10000;
        int CHANGE_ATTEMPTS = 2;
    }

    public interface BROADCASTS {
        String BASIC = "broadcasts.basic";
        String TRACK_CHANGE = "broadcasts.track";
        String PLAY_PAUSE = "broadcasts.play";
        String FULLTIME = "broadcasts.fulltime";
    }
}
