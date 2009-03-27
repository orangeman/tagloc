package check.places;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

public class CellMonitorService extends Service {
	
	private long startTime;
	private long lastUpdateTime;

	class Cell {
		int cell_id;
		long time_active;
		double signal_avg;
		
		public Cell(int cid) {
			cell_id = cid;
		}
		public int getTimePercentage() {
			return (int) (((double)time_active / (lastUpdateTime - startTime)) *100);
		}
		public synchronized void update() {
			
			long now = System.currentTimeMillis();
			long timeNew = now - lastUpdateTime;
			signal_avg = ((time_active * signal_avg + timeNew * currentSignal) / (time_active + timeNew));
			time_active = time_active + timeNew;
			lastUpdateTime = now;
		}
	}

	public static HashMap<Integer, Cell> cells;
	public static Cell currentCell;
	public static int currentSignal;


	@Override
	public void onCreate() {
		
		cells = new HashMap<Integer, Cell>();
		
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE); 
		
		class StateChekker extends PhoneStateListener {

			@Override
			public void onCellLocationChanged(CellLocation location) {
				
				if (currentCell != null)
					currentCell.update();
				
				Integer newCellId = new Integer(((GsmCellLocation)location).getCid());
				currentCell = cells.get(newCellId);
				if (currentCell == null) {
					currentCell = new Cell(newCellId);
					cells.put(newCellId, currentCell);
				}
				super.onCellLocationChanged(location);
			}

			@Override
			public void onSignalStrengthChanged(int signal) {
				
				if (currentCell != null && currentSignal != 0)
					currentCell.update();
				currentSignal = signal;
				
				super.onSignalStrengthChanged(signal);
			}
        	
        }
        
        tm.listen(new StateChekker(), PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (currentCell != null && currentSignal != 0)
			currentCell.update();
		super.onStart(intent, startId);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
