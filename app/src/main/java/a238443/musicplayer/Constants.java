package a238443.musicplayer;

class Constants {

    public interface ACTION {
        String PREV = "act.prev";
        String CHOICE = "act.choice";
        String PLAY_PAUSE = "act.status";
        String NEXT = "act.next";
        String REWIND = "act.rewind";
        String FORWARD = "act.forward";
        String MOVE_DURATION = "act.move";
        String SETTINGS_UPDATE = "act.settings";
        String START_SERVICE = "act.stop";
    }

    public interface EXTRAS {
        String NEW_POSITION = "ext.position";
        String PLAYED_POSITION = "ext.played";
        String SHUFFLE_USE = "ext.shuffle";
        String REWIND_AMOUNT = "ext.rewind";
        String DATABASE = "ext.database";
        String CLICKED_POSITION = "ext.clicked";
        String CURRENT_DURATION = "ext.duration";
        String IS_PLAYING = "ext.playing";
        String FULL_TIME = "ext.full";
        String PROGRESS = "ext.progress";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 50;
    }

    public interface FUNCTIONAL {
        int FORWARD_COOLDOWN = 500;
        int REWIND_MENU_REFRESH_DELAY = 400;
        int MUSIC_REFRESH_DELAY = 800;
        int REWIND_SET_REFRESH_DELAY = 700;
        int UI_UPDATE_REFRESH_DELAY = 800;
        int MINOR_ELEMENTS_REFRESH_DELAY = 75;
        int DURATION_BROADCAST_REFRESH_DELAY = 850;
        int REWIND_AMOUNT = 10000;
        int CHANGE_ATTEMPTS = 2;
    }

    public interface BROADCASTS {
        String BASIC = "brd.basic";
        String TRACK_CHANGE = "brd.track";
        String PLAY_PAUSE = "brd.play";
        String FULLTIME = "brd.fulltime";
    }
}
