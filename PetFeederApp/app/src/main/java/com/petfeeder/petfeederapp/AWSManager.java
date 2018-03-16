package com.petfeeder.petfeederapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.UUID;

public class AWSManager{
	static final String LOG_TAG = AWSManager.class.getCanonicalName();

	private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2ic1bnhyemh9f.iot.us-west-2.amazonaws.com";
	private static final String COGNITO_POOL_ID =  "us-west-2:8850bda9-d1a2-4b86-9548-35b40568e25a";
	private static final String AWS_IOT_POLICY_NAME = "PetFeeder_policy";
	private static final Regions MY_REGION = Regions.US_WEST_2;
	private static final String KEYSTORE_NAME = "iot_keystore";
	private static final String KEYSTORE_PASSWORD = "password";
	private static final String CERTIFICATE_ID = "default";

	private static final String MQTT_TOPIC_IN = "/in";
	private static final String MQTT_TOPIC_OUT = "/out";


	AWSIotClient mIotAndroidClient;
	AWSIotMqttManager mqttManager;
	CognitoCachingCredentialsProvider credentialsProvider;
	String clientId;
	String keystorePath;
	String keystoreName;
	String keystorePassword;
	String certificateId;

	KeyStore clientKeyStore = null;
	

	private Activity mainActivity = null;

	private TextView tvStatus = null;
	private TextView tvLastMessage = null;
	
	private Boolean subscribed = false;
	private Boolean connected = false;

	private static AWSManager manager = null;
	private AWSManager(){};

	public static AWSManager getInstance(){
		if(manager == null){
			manager = new AWSManager();
		}
		return manager;
	}

	public void init (Context appContext, TextView statusView, TextView lastMsgView, Activity mainActivity) {

		// views used by callbacks 
		tvStatus = statusView;
		tvLastMessage = lastMsgView;
		this.mainActivity = mainActivity;
		//
		
		clientId = UUID.randomUUID().toString();
		credentialsProvider = new CognitoCachingCredentialsProvider(
				appContext, // context
				COGNITO_POOL_ID, // Identity Pool ID
				MY_REGION // Region
		);

		Region region = Region.getRegion(MY_REGION);
		mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

		// Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
		// MQTT pings every 10 seconds.
		mqttManager.setKeepAlive(10);

		// Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
		// AWS IoT will publish this message to alert other clients.
		AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
				"Android client lost connection", AWSIotMqttQos.QOS0);
		mqttManager.setMqttLastWillAndTestament(lwt);

		// IoT Client (for creation of certificate if needed)
		mIotAndroidClient = new AWSIotClient(credentialsProvider);
		mIotAndroidClient.setRegion(region);

		keystorePath = appContext.getFilesDir().getPath();

		keystoreName = KEYSTORE_NAME;
		keystorePassword = KEYSTORE_PASSWORD;
		certificateId = CERTIFICATE_ID;

		// To load cert/key from keystore on filesystem
		try {
			if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
				if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
						keystoreName, keystorePassword)) {
					Log.i(LOG_TAG, "Certificate " + certificateId
							+ " found in keystore - using for MQTT.");
					// load keystore from file into memory to pass on connection
					clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
							keystorePath, keystoreName, keystorePassword);
				} else {
					Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
				}
			} else {
					Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
		}

		if (clientKeyStore == null) {
			Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");


			try {
						// Create a new private key and certificate. This call
						// creates both on the server and returns them to the
						// device.

						CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
								new CreateKeysAndCertificateRequest();
						createKeysAndCertificateRequest.setSetAsActive(true);
						final CreateKeysAndCertificateResult createKeysAndCertificateResult;
						createKeysAndCertificateResult =
								mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
						Log.i(LOG_TAG,
								"Cert ID: " +
										createKeysAndCertificateResult.getCertificateId() +
										" created.");

						// store in keystore for use in MQTT client
						// saved as alias "default" so a new certificate isn't
						// generated each run of this application

						AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
								createKeysAndCertificateResult.getCertificatePem(),
								createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
								keystorePath, keystoreName, keystorePassword);

						// load keystore from file into memory to pass on
						// connection
						clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
								keystorePath, keystoreName, keystorePassword);

						// Attach a policy to the newly created certificate.
						// This flow assumes the policy was already created in
						// AWS IoT and we are now just attaching it to the
						// certificate.
						AttachPrincipalPolicyRequest policyAttachRequest =
								new AttachPrincipalPolicyRequest();
						policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
						policyAttachRequest.setPrincipal(createKeysAndCertificateResult.getCertificateArn());
						mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

			} catch (Exception e) {
				Log.e(LOG_TAG,
					"Exception occurred when generating new private key and certificate.",
						e);
			}
		}
	}

	public void connect(){
		mqttManager.connect(clientKeyStore, mqttStatusCallback);
	};

	public void publish(String msg, final String topic){
		try {
			mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
			Log.d(LOG_TAG, " Sending message: ");
			Log.d(LOG_TAG, " Message: " + msg);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Publish error.", e);
		}
	}

	public void subscribe(){
		try {
			if (!subscribed) {
				mqttManager.subscribeToTopic(this.MQTT_TOPIC_OUT, AWSIotMqttQos.QOS0, newMessageCallback);
				subscribed = true;
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Subscription error.", e);
			subscribed = false;
		}
	};
	
    private AWSIotMqttNewMessageCallback newMessageCallback = new AWSIotMqttNewMessageCallback() {
		
       @Override
       public void onMessageArrived(final String topic, final byte[] data) {

           mainActivity.runOnUiThread(new Runnable() {
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

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (status == AWSIotMqttClientStatus.Connecting) {
                        tvStatus.setText("Connecting...");
                        tvStatus.setTextColor(Color.YELLOW);
						connected = false;

                    } else if (status == AWSIotMqttClientStatus.Connected) {
                        subscribe();
                        tvStatus.setText("Connected");
                        tvStatus.setTextColor(Color.GREEN);
                        connected = true;

                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                        tvStatus.setText("Reconnecting");
                        tvStatus.setTextColor(Color.YELLOW);
						connected = false;
                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                        tvStatus.setText("Disconnected");
                        tvStatus.setTextColor(Color.RED);
						connected = false;;
                    } else {
                        tvStatus.setText("Disconnected");
                        tvStatus.setTextColor(Color.RED);
						connected = false;
                    }
                }
            });
        }
    };
}