package com.petfeeder.petfeederapp;


import java.util.Dictionary;

public class Device {

    private MQTTmsg messenger = MQTTmsg.getInstance();

    public void checkContainer(){
        messenger.send(MQTTmsg.CHECK_CONTAINER_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void checkPlate(){
        messenger.send(MQTTmsg.CHECK_PLATE_MSG, MQTTmsg.MQTT_TOPIC_IN);
    };

    public void feed(){
        messenger.send(MQTTmsg.FEED_MSG, MQTTmsg.MQTT_TOPIC_FEED);
    };

}
