package com.petfeeder.petfeederapp;


import java.util.Dictionary;

public class Device {

    private AWSManager awsManager = AWSManager.getInstance();

    /**
     * Device state
     */
    private Boolean container = false;


    public void checkContainer(){};
    public void checkPlate(){};
    public void feed(){};


    public void updateState(){

    }

}
