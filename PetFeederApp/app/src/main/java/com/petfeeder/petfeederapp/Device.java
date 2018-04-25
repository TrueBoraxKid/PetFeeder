package com.petfeeder.petfeederapp;


import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

public class Device {

    private MQTTmsg messenger = MQTTmsg.getInstance();
    private Activity mainActivity = null;
    private static Device instance;
    private Device(){};

    private List<State> containerStates = new ArrayList<>();
    private List<State> bowlStates      = new ArrayList<>();

    private BowlProgressBar progressBar = new BowlProgressBar();
    private ContainerStatus containerStatus = new ContainerStatus();

    public void setContainerStatusDisplay(TextView tv){
        containerStatus.setStatusDisplay(tv);
    }

    public void setMainActivity(Activity mainActivity){
        this.mainActivity = mainActivity;
    }

    public void setProgressBarImageView(ImageView image){
        this.progressBar.setBowlProgressImage(image);
    }

    public void setProgressBar(int adcRead){
        //TODO: Correct translation. Callibrate
        this.progressBar.setProgress(adcRead % 10);
    }

    public void setContainerStatus(int photo){
        containerStatus.setStatus(photo);
    }

    public static Device getInstance() {
        if (instance == null) {
            instance = new Device();
        }
        return instance;
    }

    public void addContainerState(State state){
        addNewState(containerStates,state);
        setContainerStatus(containerStates.get(containerStates.size()-1).getValue());
    }

    public void addBowlState(State state){
        addNewState(bowlStates, state);
        setProgressBar(bowlStates.get(bowlStates.size()-1).getValue());
    }

    private void addNewState(List<State> states, State state){
        if (!states.isEmpty()){
            State last = states.get(states.size()-1);
            if (last.getTimestamp() == state.getTimestamp()) {
                Log.d("DEVICE","Duplicated state. Skipping...");
                return;
            }
            states.add(state);
            Log.d("DEVICE","Adding new state...");
            return;
        }
        Log.d("DEVICE","Adding new state...");
        states.add(state);
    }

    public void checkContainer(){
        messenger.send(MQTTmsg.CHECK_CONTAINER_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void checkPlate(){
        messenger.send(MQTTmsg.CHECK_PLATE_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void feed(){
        messenger.send(MQTTmsg.FEED_MSG, MQTTmsg.MQTT_TOPIC_FEED);
    };

    public void getShadow() {messenger.send("",MQTTmsg.SHADOW_GET_TOPIC);}
}
