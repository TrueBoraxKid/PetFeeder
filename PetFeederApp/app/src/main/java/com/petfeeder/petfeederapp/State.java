package com.petfeeder.petfeederapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class State {

    private int value = 0;
    private int timestamp = 0;

    public State(JsonElement element, String key){

        JsonObject state 	= element.getAsJsonObject().getAsJsonObject("state").getAsJsonObject("reported");
        JsonObject metadata = element.getAsJsonObject().getAsJsonObject("metadata").getAsJsonObject("reported");

        this.value      = Integer.parseInt(state.get(key).toString());
        this.timestamp  = Integer.parseInt(metadata.get(key).getAsJsonObject().get("timestamp").toString());

    }

    public int getTimestamp(){
        return timestamp;
    }

    public int getValue(){
        return value;
    }
}
