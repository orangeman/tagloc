package check.places;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;

import check.places.CellMonitorService.Cell;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

public class Main extends Activity {
    private static final String MY_NOTIFICATION_ID = "mhh";
	private PhoneStateListener mPhoneStateReceiver;
	private TextView cell;
	private TextView signal;
	private TelephonyManager tm;
	private TextView lac;
	
	private Runnable updateUI;
	private Handler handler;
	private Intent intent;
	private TableLayout table;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        table = (TableLayout) findViewById(R.id.cells);
        cell = (TextView) findViewById(R.id.cell);
        signal = (TextView) findViewById(R.id.signal);
        
        intent = new Intent(this, CellMonitorService.class);
        handler = new Handler();
        updateUI = new Runnable() {

			private Collection<Cell> cells;
			
			@Override
			public void run() {
				if (CellMonitorService.currentCell == null) {
					handler.postDelayed(updateUI, 500);
					return;
				}
				setText2Fit((TextView)findViewById(R.id.cell), "Cell: "+CellMonitorService.currentCell.cell_id);
				setText2Fit((TextView)findViewById(R.id.signal), "   signal: "+CellMonitorService.currentSignal+"   ");
				setText2Fit((TextView)findViewById(R.id.time), "running for "+(int)(System.currentTimeMillis()-CellMonitorService.startTime)/60000+" min. " +
																"Stats collected since "+new SimpleDateFormat("HH:mm").format(new Date(CellMonitorService.startTime)));
				
				cells = CellMonitorService.cells.values();
				
				for (int i = table.getChildCount(); i < cells.size()+1; i++) {
					table.addView(LayoutInflater.from(Main.this).inflate(R.layout.cell_row, null));
				}
				
				int i = 1;
				for (Cell cell : cells) {
					View row = table.getChildAt(i++);
					switch (cell.state) {
					case Cell.STATE_ACTIVE:
						row.setBackgroundColor(Color.RED);
						break;
					case Cell.STATE_GONE:
						row.setBackgroundColor(Color.DKGRAY);
						break;
					case Cell.STATE_VISIBLE:
						row.setBackgroundColor(Color.GRAY);
						break;
					}
					((TextView)row.findViewById(R.id.cell)).setText(""+cell.cell_id);
					((TextView)row.findViewById(R.id.signal)).setText("     "+(int)cell.signal_avg);
					((TextView)row.findViewById(R.id.active)).setText("     "+cell.getActiveTimePercentage());
					((TextView)row.findViewById(R.id.visible)).setText("     "+cell.getVisibleTimePercentage());
				}
				
//				startService(intent);
				handler.postDelayed(updateUI, 3000);
			}
		};
		
		startService(intent);
		handler.post(updateUI);
		
    }
    
    private void setText2Fit(TextView view, String text) {
		float factor = (((ViewGroup)view.getParent()).getWidth()-42) / view.getPaint().measureText(text);
		view.setTextSize(view.getTextSize()*factor);
        view.setText(text);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("stop");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		stopService(intent);
		finish();
		return super.onOptionsItemSelected(item);
	}
	
    
}