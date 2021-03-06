package petfeeder.petfeeder;

import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MQTTmsg {


    public static final String CHECK_CONTAINER_MSG = "readphoto";
    public static final String CHECK_PLATE_MSG = "readphoto";
    public static final String FEED_MSG = "feed";

    public static final String MQTT_TOPIC_IN = "/in";
    public static final String MQTT_TOPIC_OUT = "/out";
    public static final String MQTT_LIVENESS_TOPIC 	= "/live";


    public static final String SHADOW_GET_TOPIC 		= "$aws/things/esp8266_1D291A/shadow/get";
    public static final String SHADOW_GET_ACK_TOPIC		= "$aws/things/esp8266_1D291A/shadow/get/accepted";
    public static final String SHADOW_UPDATE_TOPIC 		= "$aws/things/esp8266_1D291A/shadow/update";
    public static final String SHADOW_DOCUMENT_TOPIC 	= "$aws/things/esp8266_1D291A/shadow/update/documents";


    //TODO: Messagebox, id matching
    //TODO: Send by topic per device

    static final String LOG_TAG = MQTTmsg.class.getCanonicalName();
    private AWSManager manager = AWSManager.getInstance();
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

    public void sendFormatted(String msg, String topic){
        //TODO: Send by topic per device

        manager.publish(buildMsg(msg).toString(), topic);
    }

    public void send(String msg, String topic){
        manager.publish(msg, topic);
    }

    public void send(JSONObject msg, String topic){
        manager.publish(msg.toString(), topic);
    }
}

