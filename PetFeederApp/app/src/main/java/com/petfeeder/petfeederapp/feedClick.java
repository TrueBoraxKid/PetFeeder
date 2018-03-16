package com.petfeeder.petfeederapp;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class feedClick extends popupWindowClick implements View.OnClickListener{

    Device dev = new Device();

    public feedClick(LayoutInflater inflater, RelativeLayout mainLayout, MainActivity mainActivity){
        super(inflater,R.layout.feed_popup_layout,mainLayout, mainActivity);
    }

    @Override
    public void onClick(View v) {
        dev.feed();
    }
}
