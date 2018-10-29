package petfeeder.petfeeder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static java.lang.System.currentTimeMillis;

public class AWSManager {
	static final String LOG_TAG = AWSManager.class.getCanonicalName();

	private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2ic1bnhyemh9f.iot.us-west-2.amazonaws.com";
	private static final String COGNITO_POOL_ID =  "us-west-2:8850bda9-d1a2-4b86-9548-35b40568e25a";
	private static final String AWS_IOT_POLICY_NAME = "PetFeeder_policy";

	//private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2ic1bnhyemh9f.iot.us-west-2.amazonaws.com";
	//private static final String COGNITO_POOL_ID =  "us-west-2:b58f1851-286b-4e33-870a-463692bc85c1";
	//private static final String AWS_IOT_POLICY_NAME = "PetFeeder_take_2_policy";

	private static final Regions MY_REGION = Regions.US_WEST_2;

	private static final String KEYSTORE_NAME = "iot_keystore";
	private static final String KEYSTORE_PASSWORD = "password";
	private static final String CERTIFICATE_ID = "default";

	public static final String MQTT_TOPIC_IN = "/in";
	public static final String MQTT_TOPIC_OUT = "/out";
	public static final String MQTT_LIVENESS_TOPIC 	= "/live";

	public static final String SHADOW_GET_TOPIC 		= "$aws/things/esp8266_1D291A/shadow/get";
	public static final String SHADOW_GET_ACK_TOPIC		= "$aws/things/esp8266_1D291A/shadow/get/accepted";
	public static final String SHADOW_UPDATE_TOPIC 		= "$aws/things/esp8266_1D291A/shadow/update";
	public static final String SHADOW_DOCUMENT_TOPIC 	= "$aws/things/esp8266_1D291A/shadow/update/documents";

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
	private Context mainContext = null;
	private TextView tvStatus = null;
	private TextView tvLastMessage = null;
	private TextView tvShadowDoc = null;
	private TextView tvShadowGet = null;
	private TextView tvLastUpdated = null;
    private TextView tvDeviceStatus = null;
	private Button btnSetMax = null;
    private Button btnSetMin = null;
    private long lastBeep = 0;
    private LinearLayout feedLayout = null;

	private Boolean subscribed = false;
	private Boolean connected = false;
	private int calibration = 0;

	private static AWSManager manager = null;
	private AWSManager(){};

	public static AWSManager getInstance(){
		if(manager == null){
			manager = new AWSManager();
		}
		return manager;
	}

