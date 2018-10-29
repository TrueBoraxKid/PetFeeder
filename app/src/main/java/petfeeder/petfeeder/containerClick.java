package petfeeder.petfeeder;


import android.view.LayoutInflater;
import android.widget.RelativeLayout;

public class containerClick extends popupWindowClick  {

    public containerClick(LayoutInflater inflater, RelativeLayout mainLayout, MainActivity mainActivity){
        super(inflater,R.layout.container_popup_layout,mainLayout, mainActivity);
    }
}
