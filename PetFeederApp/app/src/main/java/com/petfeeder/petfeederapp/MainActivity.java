package com.petfeeder.petfeederapp;

import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private static final String MQTT_TOPIC_IN = "/in";
    private static final String MQTT_TOPIC_OUT = "/out";


    //TODO: history,
    //TODO: timestamp for Log



    TextView tvLastMessage;
    TextView tvStatus;

    Button btnCheckContainer;
    Button btnFeed;
    List<View> mainButtons = new ArrayList<>();

    PopupWindow feedPopupWindow;

    RelativeLayout mainLayout;

    AWSManager awsmanager = AWSManager.getInstance();
    MQTTmsg messenger = MQTTmsg.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnCheckContainer = (Button) findViewById(R.id.btnCheckContainer);
        btnCheckContainer.setOnClickListener(checkContainerClick);
        mainButtons.add(btnCheckContainer);

        btnFeed =(Button) findViewById(R.id.btnFeed);
        btnFeed.setOnClickListener(feedClick);
        mainButtons.add(btnFeed);


        // Initialize aws connection
        try{
            awsmanager.init(getApplicationContext());
            awsmanager.connect(mqttStatusCallback);
        }catch (Exception e){
            tvStatus.setText("Connection error:" + e);
            tvStatus.setTextColor(Color.RED);
        }
    }

    AWSIotMqttNewMessageCallback newMessageCallback = new AWSIotMqttNewMessageCallback() {

        @Override
        public void onMessageArrived(final String topic, final byte[] data) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = new String(data, "UTF-8");
                        Log.d(LOG_TAG, "Message arrived:");
                        Log.d(LOG_TAG, "   Topic: " + topic);
                        Log.d(LOG_TAG, " Message: " + message);

                        tvLastMessage.setText(message);

                    } catch (UnsupportedEncodingException e) {
                        Log.e(LOG_TAG, "Message encoding error.", e);
                    }
                }
            });
        }
    };

    AWSIotMqttClientStatusCallback mqttStatusCallback = new AWSIotMqttClientStatusCallback() {
        @Override
        public void onStatusChanged(final AWSIotMqttClientStatus status,
                                    final Throwable throwable) {
            Log.d(LOG_TAG, "Status = " + String.valueOf(status));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (status == AWSIotMqttClientStatus.Connecting) {
                        tvStatus.setText("Connecting...");
                        tvStatus.setTextColor(Color.YELLOW);

                    } else if (status == AWSIotMqttClientStatus.Connected) {
                        awsmanager.subscribe(newMessageCallback);
                        tvStatus.setText("Connected");
                        tvStatus.setTextColor(Color.GREEN);

                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                        tvStatus.setText("Reconnecting");
                        tvStatus.setTextColor(Color.YELLOW);
                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                        tvStatus.setText("Disconnected");
                        tvStatus.setTextColor(Color.RED);
                    } else {
                        tvStatus.setText("Disconnected");
                        tvStatus.setTextColor(Color.RED);

                    }
                }
            });
        }
    };

    View.OnClickListener checkContainerClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            awsmanager.publish(messenger.buildMsg(MQTTmsg.CHECK_CONTAINER_MSG).toString());
        }

    };

    View.OnClickListener feedClick = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            View feedPopupView = inflater.inflate(R.layout.feed_popup_layout,null);
            feedPopupWindow = new PopupWindow(feedPopupView, LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            if(Build.VERSION.SDK_INT>=21) feedPopupView.setElevation(5.0f);

            Button btnfeedPopupClose = (Button) feedPopupView.findViewById(R.id.btnPopupClose);
            btnfeedPopupClose.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    feedPopupWindow.dismiss();
                    enableMainButtons();
                }
            });

            feedPopupWindow.showAtLocation(mainLayout, Gravity.CENTER,0,0);
            disableMainButtons();
        }
    };

    private void disableMainButtons(){
        for (View b: mainButtons)
            b.setClickable(false);
    }
    private void enableMainButtons(){
        for (View b: mainButtons)
            b.setClickable(true);
    }
}
