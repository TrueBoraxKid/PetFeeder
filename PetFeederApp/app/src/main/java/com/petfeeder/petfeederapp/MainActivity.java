package com.petfeeder.petfeederapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import java.util.UUID;



public class MainActivity extends AppCompatActivity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private static final String MQTT_TOPIC_IN = "/in";
    private static final String MQTT_TOPIC_OUT = "/out";

    private static final String CHECK_CONTAINER_MSG = "readphoto";
    //private static final String CHECK_CONTAINER_MSG = "checkcontainer";

    //TODO: history,
    //TODO: timestamp for Log

    TextView tvLastMessage;
    TextView tvStatus;

    Button btnCheckContainer;


    AWSManager awsmanager = new AWSManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnCheckContainer = (Button) findViewById(R.id.btnCheckContainer);
        btnCheckContainer.setOnClickListener(checkContainerClick);

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
            awsmanager.publish(CHECK_CONTAINER_MSG);
        }

    };

}
