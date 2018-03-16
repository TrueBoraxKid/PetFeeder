package com.petfeeder.petfeederapp;

import android.content.Intent;
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

    //TODO: history,
    //TODO: timestamp for Log


    TextView tvLastMessage;
    TextView tvStatus;

    Button btnCheckContainer;
    Button btnFeed;
    Button btnStat;
    Button btnCheckPlate;

    List<View> mainButtons = new ArrayList<>();

    PopupWindow feedPopupWindow;
    PopupWindow checkContainerPopupWindow;
    PopupWindow checkplatePopupWindow;

    RelativeLayout mainLayout;
    Intent statIntent = null;

    AWSManager awsmanager = AWSManager.getInstance();
    MQTTmsg messenger = MQTTmsg.getInstance();
    Device device = new Device();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		
		/**************************************/
        tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnFeed 			=(Button) findViewById(R.id.btnFeed);
        btnCheckContainer 	=(Button) findViewById(R.id.btnCheckContainer);
        btnCheckPlate 		=(Button) findViewById(R.id.btnCheckPlate);
        btnStat 			=(Button) findViewById(R.id.btnStat);


        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        View.OnClickListener checkContainerClick    = new containerClick(inflater,mainLayout, this);
        View.OnClickListener checkPlateClick        = new plateClick(inflater,mainLayout, this);
        View.OnClickListener feedClick              = new feedClick(inflater, mainLayout, this);

        btnFeed.setOnClickListener(feedClick);
        btnCheckContainer.setOnClickListener(checkContainerClick);
        btnCheckPlate.setOnClickListener(checkPlateClick);
        btnStat.setOnClickListener(statClick);

        mainButtons.add(btnCheckContainer);
        mainButtons.add(btnFeed);
        mainButtons.add(btnStat);
        mainButtons.add(btnCheckPlate);

        this.statIntent =  new Intent(this, StatisticsActivity.class);
		/****************************************/
		
		// Initialize aws connection
        try{
			
            awsmanager.init(getApplicationContext(), tvStatus, tvLastMessage, this);
            awsmanager.connect();
        }catch (Exception e){
            tvStatus.setText("Connection error:" + e);
            tvStatus.setTextColor(Color.RED);
        }
    }

	
	
	/****************************************/

    View.OnClickListener statClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: New activity, statistics graphical window
            startActivity(statIntent);
        }
    };

    public void disableMainButtons(){
        for (View b: mainButtons)
            b.setClickable(false);
    }
    public void enableMainButtons(){
        for (View b: mainButtons)
            b.setClickable(true);
    }
}
