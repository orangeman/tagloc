package check.places;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class CellMonitorService extends Service {
	
	private Handler handler;
	private Runnable update;

	class Cell {
		
		static final int STATE_GONE = 0;
		static final int STATE_ACTIVE = 1;
		static final int STATE_VISIBLE = 2;
		
		int state;
		int cell_id;
		long time_active;
		long time_visible;
		double signal_avg;
		double signal_current;
		
		public Cell(int cid) {
			cell_id = cid;
		}
		public int getActiveTimePercentage() {
			return (int)Math.round((((double)time_active / (lastUpdateTime - startTime)) *100));
		}
		public int getVisibleTimePercentage() {
			return (int) Math.round(((double)time_visible / (lastUpdateTime - startTime)) *100);
		}
		private void updateSignal(long timeNew, int signalNew) {
//			long timeNew = System.currentTimeMillis() - lastUpdateTime;
			if (signal_current == 0) signal_current = signalNew;
			signal_avg = (((time_active+time_visible) * signal_avg + timeNew * signal_current) / (time_active+time_visible+timeNew));
			signal_current = signalNew;
		}
	}
	

	public static HashMap<Integer, Cell> cells;
	public static Cell currentCell;
	public static int currentSignal;
	public static long startTime;
	private long lastUpdateTime;


	@Override
	public void onCreate() {
		
		cells = new HashMap<Integer, Cell>();
		handler = new Handler();
		
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE); 
		
		class StateChekker extends PhoneStateListener {

			@Override
			public void onCellLocationChanged(CellLocation location) {

				updateStats();
				if (currentCell != null) currentCell.state = Cell.STATE_GONE;
				currentCell = findOrCreateCell(((GsmCellLocation)location).getCid());
				currentCell.state = Cell.STATE_ACTIVE;
				super.onCellLocationChanged(location);
			}

			@Override
			public void onSignalStrengthChanged(int signal) {
				
				updateStats();
				currentSignal = signal;
				super.onSignalStrengthChanged(signal);
			}
        	
        }
        
        tm.listen(new StateChekker(), PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
        
        update = new Runnable() {
        	
        	@Override
        	public void run() {
        		updateStats();
        		handler.postDelayed(update, 300);
        	}
        };
        handler.post(update);
        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;
		super.onCreate();
	}

	
	private synchronized void updateStats() {
		if (currentCell != null && currentSignal != 0) {
			final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			final long now = System.currentTimeMillis();
			final long timeNew = now - lastUpdateTime;
			
			currentCell.updateSignal(timeNew, currentSignal);
			currentCell.time_active += timeNew;
			for (Iterator iterator = cells.values().iterator(); iterator.hasNext();) {
				Cell cell = (Cell) iterator.next();
				if (cell.state == Cell.STATE_VISIBLE)
					cell.state = Cell.STATE_GONE;
			}
//			Log.d("HIER", "NEIGHBOURs  "+tm.getNeighboringCellInfo().size());
			for (Iterator iterator = tm.getNeighboringCellInfo().iterator(); iterator.hasNext();) {
				NeighboringCellInfo neighboringCellInfo = (NeighboringCellInfo) iterator.next();
				Log.d("HIER", "NEIGHBOUR "+neighboringCellInfo.getCid());
				Cell neighbouringCell = findOrCreateCell(neighboringCellInfo.getCid());
				neighbouringCell.state = Cell.STATE_VISIBLE;
				neighbouringCell.updateSignal(timeNew, neighboringCellInfo.getRssi());
				neighbouringCell.time_visible += timeNew;
			}
			lastUpdateTime = now;
		}

	}
		

		

	private Cell findOrCreateCell(int cellId) {
		Integer newCellId = new Integer(cellId);
		Cell cell = cells.get(newCellId);
		if (cell == null) {
			cell = new Cell(newCellId);
			cells.put(newCellId, cell);
		}
		return cell;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
//		if (currentCell != null && currentSignal != 0)
//			currentCell.update();
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
