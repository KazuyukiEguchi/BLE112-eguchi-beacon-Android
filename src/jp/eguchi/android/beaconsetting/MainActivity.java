// eguchi-beaconのパラメータを書き換えるアプリケーション
// Programed by Kazuyuki Eguchi
// Copyright(c) Kazuyuki Eguchi

package jp.eguchi.android.beaconsetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private static final String SERVICE_UUID = "9D9AD8D3-5940-479B-A6BA-EA85474ABAEE";
	private static final String C_UUID_UUID = "4FD5BB8D-9974-47BE-91D3-9CA4171750D8";
	private static final String C_MAJOR_UUID = "F7BBC353-CDC2-40A2-B422-5442413DA8BA";
	private static final String C_MINOR_UUID = "4814DACE-D2DC-4209-9F1D-7A880B6026C0";
	
	private static final String D_UUID = "CDAD3F7F82914B078F1FB36B3A27AD89";
	
	private BluetoothManager bm = null;
	private BluetoothAdapter ba = null;
	private boolean mScanning = false;
	private boolean mConnected = false;
    private Handler mHandler = null;
    private static final long SCAN_PERIOD = 10000;
    // private static final String TAG = "BEACONCONT";
    private BluetoothDevice bd = null;
    private List<BluetoothGattCharacteristic> mCharacteristics = null;
    private int mPos = 0;
    
    private ArrayList<String> array = new ArrayList<String>();
    
    private Spinner mSpinner = null;
    private EditText mText01 = null;
    private EditText mText02 = null;
    private EditText mText03 = null;
    private TextView mMes = null;
    
    private ProgressDialog mDialog = null;
    private Context mContext = null;
    
    private String mUUID = D_UUID;
    private String mMajor = "0000";
    private String mMinor = "0000";
    
    private boolean bWrite = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mHandler = new Handler();
		mContext = this;
		
		setContentView(R.layout.activity_main);
		
		mSpinner = (Spinner)findViewById(R.id.spinner01);
		
		mMes = (TextView)findViewById(R.id.mes);

		mText01 = (EditText)findViewById(R.id.text01);
		mText01.setText(mUUID);
		
		mText02 = (EditText)findViewById(R.id.text02);
		mText02.setText(mMajor);
		
		mText03 = (EditText)findViewById(R.id.text03);
		mText03.setText(mMinor);
		
		Button btn01 = (Button)findViewById(R.id.button01);
		
		btn01.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(mConnected == false)
				{
					scanLeDevice(true);
				}
			}
		});

		Button btn02 = (Button)findViewById(R.id.button02);
		
		btn02.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(mSpinner.getSelectedItem() != null)
				{
					// Log.d(TAG,mSpinner.getSelectedItem().toString());
					
					if(mConnected == false)
					{
						connect(mSpinner.getSelectedItem().toString());
					}
				}
			}
		});
		
		Button btn03 = (Button)findViewById(R.id.button03);
		
		btn03.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(mSpinner.getSelectedItem() != null)
				{
					if(mConnected == false)
					{
						// Log.d(TAG,mSpinner.getSelectedItem().toString());
						connect2(mSpinner.getSelectedItem().toString());
					}
					else
					{
						Toast.makeText(mContext, "前の工程が終わっていません。", Toast.LENGTH_LONG).show();
					}
				}
			}
		});
		
		bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        ba = bm.getAdapter();
	}
	
	private void setSpinner(Spinner spinner,String[] lists)
	{
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lists);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}
		
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
	{
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
		{
			if(scanRecord.length > 30)
			{
				// iBeacon互換信号でフィルターする
			    if((scanRecord[5] == (byte)0x4c) && (scanRecord[6] == (byte)0x00) &&
			       (scanRecord[7] == (byte)0x02) && (scanRecord[8] == (byte)0x15))
			    {
			    	// その後はデバイス名でフィルターする
			    	if(device.getName().compareTo("eguchi") == 0)
			    	{
			    		array.add(device.getAddress().toString());
			    	}
			    }
			}
		}
	};
	
	private void scanLeDevice(final boolean enable)
	{
		 if (enable)
		 {
			 mHandler.postDelayed(new Runnable()
			 {
				 @Override
				 public void run()
				 {
					 mScanning = false;
	                 ba.stopLeScan(mLeScanCallback);
	                 
	                 mDialog.dismiss();
	                 
	                 // 重複を取り除く
	                 HashSet<String> hashSet = new HashSet<String>();
	                 hashSet.addAll(array);
	                 
	                 // String[]に変換する
	                 String[] lists = (String[]) hashSet.toArray(new String[]{});
	                 
	                 // Spinnerを作る
	                 setSpinner(mSpinner,lists);
	             }
	         }, SCAN_PERIOD);

	         if(ba.startLeScan(mLeScanCallback) == true)
	         {
		         mScanning = true;

		         mDialog = new ProgressDialog(mContext);
				 mDialog.setMessage("ビーコンを検索中 ...");
				 mDialog.setCancelable(false);
				 mDialog.show();
	         }
	     }
		 else
		 {
	         mScanning = false;
	         ba.stopLeScan(mLeScanCallback);
	     }
	}
	
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			// Log.d(TAG,"onServicesDiscovered");
			List<BluetoothGattService> lists = gatt.getServices();
			
			for(int i = 0 ; i < lists.size() ; i++)
			{
				BluetoothGattService service = lists.get(i);
				
				if(service.getUuid().compareTo(UUID.fromString(SERVICE_UUID)) == 0)
				{
					mCharacteristics = service.getCharacteristics();
					
					for(int j = 0 ; j < mCharacteristics.size() ; j++)
					{
						BluetoothGattCharacteristic characteristic = mCharacteristics.get(j);
					}
				}
			}

			mPos = 0;
			
			if(bWrite == false)
			{
				mMajor = null;
				mMinor = null;
				mUUID = null;

				ReadParameter(gatt);
			}
			else
			{
				mUUID = mText01.getText().toString();
				mMajor = mText02.getText().toString();
				mMinor = mText03.getText().toString();
				
				WriteParameter(gatt);
			}
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,int newState)
		{
			switch(newState)
			{
				case BluetoothProfile.STATE_CONNECTED:
					// Log.d(TAG,"onConnectionStateChange STATE_CONNECTED");
					mConnected = true;
					gatt.discoverServices();
					break;
					
				case BluetoothProfile.STATE_DISCONNECTED:
					// Log.d(TAG,"onConnectionStateChange STATE_DISCONNECTED");
					mConnected = false;
					break;
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status)
		{
			// Log.d(TAG,"onCharacteristicRead");
			
			byte [] tmp = characteristic.getValue();

			if(characteristic.getUuid().compareTo(UUID.fromString(C_MAJOR_UUID)) == 0)
			{
				mMajor = asHex(tmp);
			}
			
			if(characteristic.getUuid().compareTo(UUID.fromString(C_MINOR_UUID)) == 0)
			{
				mMinor = asHex(tmp);
			}
			
			if(characteristic.getUuid().compareTo(UUID.fromString(C_UUID_UUID)) == 0)
			{
				mUUID = asHex(tmp);
			}

			mPos++;
			
			ReadParameter(gatt);
		}

		@Override
		public void onCharacteristicWrite(final BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) 
		{
			// Log.d(TAG,"onCharacteristicWrite");
			
			mPos++;
			WriteParameter(gatt);
		}
	};
	
	private void connect(String addr)
	{
		bWrite = false;
		
		if(addr != null)
		{
			bd = ba.getRemoteDevice(addr);
			
			if(bd != null)
			{
				bd.connectGatt(getApplicationContext(), false, mGattCallback);
			}
		}
	}
	
	private void connect2(String addr)
	{
		bWrite = true;
		
		if(addr != null)
		{
			bd = ba.getRemoteDevice(addr);
			
			if(bd != null)
			{
				bd.connectGatt(getApplicationContext(), false, mGattCallback);
			}
		}
	}
	
	private void ReadParameter(BluetoothGatt gatt)
	{
		if(mPos < mCharacteristics.size())
		{
			BluetoothGattCharacteristic characteristics = mCharacteristics.get(mPos);
			gatt.readCharacteristic(characteristics);
		}
		else
		{
			// Log.d(TAG,"UUID: " + mUUID);
			// Log.d(TAG,"MAJOR: " + mMajor + ", MINIR: " + mMinor);
			
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mMes.setText("UUID="+ mUUID + " ,MAJOR=" + mMajor + " ,MINOR" + mMinor);
				}
			});

			gatt.disconnect();
		}
	}
	
	private void WriteParameter(BluetoothGatt gatt)
	{
		// Log.d(TAG,"WriteParameter");
		
		if(mPos < mCharacteristics.size())
		{
			BluetoothGattCharacteristic characteristic = mCharacteristics.get(mPos);
			
			byte[] value = null;
			
			if(characteristic.getUuid().compareTo(UUID.fromString(C_MAJOR_UUID)) == 0)
			{
				// Log.d(TAG,"C_MAJOR_UUID");
				value = asByteArray(mMajor);
			}
			else if(characteristic.getUuid().compareTo(UUID.fromString(C_MINOR_UUID)) == 0)
			{
				// Log.d(TAG,"C_MINOR_UUID");
				value = asByteArray(mMinor);
			}
			else if(characteristic.getUuid().compareTo(UUID.fromString(C_UUID_UUID)) == 0)
			{
				// Log.d(TAG,"C_UUID_UUID");
				value = asByteArray(mUUID);
			}
			
			if(value != null)
			{
				characteristic.setValue(value);
			
				if(gatt.writeCharacteristic(characteristic) != true)
				{
					// Log.d(TAG,"Error writeCharacteristic");
				}
				
				return;
			}
			
			mPos++;
			WriteParameter(gatt);
		}
		else
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Toast.makeText(mContext, "書き込み完了!!!", Toast.LENGTH_LONG).show();
				}
			});
			
			gatt.disconnect();
		}
	}
	
	private static String asHex(byte bytes[])
	{
		StringBuffer strbuf = new StringBuffer(bytes.length * 2);

		for (int index = 0; index < bytes.length; index++)
		{
			int bt = bytes[index] & 0xff;

			if (bt < 0x10)
			{
				strbuf.append("0");
			}

			strbuf.append(Integer.toHexString(bt).toUpperCase());
		}

		return strbuf.toString();
	}
	
	private static byte[] asByteArray(String hex)
	{
		byte[] bytes = new byte[hex.length() / 2];

		for (int index = 0; index < bytes.length; index++)
		{
			bytes[index] = (byte) Integer.parseInt(hex.substring(index * 2, (index + 1) * 2),16);
		}

		return bytes;
	}

}
