package com.petfeeder.petfeederapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class feedClick extends popupWindowClick implements View.OnClickListener{

    private Device dev = Device.getInstance();
    private Context appContext = null;
    private MediaPlayer mp = null;

    public feedClick(LayoutInflater inflater, RelativeLayout mainLayout, MainActivity mainActivity, Context appContext){
        super(inflater,R.layout.feed_popup_layout,mainLayout, mainActivity);
        appContext = appContext;
        mp = MediaPlayer.create(appContext, R.raw.feedclick_sound);
    }

    @Override
    public void onClick(View v) {
        dev.feed();
        mp.start();
    }
}
