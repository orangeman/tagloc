package check.places;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;

import check.places.CellMonitorService.Cell;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.LayoutInflater;
import android.view.View;
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
				
				((TextView)findViewById(R.id.cell)).setText("Cell: "+CellMonitorService.currentCell.cell_id);
				((TextView)findViewById(R.id.signal)).setText("Signal: "+CellMonitorService.currentSignal);
				
				cells = CellMonitorService.cells.values();
				
				for (int i = table.getChildCount(); i < cells.size(); i++) {
					table.addView(LayoutInflater.from(Main.this).inflate(R.layout.cell_row, null));
				}
				
				int i = 0;
				for (Cell cell : cells) {
					View row = table.getChildAt(i++);
					((TextView)row.findViewById(R.id.cell)).setText("Cell: "+cell.cell_id+"   ");
					((TextView)row.findViewById(R.id.signal)).setText("ø Signal: "+(int)cell.signal_avg+"    ");
					((TextView)row.findViewById(R.id.time)).setText("% Zeit: "+cell.getTimePercentage()+"   ");
				}
				
				startService(intent);
				handler.postDelayed(updateUI, 3000);
			}
		};
		
		startService(intent);
		handler.postDelayed(updateUI, 1000);
		
    }
}