	public void init (Context appContext,
					  TextView statusView,
					  TextView lastMsgView,
					  TextView shadowDoc,
					  TextView shadowGet,
					  TextView lastUpdated,
					  TextView deviceStatus,
					  LinearLayout fl,
					  Activity mainActivity
	)
	{

		// views used by callbacks 
		mainContext = appContext;
		tvStatus = statusView;
		tvLastMessage = lastMsgView;
		tvShadowDoc = shadowDoc;
		tvShadowGet = shadowGet;
		tvLastUpdated = lastUpdated;
		tvDeviceStatus = deviceStatus;
		feedLayout = fl;
		this.mainActivity = mainActivity;

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
									  @Override
									  public void run() {
										  long current_time = System.currentTimeMillis();
										  long delta = current_time - lastBeep;

										  if (delta >= 10* 1000) {
											  tvDeviceStatus.setText("Not responding");
											  tvDeviceStatus.setTextColor(Color.YELLOW);
										  }

										  if (delta >= 30* 1000) {
											  tvDeviceStatus.setText("Lost");
											  tvDeviceStatus.setTextColor(Color.RED);
										  }

									  }
								  },
				0, 1000);

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
				mqttManager.subscribeToTopic(this.SHADOW_DOCUMENT_TOPIC, AWSIotMqttQos.QOS0, shadowDocCallback);
				mqttManager.subscribeToTopic(this.SHADOW_GET_ACK_TOPIC, AWSIotMqttQos.QOS0, shadowGetAckCallback);
                mqttManager.subscribeToTopic(this.MQTT_LIVENESS_TOPIC, AWSIotMqttQos.QOS0, livenessHandler);

				subscribed = true;
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Subscription error.", e);
			subscribed = false;
		}
	};

    private AWSIotMqttNewMessageCallback livenessHandler = new AWSIotMqttNewMessageCallback() {

        @Override
        public void onMessageArrived(final String topic, final byte[] data) {

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = new String(data, "UTF-8");
                        if (message.equals("beep")){
                            lastBeep = System.currentTimeMillis();
                            tvDeviceStatus.setText("Alive");
                            tvDeviceStatus.setTextColor(Color.GREEN);
                        }

                        //TODO: Feeding ack
						if (message.equals("feed_ack")) {
							Toast toast = Toast.makeText(mainContext, "Received feed ack", Toast.LENGTH_LONG);
							toast.show();
							feedLayout.setBackgroundColor(Color.WHITE);
						}

                    } catch (UnsupportedEncodingException e) {
                        Log.e(LOG_TAG, "Message encoding error.", e);
                    }
                }
            });
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

	private AWSIotMqttNewMessageCallback shadowDocCallback = new AWSIotMqttNewMessageCallback() {

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

						proccessNewState(message);

					} catch (UnsupportedEncodingException e) {
						Log.e(LOG_TAG, "Message encoding error.", e);
					}
				}
			});
		}
	};

	private AWSIotMqttNewMessageCallback shadowGetAckCallback = new AWSIotMqttNewMessageCallback() {

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

						proccessNewState(message);

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

	private void proccessNewState (String message) {
		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement el = parser.parse(message);
		String jsonString = gson.toJson(el);

		State container = new State(el,"container");
		State bowl = new State(el, "adc");
		State max_adc = new State(el,"max_adc");
		State min_adc = new State(el,"min_adc");
		State feed_time = new State(el,"feed_time");

		Device device = Device.getInstance();

		if (calibration == 0) {
			device.addBowlState(bowl);
			device.addContainerState(container);
		} else if (calibration == 1) {
			device.setMinBowl(bowl.getValue());
			btnSetMin.setBackgroundColor(Color.GREEN);
			Toast toast = Toast.makeText(mainContext, "Finished calibrating min bowl value", Toast.LENGTH_LONG);
			toast.show();
			finishCalibration();
		} else {
			device.setMaxBowl(bowl.getValue());
            btnSetMax.setBackgroundColor(Color.GREEN);
			Toast toast = Toast.makeText(mainContext, "Finished calibrating min bowl value", Toast.LENGTH_LONG);
			toast.show();
			finishCalibration();
		}

		Date date = new Date(bowl.getTimestamp()*1000L);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, d MMM yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+3"));
		String lastUpdateTimestamp = sdf.format(date);


		tvLastUpdated.setText("Last updated: " + lastUpdateTimestamp);
		tvShadowGet.setText(jsonString);
		tvShadowGet.setText("Photoread: "		+ container.getValue()	+	"\t time: " + container.getTimestamp()+
							"\n ADC: "			+ bowl.getValue()		+	"\t time: " + bowl.getTimestamp() +
							"\n MAX BOWL:" 		+ max_adc.getValue()	+	"\t time: " + max_adc.getTimestamp() +
							"\n MIN BOWL:" 		+ min_adc.getValue()	+	"\t time: " + min_adc.getTimestamp() +
							"\n FEED TIME:"		+ feed_time.getValue()	+	"\t time: " + feed_time.getTimestamp()
								);
	};

	public void calibrateMin(Button btn) {
		this.calibration = 1;
		btnSetMin = btn;
	};

	public void calibrateMax(Button btn) {
		this.calibration = 2;
		btnSetMax = btn;
	};

	public void finishCalibration() {
		Device device = Device.getInstance();
		this.calibration = 0;
		device.updateShadow();
	};
}