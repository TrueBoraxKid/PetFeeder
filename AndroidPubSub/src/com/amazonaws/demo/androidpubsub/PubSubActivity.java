/**
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * kadkajhs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.androidpubsub;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PubSubActivity extends Activity {

    static final String LOG_TAG = PubSubActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.

    //private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2ic1bnhyemh9f.iot.us-west-2.amazonaws.com";
    //private static final String COGNITO_POOL_ID =  "us-west-2:5a62d66c-e0d0-450e-9b63-77d5698597d3";

    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2ic1bnhyemh9f.iot.us-west-2.amazonaws.com";
    private static final String COGNITO_POOL_ID =  "us-west-2:8850bda9-d1a2-4b86-9548-35b40568e25a";

    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "PetFeeder_policy";


    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_WEST_2;
    // Filename of KeyStore file on the filesystem
    //private static final String KEYSTORE_NAME = "iot_keystore";
    private static final String KEYSTORE_NAME = "new_iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private static final String MQTT_TOPIC_IN = "/in";
    private static final String MQTT_TOPIC_OUT = "/out";
    
	private static final String CHECK_CONTAINER_MSG = "readphoto";
    //private static final String CHECK_CONTAINER_MSG = "checkcontainer";

    //TODO: history, timestamp for incoming msg

	EditText txtSubcribe;
    EditText txtTopic;
    EditText txtMessage;

    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    Button btnSubscribe;
    Button btnPublish;
    Button btnDisconnect;
    Button btnSwitchLed;
	Button btnCheckContainer;


    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //txtSubcribe = (EditText) findViewById(R.id.txtSubcribe);
        //txtTopic = (EditText) findViewById(R.id.txtTopic);
        txtMessage = (EditText) findViewById(R.id.txtMessage);

        tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        //tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);
		
        //btnSubscribe = (Button) findViewById(R.id.btnSubscribe);
        //btnSubscribe.setOnClickListener(subscribeClick);

        btnPublish = (Button) findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(publishClick);

//        btnSwitchLed = (Button) findViewById(R.id.btnSwitchLed);
//        btnSwitchLed.setOnClickListener(switchLedClick);

		btnCheckContainer = (Button) findViewById(R.id.btnCheckContainer);
        btnCheckContainer.setOnClickListener(checkContainerClick);


		//MQTT/AWS related
		try {
            // MQTT client IDs are required to be unique per AWS IoT account.
            // This UUID is "practically unique" but does not _guarantee_
            // uniqueness.
            clientId = UUID.randomUUID().toString();
            //tvClientId.setText(clientId);

            // Initialize the AWS Cognito credentials provider
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(), // context
                    COGNITO_POOL_ID, // Identity Pool ID
                    MY_REGION // Region
            );

            Region region = Region.getRegion(MY_REGION);

            // MQTT Client
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

            keystorePath = getFilesDir().getPath();
            //keystorePath = "/storage/emulated/0/keystore";
            keystoreName = KEYSTORE_NAME;
            keystorePassword = KEYSTORE_PASSWORD;
            certificateId = CERTIFICATE_ID;

            /**
            File f = new File(getFilesDir(), keystoreName);

            File externalDir = new File(Environment.getExternalStorageDirectory(), "keystore");
            externalDir.mkdir();

            File copyKeyStore = new File(externalDir, keystoreName);

            FileChannel src = new FileInputStream(f).getChannel();
            FileChannel dest = new FileOutputStream(copyKeyStore).getChannel();
            dest.transferFrom(src, 0, src.size());
            **/




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
						btnConnect.setEnabled(true);
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
	
				new Thread(new Runnable() {
					@Override
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

                            String certPem = createKeysAndCertificateResult.getCertificatePem();
                            String privKey = createKeysAndCertificateResult.getKeyPair().getPrivateKey();
                            String pubKey = createKeysAndCertificateResult.getKeyPair().getPublicKey();
                            String arn = createKeysAndCertificateResult.getCertificateArn();

							AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
									createKeysAndCertificateResult.getCertificatePem(),
									createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
									keystorePath, keystoreName, keystorePassword);
	                        //AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
							//		certPem,
							//		privKey,
							//		keystorePath, keystoreName, keystorePassword);

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
							policyAttachRequest.setPrincipal(createKeysAndCertificateResult
									.getCertificateArn());

                            policyAttachRequest.setPrincipal(arn);

							mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);
	
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									btnConnect.setEnabled(true);
								}
							});
						} catch (Exception e) {
							Log.e(LOG_TAG,
									"Exception occurred when generating new private key and certificate.",
									e);
						}
					}
				}).start();
			}
		}catch (Exception e) {
			Log.e(LOG_TAG, "Untraced exception", e);
		}
		
		this.autoConnect();
    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                            final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    tvStatus.setText("Connected");

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
        }
    };

    View.OnClickListener subscribeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String topic = txtSubcribe.getText().toString();

            Log.d(LOG_TAG, "topic = " + topic);

            try {
                mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                        new AWSIotMqttNewMessageCallback() {
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
                        });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Subscription error.", e);
            }
        }
    };

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String topic = MQTT_TOPIC_IN;
            final String msg = txtMessage.getText().toString();

            try {
                mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };
	
	View.OnClickListener switchLedClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String msg = "{led switch request}";
			publish(msg);
        }
    };

	View.OnClickListener checkContainerClick = new View.OnClickListener() {
		@Override
        public void onClick(View v) {
			final String msg = CHECK_CONTAINER_MSG;
			publish(msg);
		}
	
	};
	
    private void subscribe(){
        try {
            mqttManager.subscribeToTopic(this.MQTT_TOPIC_OUT, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
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
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    };
	private void autoConnect(){
		Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                            final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    subscribe();
                                    tvStatus.setText("Connected");

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
        };
	private void publish(String msg){
		try {
                mqttManager.publishString(msg, MQTT_TOPIC_IN, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }
	}
	
}