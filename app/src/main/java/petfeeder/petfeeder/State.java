package petfeeder.petfeeder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class State {

    private int value = 0;
    private int timestamp = 0;

    public State(JsonElement element, String key){

        JsonObject state = null;
        JsonObject metadata = null;

        try {
            state = element.getAsJsonObject().getAsJsonObject("current").getAsJsonObject("state").getAsJsonObject("reported");
            metadata = element.getAsJsonObject().getAsJsonObject("current").getAsJsonObject("metadata").getAsJsonObject("reported");
        } catch (NullPointerException e) {
            state = element.getAsJsonObject().getAsJsonObject("state").getAsJsonObject("reported");
            metadata = element.getAsJsonObject().getAsJsonObject("metadata").getAsJsonObject("reported");
        }


        this.value      = Integer.parseInt(state.get(key).toString());
        this.timestamp  = Integer.parseInt(metadata.get(key).getAsJsonObject().get("timestamp").toString());

    }

    public State() {
        this.value      = 0;
        this.timestamp  = (int)(System.currentTimeMillis()/1000);
        //TODO: Check if this is ok
    }

    public int getTimestamp(){
        return timestamp;
    }

    public int getValue(){
        return value;
    }

    public void setValue(int newValue) {
        this.value = newValue;
    }
}
