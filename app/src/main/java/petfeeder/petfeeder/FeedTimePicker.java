package petfeeder.petfeeder;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;

import android.app.DialogFragment;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

public class FeedTimePicker extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private Device device = Device.getInstance();
    private TextView tvTimeText = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        device.setFeedTime(hourOfDay*3600 + minute*60);
        device.updateShadow();
        try{
            if (minute < 10 ) {
                tvTimeText.setText(hourOfDay + " : 0" + minute);
            } else {
                tvTimeText.setText(hourOfDay + " : " + minute);
            }
        }
        catch (Exception e) {
            {
                Log.d("DEVICE","TimePicker exception: \n" + e);}
        }
    }

    public void setTimeTextView (TextView tv) {
        this.tvTimeText = tv;
    }

}
