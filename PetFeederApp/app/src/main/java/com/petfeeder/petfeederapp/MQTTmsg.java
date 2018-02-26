package com.petfeeder.petfeederapp;

import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MQTTmsg {
    static final String LOG_TAG = MQTTmsg.class.getCanonicalName();


    public static final String CHECK_CONTAINER_MSG = "readphoto";
    //private static final String CHECK_CONTAINER_MSG = "checkcontainer";



    private static MQTTmsg messenger = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yy-hh-mm-ss");

    private MQTTmsg() {
    }

    public static MQTTmsg getInstance() {
        if (messenger == null) {
            messenger = new MQTTmsg();
        }
        return messenger;
    }

    public JSONObject buildMsg(String payload) {
        JSONObject msg = new JSONObject();
        Date d = new Date();
        String timestamp = simpleDateFormat.format(new Date());

        try {
            msg.put("id", d.getTime());
            msg.put("payload", payload);
            msg.put("timestamp", timestamp);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Message builder exception:" + e);
        }
        return msg;
    }
}

