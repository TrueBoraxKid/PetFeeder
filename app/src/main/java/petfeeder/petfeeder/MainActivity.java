package petfeeder.petfeeder;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    //TODO: history,
    //TODO: timestamp for Log


    TextView tvLastMessage;
    TextView tvStatus;
    TextView tvShadowDoc;
    TextView tvShadowGet;
    TextView tvLastUpdated;
    TextView tvDeviceStatus;
    TextView tvFeedTime;

    ImageButton btnFeed;
    Button btnCheckContainer;
    Button btnStat;
    Button btnCheckPlate;
    Button btnShadowGet;
    Button btnSetMinBowl;
    Button btnSetMaxBowl;
    ImageView bowlProgressImage;

    LinearLayout layoutFeed;

    FeedTimePicker FeedTimePicker;

    List<View> mainButtons = new ArrayList<>();

    PopupWindow feedPopupWindow;
    PopupWindow checkContainerPopupWindow;
    PopupWindow checkplatePopupWindow;

    RelativeLayout mainLayout;
    Intent statIntent = null;

    AWSManager awsmanager = AWSManager.getInstance();
    MQTTmsg messenger = MQTTmsg.getInstance();
    Device device = Device.getInstance();

    SwipeRefreshLayout swipeLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		
		/**************************************/
        tvLastMessage   = (TextView) findViewById(R.id.tvLastMessage);
        tvStatus        = (TextView) findViewById(R.id.tvStatus);
        tvLastUpdated   = (TextView) findViewById(R.id.tvLastupdated);
        tvShadowDoc     = (TextView) findViewById(R.id.tvShadowDoc);
        tvShadowGet     = (TextView) findViewById(R.id.tvShadowGet);
        tvDeviceStatus  = (TextView) findViewById(R.id.tvDeviceStatus);
        tvFeedTime      = (TextView) findViewById(R.id.tvCurrFeedTime);

        btnFeed 			=(ImageButton) findViewById(R.id.btnFeed);
        btnCheckContainer 	=(Button) findViewById(R.id.btnCheckContainer);
        btnCheckPlate 		=(Button) findViewById(R.id.btnCheckPlate);
        btnStat 			=(Button) findViewById(R.id.btnStat);
        btnShadowGet        =(Button) findViewById(R.id.btnShadowGet);
        btnSetMinBowl       =(Button) findViewById(R.id.btnSetMinBowl);
        btnSetMaxBowl       =(Button) findViewById(R.id.btnSetMaxBowl);

        layoutFeed          =(LinearLayout) findViewById(R.id.feedPane);

        device.setMainActivity(this);
        device.setProgressBarImageView((ImageView) findViewById(R.id.bowlProgress));
        device.setContainerStatusDisplay((TextView) findViewById(R.id.containerStatus));

        FeedTimePicker = new FeedTimePicker();
        FeedTimePicker.setTimeTextView(tvFeedTime);


        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                device.forceUpdateState();
                device.getShadow();
                swipeLayout.setRefreshing(false);
            }
        });

        /****************************************************/
        GraphView graph = (GraphView) findViewById(R.id.bowlStatGraph);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(10);
        graph.getViewport().setXAxisBoundsManual(false);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        graph.addSeries(series);
        device.addGraph(series);
        /****************************************************/


        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        View.OnClickListener checkContainerClick    = new containerClick(inflater,mainLayout, this);
        View.OnClickListener checkPlateClick        = new plateClick(inflater,mainLayout, this);
        View.OnClickListener feedClick              = new feedClick(inflater, mainLayout, this, getApplicationContext(), layoutFeed);
        View.OnClickListener getShadowClick         = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device.getShadow();
            }
        };

        btnFeed.setOnClickListener(feedClick);
        btnShadowGet.setOnClickListener(getShadowClick);
        btnShadowGet.setVisibility(View.INVISIBLE);

        btnCheckContainer.setOnClickListener(checkContainerClick);
        btnCheckPlate.setOnClickListener(checkPlateClick);
        btnStat.setOnClickListener(statClick);

        btnSetMinBowl.setOnClickListener(setMinBowlClick);
        btnSetMaxBowl.setOnClickListener(setMaxBowlClick);

        mainButtons.add(btnCheckContainer);
        mainButtons.add(btnFeed);
        mainButtons.add(btnStat);
        mainButtons.add(btnCheckPlate);

        this.statIntent =  new Intent(this, StatisticsActivity.class);
		/****************************************/
		
		// Initialize aws connection
        try{
			
            awsmanager.init(getApplicationContext(),
                            tvStatus,
                            tvLastMessage,
                            tvShadowDoc,
                            tvShadowGet,
                            tvLastUpdated,
                            tvDeviceStatus,
                            layoutFeed,
                            this);

            awsmanager.connect();
        }catch (Exception e){
            tvStatus.setText("Connection error:" + e);
            tvStatus.setTextColor(Color.RED);
        }
    }

	
	
	/****************************************/

    View.OnClickListener statClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO: New activity, statistics graphical window
            startActivity(statIntent);
        }
    };

    View.OnClickListener setMinBowlClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(getApplicationContext(), "Calibrating min bowl value", Toast.LENGTH_LONG);
            toast.show();
            btnSetMinBowl.setBackgroundColor(Color.RED);
            awsmanager.calibrateMin(btnSetMinBowl);
            try {device.forceUpdateState();} catch (Exception e) {Log.d("AWS_MANAGER","Failed to get state while calibrating");}
            //awsmanager.finishCalibration();
        }
    };

    View.OnClickListener setMaxBowlClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast toast = Toast.makeText(getApplicationContext(), "Calibrating max bowl value", Toast.LENGTH_LONG);
            toast.show();
            btnSetMaxBowl.setBackgroundColor(Color.RED);
            awsmanager.calibrateMax(btnSetMaxBowl);
            try {device.forceUpdateState();} catch (Exception e) {Log.d("AWS_MANAGER","Failed to get state while calibrating");}
            //awsmanager.finishCalibration();
        }
    };
    public void showTimePickerDialog(View v) {
        FeedTimePicker.show(getFragmentManager(), "timePicker");
    }

    public void disableMainButtons(){
        for (View b: mainButtons)
            b.setClickable(false);
    }
    public void enableMainButtons(){
        for (View b: mainButtons)
            b.setClickable(true);
    }
}
