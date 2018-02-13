package com.petfeeder.petfeederapp;

import android.content.Context;
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

	private static final String CHECK_CONTAINER_MSG = "readphoto";
	//private static final String CHECK_CONTAINER_MSG = "checkcontainer";

	//TODO: history, timestamp for incoming msg


	AWSIotClient mIotAndroidClient;
	AWSIotMqttManager mqttManager;
	String clientId;
	String keystorePath;
	String fakeKeystorePath;
	String keystoreName;
	String keystorePassword;
	KeyStore clientKeyStore = null;
	KeyStore fakeClientKeyStore = null;
	String certificateId;

	private Boolean subscribed = false;


	CognitoCachingCredentialsProvider credentialsProvider;

	public void init (Context appContext) {

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
		//keystorePath = Environment.getExternalStorageDirectory()+"keystore";
		//keystorePath = "/storage/emulated/0/keystore";

		File f = new File(keystorePath);

		keystoreName = KEYSTORE_NAME;
		keystorePassword = KEYSTORE_PASSWORD;
		certificateId = CERTIFICATE_ID;
		//AWSIotKeystoreHelper.deleteKeystoreAlias(certificateId,keystorePath,keystoreName,keystorePassword);
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

			//final String certPem = "";

			//final String privKey ="";

			//final String certARN = "arn:aws:iot:us-west-2:862529152389:cert/e6a3fbd302a44645adbbafcf7cfc346f4f6a5918de04b11c5014a3e14cca546e";

			new Thread(new Runnable() {
			//	@Override
			public void run() {
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
			}).start();
		}
	}

	public void connect(AWSIotMqttClientStatusCallback callback){
		mqttManager.connect(clientKeyStore, callback);
	};

	public void publish(String msg){
		try {
			mqttManager.publishString(msg, MQTT_TOPIC_IN, AWSIotMqttQos.QOS0);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Publish error.", e);
		}
	}

	public void subscribe(AWSIotMqttNewMessageCallback callback){
		try {
			if (!subscribed) {
				mqttManager.subscribeToTopic(this.MQTT_TOPIC_OUT, AWSIotMqttQos.QOS0, callback);
				subscribed = true;
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Subscription error.", e);
			subscribed =false;
		}
	};
}