package petfeeder.petfeeder;


import android.app.Activity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class Device {

    private MQTTmsg messenger = MQTTmsg.getInstance();
    private AWSManager awsManager = AWSManager.getInstance();
    private Activity mainActivity = null;
    private static Device instance;
    private Device(){};

    private List<State> containerStates = new ArrayList<>();
    private List<State> bowlStates      = new ArrayList<>();
    private State minBowlState          = new State();
    private State maxBowlState          = new State();
    private State feedTimeState         = new State();
    private LineGraphSeries<DataPoint> graphSeries = null;
    private int graphX                  = 1;


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
        int minBowl = minBowlState.getValue();
        int maxBowl = maxBowlState.getValue();
        int relativeADC = adcRead - minBowl;
        if (relativeADC < 0 ) {relativeADC = 0;}
        int bowlRange = maxBowl - minBowl;
        int progress = 0;
        try {progress = (int) (100*relativeADC/bowlRange) / 10;} catch (Exception e) {Log.d("DEVICE", "Exception " + e);}
        try {this.progressBar.setProgress(progress);} catch (Exception e) {Log.d("DEVICE","Progress updateing faliure" + e);}
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
        try {
            int minBowl = minBowlState.getValue();
            int maxBowl = maxBowlState.getValue();
            int relativeADC = bowlStates.get(bowlStates.size() - 1).getValue() - minBowl;
            if (relativeADC < 0 ) {relativeADC = 0;}
            int bowlRange = maxBowl - minBowl;
            int progress = 0;
            try {progress = (int) (100*relativeADC/bowlRange) / 10;} catch (Exception e) {Log.d("DEVICE", "Exception " + e);}
            DataPoint point= new DataPoint(graphX,progress);
            graphSeries.appendData(point,false,50);
            graphX += 1;
        } catch (Exception e) {
            Log.d("DEVICE",e.toString());
        }
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

    public void  setMinBowl (int adcRead){
        Log.d("DEVICE", "Setting min value for bowl");
        this.minBowlState.setValue(adcRead);
    };
    public void  setMaxBowl (int adcRead){
        Log.d("DEVICE", "Setting max value for bowl");
        this.maxBowlState.setValue(adcRead);
    };

    public void checkContainer(){
        messenger.send(MQTTmsg.CHECK_CONTAINER_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void setFeedTime (int feedTime) {
        this.feedTimeState.setValue(feedTime);
    }

    public void checkPlate(){
        messenger.send(MQTTmsg.CHECK_PLATE_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void feed(){
        messenger.send(MQTTmsg.FEED_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void getShadow() {messenger.send("",MQTTmsg.SHADOW_GET_TOPIC);}

    public void forceUpdateState() {messenger.send("update_shadow",MQTTmsg.MQTT_TOPIC_IN);}

    public void updateShadow(){
        String msg = "{\"state\": {\"reported\": {\"max_adc\":"+maxBowlState.getValue()+",\"min_adc\":"+minBowlState.getValue()+",\"feed_time\":"+feedTimeState.getValue()+"}}}";
        messenger.send(msg,MQTTmsg.SHADOW_UPDATE_TOPIC);
    }

    public void addGraph (LineGraphSeries<DataPoint> series) {
        this.graphSeries = series;
    }

}
