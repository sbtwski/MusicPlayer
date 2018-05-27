package a238443.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(action != null) {
            if (action.equals(Constants.ACTION.PLAY_PAUSE))
                universalBroadcast(context,Constants.ACTION.PLAY_PAUSE);
            else if (action.equals(Constants.ACTION.NEXT)) {
                universalBroadcast(context,Constants.ACTION.NEXT);
            }
            else if (action.equals(Constants.ACTION.PREV)) {
                Log.d("PREV", "INPREV");
                universalBroadcast(context,Constants.ACTION.PREV);
            }
        }
    }

    private void universalBroadcast(Context context, String broadcastCode) {
        Intent service = new Intent(context, MusicService.class);
        service.setAction(broadcastCode);
        ContextCompat.startForegroundService(context,service);
    }
}
