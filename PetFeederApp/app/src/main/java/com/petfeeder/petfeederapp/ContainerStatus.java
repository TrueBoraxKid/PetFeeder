package com.petfeeder.petfeederapp;

import android.graphics.Color;
import android.widget.TextView;


public class ContainerStatus {
    private TextView statusDisplay = null;

    public void setStatus(int status){
        String res = (status == 0) ?  "Full":"Empty";
        int cl   = (status == 0) ? Color.GREEN: Color.RED;
        statusDisplay.setText(res);
        statusDisplay.setTextColor(cl);

    }

    public void setStatusDisplay(TextView tv) {
        this.statusDisplay = tv;
    }
}
