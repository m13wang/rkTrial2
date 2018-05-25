package dashboard.rk.com.rktrial2;




import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.text.*;


public class Main extends Activity {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_PACKET = 6;


    ToggleButton StreamingButton;
    ToggleButton DataLogButton;
    TextView ConnectionState;
    TextView DataLog;
    TextView DataPoints;
    TextView ErrorLog;
    TextView BatteryN;
    TextView CurrentN;
    TextView TemperatureN;

    int CurrentSum = 0;
    int TempCntrl = 0;
    int BatteryCntrl = 0;
    int ThrottleCntrl = 0;
    int DataPackets = 0;

    int ErrorByteH = 0;
    int ErrorByteL = 0;

    int LastErrorByteH = 0;
    int LastErrorByteL = 0;



    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private Button ScanButton;
    private Button ShareButton;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);





        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            setupConnection();
        }
  //      TestLogger();
    }

/*
    private void TestLogger() {

        int rBuff[]={10,20,30,40,50,60,128,254,9,00};
        String dataline;

        // create a file
        File file = null;
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        try{

            //Specify the file name and path here
                    //	File file =new File("C://myfile.txt");
            Date date = new Date() ;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
            file = new File(path, dateFormat.format(date) + "logfile.csv") ;

            //file = new File(path, "loggerfile2.csv");
                	// Create the file if the file is not already present
            if(!file.exists()){
                file.createNewFile();
            }

        }catch(IOException ioe){
            System.out.println("Exception occurred:");
            ioe.printStackTrace();
        }

        //generate a string, write append, do this 5 times
        for (int newline=1; newline<5; newline++){
            rBuff[1]++;
            dataline = "";
            for(int i=1; i<rBuff.length; i++){
                dataline += rBuff[i];
                dataline += ",";
            }
            dataline += "\n";
            // write the line
            try{
                //Here true is to append the content to file
                FileWriter fw = new FileWriter(file,true);
                //BufferedWriter writer give better performance
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(dataline);
                //Closing BufferedWriter Stream
                bw.close();

                System.out.println("Data successfully appended at the end of file");

            }catch(IOException ioe){
                System.out.println("Exception occurred:");
                ioe.printStackTrace();
            }
        }
    }
*/
    private void setupConnection()
    {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        ScanButton = (Button) findViewById(R.id.Scan);
        ScanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent serverIntent = new Intent(Main.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

            }


        });


        StreamingButton = (ToggleButton) findViewById(R.id.StreamingToggle);
        StreamingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (StreamingButton.isChecked())
                {
                    String message = "##SxOx;";
                    sendMessage(message.getBytes());
                }
                else
                {
                    String message = "##SxFx;";
                    sendMessage(message.getBytes());

                }

            }


        });


        ConnectionState = (TextView) findViewById(R.id.TextView12);
        ErrorLog = (TextView) findViewById(R.id.TextViewErrors);
        DataPoints = (TextView) findViewById(R.id.DataPoints);
        BatteryN = (TextView) findViewById(R.id.TextBatt);
        CurrentN = (TextView) findViewById(R.id.TextCurr);
        TemperatureN = (TextView) findViewById(R.id.TextTemp);
    }


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            // mTitle.setText(R.string.title_connected_to);
                            //mTitle.append(mConnectedDeviceName);
                            //mConversationArrayAdapter.clear();
                            ConnectionState.setText("Connected");

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //mTitle.setText(R.string.title_connecting);
                            ConnectionState.setText("Connecting..");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(readMessage);
                    //TextView pData = (TextView) findViewById(R.id.PacketData);

                    //Toast.makeText(getApplicationContext(), "Packet Recieved!",Toast.LENGTH_SHORT).show();

                    ProgressBar ThrottleBar = (ProgressBar) findViewById(R.id.ThrottleVoltage);
                    ProgressBar BatteryBar = (ProgressBar) findViewById(R.id.BattVoltage);
                    ProgressBar CurrentBar = (ProgressBar) findViewById(R.id.Current);
                    ProgressBar PWMBar = (ProgressBar) findViewById(R.id.PWM);
                    ProgressBar TempBar = (ProgressBar) findViewById(R.id.Temperature);

                    byte ThrottleIndex = 3;
                    byte CurrentIndex = 4;
                    byte BatteryIndex = 6;
                    byte PWMIndex = 10;
                    byte TempIndex = 8;

                    ThrottleBar.setMax(100);
                    ThrottleCntrl = (ConvertByte(readBuf[ThrottleIndex]));
                    if(ThrottleCntrl>170) {ThrottleCntrl=170;}
                    ThrottleCntrl =(ThrottleCntrl-80)*10/9;
                    ThrottleBar.setProgress(ThrottleCntrl);

                    PWMBar.setMax(255);
                    PWMBar.setProgress(ConvertByte(readBuf[PWMIndex]));

                    CurrentBar.setMax(40);
                    //CurrentSum = (float) (CurrentSum*0.95 + (((float)ConvertByte(readBuf[4])/1024*1.024)/0.003)*.05);
                    CurrentSum = ConvertByte(readBuf[CurrentIndex])/3;
                    CurrentN.setText("Current :"+CurrentSum);
                    CurrentBar.setProgress(CurrentSum);

                    BatteryBar.setMax(175);
                    BatteryCntrl = ConvertByte(readBuf[BatteryIndex]);
                    BatteryBar.setProgress(BatteryCntrl);
                    BatteryCntrl = BatteryCntrl * 30/17;
                    BatteryN.setText("Battery :"+BatteryCntrl);

                    TempBar.setMax(125);
                    TempCntrl = ConvertByte(readBuf[TempIndex]);
                    TemperatureN.setText("Temperature :"+TempCntrl);
                    TempBar.setProgress(TempCntrl);

                    ErrorByteH = ConvertByte(readBuf[1]);
                    ErrorByteL = ConvertByte(readBuf[2]);

                    //Look for a new error
                    if (LastErrorByteH != ErrorByteH || LastErrorByteL != ErrorByteL )
                    {
                        ErrorLog.append("E="+DecodeErrors(ErrorByteH*256+ErrorByteL)+"\n");
                    }

                    LastErrorByteL = ErrorByteL;
                    LastErrorByteH = ErrorByteH;

                    DataPoints.setText("Data Points:"+DataPackets);
                    DataPackets++;

                    /*  If logging just switced on = open the file for writing
                     Need a unique name in case a file was just written. (Don't overwrite it) */

                    String dataline;
                    DataLogButton = (ToggleButton) findViewById(R.id.LoggingToggle);

                    /* new click opens a new file with unique name */
                    DataLogButton.setOnClickListener(new OnClickListener() {
                                                         public void onClick(View v) {
                                                             DataLogger.init();
                                                         }
                                                     });

                    // When logging is on, write the entire readBuf() to the write stream
                    if (DataLogButton.isChecked())
                    {
                    // add commas and line feed to readbuf
                        dataline = "";
                        for(int i=1; i<readBuf.length; i++){
                            dataline += ConvertByte(readBuf[i]);
                            dataline += ",";
                        }
                        dataline += "\n";
                    // write the line
                        DataLogger.append (dataline);

                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_PACKET:
                    Toast.makeText(getApplicationContext(), "Packet Recieved!",Toast.LENGTH_SHORT).show();

                    break;


            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupConnection();
                } else {
                    // User did not enable Bluetooth or an error occured

                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }


    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        // if (message.length() > 0) {
        // Get the message bytes and tell the BluetoothChatService to write
        //byte[] send = message.getBytes();
        mChatService.write(message);

        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
        //mOutEditText.setText(mOutStringBuffer);
        //}
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.exit:


                int pid = android.os.Process.myPid();
                android.os.Process.killProcess(pid);


                return true;

            //   case R.id.Rotate:
            //   	if (this.getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            //   	       	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            // 	else
            //		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // 	getRequestedOrientation();
            // return true;

        }
        return false;
    }

    public int ConvertByte(byte data)
    {
        int mask = 0xFF;
        return data & mask;
        /*
        if (data >= 0)
            return data;
        else
            return data & mask;
    */
    }


    //Decode Errors
    public String DecodeErrors(int Error)
    {
        String Errors = "[";

    	/*
  			From the firmware:
    	#define LowBattery_m			0b0000000000000001 =1
    	#define DeadBattery_m			0b0000000000000010 =2
    	#define Temperature_m 			0b0000000000000100 =4
    	#define ThrottleNotOff_m 			0b0000000000001000 =8
    	#define ThrottleZero_m 			0b0000000000010000 =16
    	#define CurrentLimit_m 			0b0000000000100000 =32
    	#define ChargerConnected_m 		0b0000000001000000 =64
    	#define BadFuse_m 				0b0000000010000000 =128
    	#define USBCurrent_m 			0b0000000100000000 = 256
    	#define PowerShort_m 			0b0000001000000000 = 512
    	#define MotorNotConnected_m 		0b0000010000000000 =1024
    	#define  FaultyBattery_m			0b0000100000000000 =2048
    	#define DeadBatteryUnderPower_m   0b0001000000000000 = 4096
    	#define StallDetected_m  			0b0010000000000000 = 8192
    	*/


        if ((Error & 1) != 0)
            Errors = Errors + " LowBatt";

        if ((Error & 2) != 0)
            Errors = Errors + " DeadBatt";

        if ((Error & 4) != 0)
            Errors = Errors + " FetTemp";

        if ((Error & 8) != 0)
            Errors = Errors + " ThrottleNotOff";

        if ((Error & 16) != 0)
            Errors = Errors + " ThrottleShort";

        if ((Error & 32) != 0)
            Errors = Errors + " CurrentLimit";

        if ((Error & 64) != 0)
            Errors = Errors + " Charger";

        if ((Error & 128) != 0)
            Errors = Errors + " BadFuse";

        if ((Error & 256) != 0)
            Errors = Errors + " USB";

        if ((Error & 512) != 0)
            Errors = Errors + " PowerShort";

        if ((Error & 1024) != 0)
            Errors = Errors + " MotorNC";

        if ((Error & 2048) != 0)
            Errors = Errors + " BattFault";

        if ((Error & 4096) != 0)
            Errors = Errors + " DeadBattUnderPower";

        if ((Error & 8192) != 0)
            Errors = Errors + " StallDetected";


        Errors = Errors + " ]";





        return (Errors);
    }

}