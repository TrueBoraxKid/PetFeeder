package petfeeder.petfeeder;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class feedClick extends popupWindowClick implements View.OnClickListener{

    private Device dev = Device.getInstance();
    private Context appContext = null;
    private MediaPlayer mp = null;
    private LinearLayout feedLayout = null;

    public feedClick(LayoutInflater inflater, RelativeLayout mainLayout, MainActivity mainActivity, Context appContext, LinearLayout layout){
        super(inflater,R.layout.feed_popup_layout,mainLayout, mainActivity);
        appContext = appContext;
        mp = MediaPlayer.create(appContext, R.raw.feedclick_sound);
        feedLayout = layout;
    }

    @Override
    public void onClick(View v) {
        feedLayout.setBackgroundColor(Color.RED);
        dev.feed();
        mp.start();
    }
}
