package petfeeder.petfeeder;

import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class popupWindowClick implements View.OnClickListener {

    private LayoutInflater inflater;
    private PopupWindow window;
    private int popupLayout;
    private RelativeLayout mainLayout;
    private MainActivity mainActivity;
    private View view;

    private Button btnPopupClose;

    public popupWindowClick (LayoutInflater inflater, int popupLayout, RelativeLayout mainLayout, MainActivity mainActivity) {
        super();
        this.inflater = inflater;
        this.popupLayout = popupLayout;
        this.mainActivity = mainActivity;
        this.mainLayout = mainLayout;

        view = inflater.inflate(popupLayout,null);
        window = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(Build.VERSION.SDK_INT>=21) view.setElevation(5.0f);

        btnPopupClose = (Button) view.findViewById(R.id.btnPopupClose);
        btnPopupClose.setOnClickListener(popupCloseListener);
    }

    @Override
    public void onClick(View v) {

        window.showAtLocation(mainLayout, Gravity.CENTER,0,0);
        mainActivity.disableMainButtons();
    }


    View.OnClickListener popupCloseListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            window.dismiss();
            mainActivity.enableMainButtons();
        }
    };

};

