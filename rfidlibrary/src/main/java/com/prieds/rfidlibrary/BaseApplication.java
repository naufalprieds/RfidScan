package com.prieds.rfidlibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.BRMicro.Tools;
import com.handheld.uhfr.UHFRManager;
import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.prieds.rfidlibrary.model.UhfTagInfoCustom;
import com.rscja.deviceapi.RFIDWithUHFBLE;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.ConnectionStatus;
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback;
import com.uhf.api.cls.Reader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BaseApplication {
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_SELECT_DEVICE = 1;

    public BluetoothDevice mDevice = null;

    public BluetoothAdapter mBtAdapter = null;
    public RFIDWithUHFBLE uhf = RFIDWithUHFBLE.getInstance();
    BTStatus btStatus = new BTStatus();
    public static final String SHOW_HISTORY_CONNECTED_LIST = "showHistoryConnectedList";
    public static final String TAG_DATA = "tagData";
    public static final String TAG_EPC = "tagEpc";
    public static final String TAG_TID = "tagTid";
    public static final String TAG_LEN = "tagLen";
    public static final String TAG_COUNT = "tagCount";
    public static final String TAG_RSSI = "tagRssi";

    private boolean mIsActiveDisconnect = true; // 是否主动断开连接
    private static final int RECONNECT_NUM = Integer.MAX_VALUE; // 重连次数
    private int mReConnectCount = RECONNECT_NUM; // 重新连接次数

    private Timer mDisconnectTimer = new Timer();
    private DisconnectTimerTask timerTask;
    private long timeCountCur; // 断开时间选择
    private long period = 1000 * 30; // 隔多少时间更新一次
    private long lastTouchTime = System.currentTimeMillis(); // 上次接触屏幕操作的时间戳
    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 100;
    private static final int REQUEST_ACTION_LOCATION_SETTINGS = 3;
    private TextView tvAddress;
    public static UHFRManager mUhfrManager;//uhf
    private boolean isSingle = false ;// single mode flag
    private boolean isMulti = false ;

    private static final int RUNNING_DISCONNECT_TIMER = 10;

    private String TAG = "UHFReadTagFragment";

    private boolean loopFlag = false;

    private ConnectStatus mConnectStatus = new ConnectStatus();

    //--------------------------------------获取 解析数据-------------------------------------------------
    final int FLAG_START = 0;//开始
    final int FLAG_STOP = 1;//停止
    final int FLAG_UPDATE_TIME = 2; // 更新时间
    final int FLAG_UHFINFO = 3;
    final int FLAG_UHFINFO_LIST = 5;
    final int FLAG_SUCCESS = 10;//成功
    final int FLAG_FAIL = 11;//失败

    boolean isRuning = false;
    private long mStrTime;

    private HashMap<String, String> tagMap = new HashMap<>();
    private List<String> tempDatas = new ArrayList<>();
    private ArrayList<HashMap<String, String>> tagList;
    private boolean isExit = false;
    private long total = 0;

    List<UhfTagInfoCustom> uhfTagInfoCustoms = new ArrayList<>();

    //variable rfid

    //uhf new
    /**读码结果发送的广播action*/
    public final static String ACTION_UHF_RESULT_SEND = "nlscan.intent.action.uhf.ACTION_RESULT";
    /**读码结果发送的广播Extra*/
    public final static String EXTRA_TAG_INFO = "tag_info";
    private UHFManager mUHFMgr;
    private HandlerThread mHandlerThread;
    private ResultHandler mResultHandler;

    Map<String, TagInfo> TagsMap = new LinkedHashMap<String, TagInfo>();// 有序
    private List<Map<String, ?>> ListMs = new ArrayList<Map<String, ?>>();
    private String[] Coname = new String[] { "序号", "EPC ID", "次数", "天线", "协议", "RSSI", "频率", "附加数据" };//表头

    private Map<String, String> listHeader = new HashMap<String, String>();

    /*About Read EPC*/
    private final int MSG_FIND_ASSET = 0;
    private final int MSG_NOT_FIND_ASSET = 1;
    private final int MSG_NOT_OPEN_COMMPORT=2;

    private long exittime;

    private String TAG = "LIB_EXAMPLE_ANDROID"
    void i(String message){
        Log.i(TAG, message);
    }

    class BTStatus implements ConnectionStatusCallback<Object> {
        @Override
        public void getStatus(final ConnectionStatus connectionStatus, final Object device1) {
            runOnUiThread(new Runnable() {
                public void run() {
                    BluetoothDevice device = (BluetoothDevice) device1;
                    remoteBTName = "";
                    remoteBTAdd = "";
                    if (connectionStatus == ConnectionStatus.CONNECTED) {
                        remoteBTName = device.getName();
                        remoteBTAdd = device.getAddress();

                        tvAddress.setText(String.format("%s(%s)\nconnected", remoteBTName, remoteBTAdd));
                        if (shouldShowDisconnected()) {
                            showToast(R.string.connect_success);
                        }

                        timeCountCur = SPUtils.getInstance(getApplicationContext()).getSPLong(SPUtils.DISCONNECT_TIME, 0);
                        if (timeCountCur > 0) {
                            startDisconnectTimer(timeCountCur);
                        } else {
                            formatConnectButton(timeCountCur);
                        }

                        // 保存已链接记录
                        if (!TextUtils.isEmpty(remoteBTAdd)) {
                            saveConnectedDevice(remoteBTAdd, remoteBTName);
                        }


                        mIsActiveDisconnect = false;
                        mReConnectCount = RECONNECT_NUM;
                    } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                        cancelDisconnectTimer();
                        formatConnectButton(timeCountCur);
                        if (device != null) {
                            remoteBTName = device.getName();
                            remoteBTAdd = device.getAddress();
//                            if (shouldShowDisconnected())
                            tvAddress.setText(String.format("%s(%s)\ndisconnected", remoteBTName, remoteBTAdd));
                        } else {
//                            if (shouldShowDisconnected())
                            tvAddress.setText("disconnected");
                        }
                        if (shouldShowDisconnected())
                            showToast(R.string.disconnect);

                        boolean reconnect = SPUtils.getInstance(getApplicationContext()).getSPBoolean(SPUtils.AUTO_RECONNECT, false);
                        if (mDevice != null && reconnect) {
                            reConnect(mDevice.getAddress()); // 重连
                        }
                    }

                    for (IConnectStatus iConnectStatus : connectStatusList) {
                        if (iConnectStatus != null) {
                            iConnectStatus.getStatus(connectionStatus);
                        }
                    }
                }
            });
        }
    }

    private void startDisconnectTimer(long time) {
        timeCountCur = time;
        timerTask = new DisconnectTimerTask();
        mDisconnectTimer.schedule(timerTask, 0, period);
    }

    public void cancelDisconnectTimer() {
        timeCountCur = 0;
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private class DisconnectTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.e(TAG, "timeCountCur = " + timeCountCur);
            if(isScanning) {
                resetDisconnectTime();
            } else if (timeCountCur <= 0){
                disconnect(true);
            }
            timeCountCur -= period;
        }
    }

    public void resetDisconnectTime() {
        timeCountCur = SPUtils.getInstance(getApplicationContext()).getSPLong(SPUtils.DISCONNECT_TIME, 0);
        if (timeCountCur > 0) {
            formatConnectButton(timeCountCur);
        }
    }

    private void formatConnectButton(long disconnectTime) {
        if (uhf.getConnectStatus() == ConnectionStatus.CONNECTED) {
            if (!isScanning && System.currentTimeMillis() - lastTouchTime > 1000 * 30 && timerTask != null) {
                long minute = disconnectTime / 1000 / 60;
                if(minute > 0) {
//                    btn_connect.setText(getString(R.string.disConnectForMinute, minute)); //倒计时分
                } else {
//                    btn_connect.setText(getString(R.string.disConnectForSecond, disconnectTime / 1000)); // 倒计时秒
                }
            } else {
//                btn_connect.setText(R.string.disConnect);
            }
        } else {
//            btn_connect.setText(R.string.Connect);
        }
    }

    public void saveConnectedDevice(String address, String name) {
        List<String[]> list = FileUtils.readXmlList();
        for (int k = 0; k < list.size(); k++) {
            if (address.equals(list.get(k)[0])) {
                list.remove(list.get(k));
                break;
            }
        }
        String[] strArr = new String[]{address, name};
        list.add(0, strArr);
        FileUtils.saveXmlList(list);
    }

    //------------连接状态监听-----------------------
    private List<IConnectStatus> connectStatusList = new ArrayList<>();

    public void addConnectStatusNotice(IConnectStatus iConnectStatus) {
        connectStatusList.add(iConnectStatus);
    }

    public void removeConnectStatusNotice(IConnectStatus iConnectStatus) {
        connectStatusList.remove(iConnectStatus);
    }

    public interface IConnectStatus {
        void getStatus(ConnectionStatus connectionStatus);
    }

    public void onHidden(Boolean hidden) {
        f1hidden = hidden;
//		Log.e("hidden", "hide"+hidden) ;
        if (hidden) {
            if (isStart) runInventory();// stop inventory
        }
        if (mUhfrManager!=null) mUhfrManager.setCancleInventoryFilter();
    }

    private boolean f1hidden = false;
    private  long startTime = 0 ;
    private boolean keyUpFalg= true;
    private BroadcastReceiver keyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (f1hidden) return;
            int keyCode = intent.getIntExtra("keyCode", 0) ;
            if(keyCode == 0){//H941
                keyCode = intent.getIntExtra("keycode", 0) ;
            }
            Log.v("keyCode", Integer.toString(keyCode));
//            Log.e("key ","keyCode = " + keyCode) ;
            boolean keyDown = intent.getBooleanExtra("keydown", false) ;
//			Log.e("key ", "down = " + keyDown);
            if(keyUpFalg&&keyDown && System.currentTimeMillis() - startTime > 100){
                keyUpFalg = false;
                startTime = System.currentTimeMillis() ;

                if ( (keyCode == KeyEvent.KEYCODE_F1 || keyCode == KeyEvent.KEYCODE_F2
                        || keyCode == KeyEvent.KEYCODE_F3 || keyCode == KeyEvent.KEYCODE_F4 ||
                        keyCode == KeyEvent.KEYCODE_F5)) {
//                Log.e("key ","inventory.... " ) ;
                    runInventory();
                }
                return ;
            }else if (keyDown){
                startTime = System.currentTimeMillis() ;
            }else {
                keyUpFalg = true;
            }

        }
    } ;

    private boolean isRunning = false ;
    private boolean isStart = false ;
    String epc ;
    //inventory epc
    private Runnable inventoryTask = new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                if (isStart) {
                    List<Reader.TAGINFO> list1 ;
                    if (isMulti) { // multi mode
                        list1 = mUhfrManager.tagInventoryRealTime();
                    }else if (isSingle) {
                        list1 = mUhfrManager.tagInventoryByTimer((short)50);
                    }
                    else {
                        //sleep can save electricity
//						try {
//							Thread.sleep(250);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
                        list1 = mUhfrManager.tagInventoryByTimer((short)50);
                        //inventory epc + tid
//						list1 = mUhfrManager.tagEpcTidInventoryByTimer((short) 50);
                    }//
                    if (list1 != null&&list1.size()>0) {//
//                        for (int i = 0; i < list1.size(); i++) {
                        byte[] epcdata = list1.get(0).EpcId;
                        epc = Tools.Bytes2HexString(epcdata, epcdata.length);

//                            Message msg = new Message();
//                            msg.what = 1;
//                            Bundle b = new Bundle();
//                            b.putString("epc", epc);
//                            b.putString("rssi", rssi + "");
                        EventBus.getDefault().post(new EpcEventbus(epc, "embed", FLAG_START, null));
//                        }

//                        for (Reader.TAGINFO tfs : list1) {
//
////                            msg.setData(b);
////                            handler.sendMessage(msg);
//                        }
                    }
                }
            }
        }
    } ;
    private boolean keyControl = true;
    private void runInventory() {
        if (keyControl) {
            keyControl = false;
            if (!isStart) { // not start
                mUhfrManager.setCancleInventoryFilter();
                isRunning = true;
                if (isMulti) {
                    mUhfrManager.setFastMode();
                    mUhfrManager.asyncStartReading();
                } else {
                    mUhfrManager.setCancleFastMode();
                }
                new Thread(inventoryTask).start();
//                checkMulti.setClickable(false);
//                checkMulti.setTextColor(Color.GRAY);
                showToast("Stop Scan");
//            Log.e("inventoryTask", "start inventory") ;
                isStart = true;
            } else {
//                checkMulti.setClickable(true);
//                checkMulti.setTextColor(Color.BLACK);
                if (isMulti)
                    mUhfrManager.asyncStopReading();
                else
                    mUhfrManager.stopTagInventory();
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isRunning = false;
                showToast("Start Scan");
                isStart = false;
            }
            keyControl = true;
        }
    }

    public void startThread() {
        if (isRuning) {
            return;
        }
        isRuning = true;
//        cbFilter.setChecked(false);
        new TagThread().start();
    }

    class TagThread extends Thread {

        public void run() {
            EventBus.getDefault().post(new EpcEventbus("", "not-embed", FLAG_START, null));
            if (uhf.startInventoryTag()) {
                loopFlag = true;
                isScanning = true;
                mStrTime = System.currentTimeMillis();
                EventBus.getDefault().post(new EpcEventbus("", "not-embed", FLAG_SUCCESS, null));
            } else {
                EventBus.getDefault().post(new EpcEventbus("", "not-embed", FLAG_FAIL, null));
            }
            isRuning = false;//执行完成设置成false
            long startTime=System.currentTimeMillis();
            while (loopFlag) {
                List<UHFTAGInfo> list = getUHFInfo();
                if(list==null || list.size()==0){
                    SystemClock.sleep(1);
                }else{
                    Utils.playSound(1);
                    EventBus.getDefault().post(new EpcEventbus("", "not-embed", FLAG_UHFINFO_LIST, list));
                }
                if(System.currentTimeMillis()-startTime>100){
                    startTime=System.currentTimeMillis();
//                    handler.sendEmptyMessage(FLAG_UPDATE_TIME);
                }

            }
            stopInventory();
        }
    }

    private synchronized   List<UHFTAGInfo> getUHFInfo() {
        List<UHFTAGInfo> list = uhf.readTagFromBufferList();
        return list;
    }

    /**
     * 添加EPC到列表中
     * @param uhftagInfo
     */
    private void addEPCToList(UHFTAGInfo uhftagInfo) {
        addEPCToList(uhftagInfo, true);
    }
    private void addEPCToList(List<UHFTAGInfo> list,boolean isRepeat) {
        boolean found = false;

        for(int k=0;k<list.size();k++){
            UHFTAGInfo uhftagInfo=list.get(k);

            if (adapter.getDataOriginal().size() < 1){
//                adapter.add(new UhfTagInfoCustom(uhftagInfo.getEPC()));

                for (int i = 0; i < cartItem.getAttributeList().size(); i++) {
                    if (cartItem.getAttributeList().get(i).getK().equalsIgnoreCase("STD_REPACKING")){
                        uhfTagInfoCustoms.add(new UhfTagInfoCustom(uhftagInfo.getEPC(), Float.parseFloat(cartItem.getAttributeList().get(i).getV()), 0));
                        break;
                    }
                }
                adapter.setData(uhfTagInfoCustoms);
                adapter.notifyDataSetChanged();

            }else{
                for (int i = 0; i < adapter.getDataOriginal().size(); i++) {
                    if (uhftagInfo.getEPC().equalsIgnoreCase(adapter.getDataOriginal().get(i).getEpc())){
                        found = true;
                    }
                }
                if (!found){
                    for (int i = 0; i < cartItem.getAttributeList().size(); i++) {
                        if (cartItem.getAttributeList().get(i).getK().equalsIgnoreCase("STD_REPACKING")){
                            adapter.add(new UhfTagInfoCustom(uhftagInfo.getEPC(), Float.parseFloat(cartItem.getAttributeList().get(i).getV()), 0));
                            break;
                        }
                    }
                }
            }
            Utils.playSound(1);
            binding.txtFulffilled.setText(adapter.getTotalQty((ArrayList<UhfTagInfoCustom>) adapter.getDataOriginal())+"");
            binding.txtScan.setText(adapter.getItemCount()+"");

        }
        adapter.notifyDataSetChanged();
    }
    private StringBuilder stringBuilder = new StringBuilder();
    /**
     * 添加EPC到列表中
     * @param uhftagInfo
     * @param isRepeat 是否重复添加
     */
    private void addEPCToList(UHFTAGInfo uhftagInfo, boolean isRepeat) {
        List<UhfTagInfoCustom> uhfTagInfoCustoms = new ArrayList<>();
        boolean found = false;

        if (adapter.getDataOriginal().size() < 1){
            for (int i = 0; i < cartItem.getAttributeList().size(); i++) {
                if (cartItem.getAttributeList().get(i).getK().equalsIgnoreCase("STD_REPACKING")){
                    uhfTagInfoCustoms.add(new UhfTagInfoCustom(uhftagInfo.getEPC(), Float.parseFloat(cartItem.getAttributeList().get(i).getV()), 0));
                    break;
                }
            }
            adapter.setData(uhfTagInfoCustoms);
            adapter.notifyDataSetChanged();
        }else{
            for (int i = 0; i < adapter.getDataOriginal().size(); i++) {
                if (uhftagInfo.getEPC().equalsIgnoreCase(adapter.getDataOriginal().get(i).getEpc())){
                    found = true;
                }
            }
            if (!found){
                for (int i = 0; i < cartItem.getAttributeList().size(); i++) {
                    if (cartItem.getAttributeList().get(i).getK().equalsIgnoreCase("STD_REPACKING")){
                        adapter.add(new UhfTagInfoCustom(uhftagInfo.getEPC(), Float.parseFloat(cartItem.getAttributeList().get(i).getV()), 0));
                        break;
                    }
                }
            }
        }
        Utils.playSound(1);
        binding.txtFulffilled.setText(adapter.getTotalQty((ArrayList<UhfTagInfoCustom>) adapter.getDataOriginal())+"");
        binding.txtScan.setText(adapter.getItemCount()+"");
    }

    /**
     * 判断EPC是否在列表中
     *
     * @param epc 索引
     * @return
     */
    public int checkIsExist(String epc) {
        if (TextUtils.isEmpty(epc)) {
            return -1;
        }
        return binarySearch(tempDatas, epc);
    }

    /**
     * 二分查找，找到该值在数组中的下标，否则为-1
     */
    static int binarySearch(List<String> array, String src) {
        int left = 0;
        int right = array.size() - 1;
        // 这里必须是 <=
        while (left <= right) {
            if (compareString(array.get(left), src)) {
                return left;
            } else if (left != right) {
                if (compareString(array.get(right), src))
                    return right;
            }
            left++;
            right--;
        }
        return -1;
    }

    static boolean compareString(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        } else if (str1.hashCode() != str2.hashCode()) {
            return false;
        } else {
            char[] value1 = str1.toCharArray();
            char[] value2 = str2.toCharArray();
            int size = value1.length;
            for (int k = 0; k < size; k++) {
                if (value1[k] != value2[k]) {
                    return false;
                }
            }
            return true;
        }
    }

    private Timer mTimer = new Timer();
    private TimerTask mInventoryPerMinuteTask;
    //    private long period = 6 * 1000; // 每隔多少ms
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "BluetoothReader" + File.separator;
    private String fileName;
    private void inventoryPerMinute() {
        cancelInventoryTask();
//        btInventoryPerMinute.setEnabled(false);
//        btInventory.setEnabled(false);
//        InventoryLoop.setEnabled(false);
//        btStop.setEnabled(true);
        isScanning = true;
        fileName = path + "battery_" + DateUtils.getCurrFormatDate(DateUtils.DATEFORMAT_FULL) + ".txt";
        mInventoryPerMinuteTask = new TimerTask() {
            @Override
            public void run() {
                String data = DateUtils.getCurrFormatDate(DateUtils.DATEFORMAT_FULL) + "\t电量：" + uhf.getBattery() + "%\n";
                FileUtils.writeFile(fileName, data, true);
                inventory();
            }
        };
        mTimer.schedule(mInventoryPerMinuteTask, 0, period);
    }

    private void cancelInventoryTask() {
        if(mInventoryPerMinuteTask != null) {
            mInventoryPerMinuteTask.cancel();
            mInventoryPerMinuteTask = null;
        }
    }

    private void inventory() {
        mStrTime = System.currentTimeMillis();
        UHFTAGInfo info = uhf.inventorySingleTag();
        if (info != null) {
            EventBus.getDefault().post(new EpcEventbus("", "not-embed", FLAG_UHFINFO, info));
        }

//        handler.sendEmptyMessage(FLAG_UPDATE_TIME);
    }

    private void stopInventory() {
        loopFlag = false;
        cancelInventoryTask();
        boolean result = uhf.stopInventory();
        if(isScanning) {
            int flag;
            ConnectionStatus connectionStatus = uhf.getConnectStatus();
            if (result || connectionStatus == ConnectionStatus.DISCONNECTED) {
                flag = FLAG_SUCCESS;
            } else {
                flag = FLAG_FAIL;
            }
            if (connectionStatus == ConnectionStatus.CONNECTED) {
                //在连接的情况下，结束之后继续接收未接收完的数据
                //getUHFInfoEx();
            }
            isScanning = false;
            EventBus.getDefault().post(new EpcEventbus("", "not-embed", flag, null));
        }
    }


    class ConnectStatus implements IConnectStatus {
        @Override
        public void getStatus(ConnectionStatus connectionStatus) {
            if (connectionStatus == ConnectionStatus.CONNECTED) {
                if (!loopFlag) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    InventoryLoop.setEnabled(true);
//                    btInventory.setEnabled(true);
//                    btInventoryPerMinute.setEnabled(true);
                }

//                cbFilter.setEnabled(true);
            } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                loopFlag = false;
                isScanning = false;
//                btClear.setEnabled(true);
//                btStop.setEnabled(false);
//                InventoryLoop.setEnabled(false);
//                btInventory.setEnabled(false);
//                btInventoryPerMinute.setEnabled(false);

//                cbFilter.setChecked(false);
//                cbFilter.setEnabled(false);
            }
        }
    }

    // uhf embed new

    private final static int MSG_REFRESH_RESULT_LIST = 0x01;

    /**
     * Handler分发Runnable对象的方式
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_FIND_ASSET:
                    Bundle bundle = msg.getData();
                    getNewData(bundle.getString("rfid"));
                    break;
                case MSG_NOT_FIND_ASSET:
                    break;
                case MSG_NOT_OPEN_COMMPORT:
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mResultReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(!"nlscan.intent.action.uhf.ACTION_RESULT".equals(action))
                return ;
            //标签数据数组
            Parcelable[] tagInfos =  intent.getParcelableArrayExtra("tag_info");
            //本次盘点启动的时间
            long startReading = intent.getLongExtra("extra_start_reading_time", 0l);
            //......
            for(Parcelable parcel : tagInfos)
            {
                TagInfo tagInfo = (TagInfo)parcel;
                String epcStr = UHFReader. bytes_Hexstr(tagInfo. EpcId);
                Log.d("TAG","Epc ID : "+ epcStr);

                Message message = new Message();
                message.what = MSG_FIND_ASSET;
                Bundle bundle = new Bundle();
                bundle.putString("rfid", epcStr);
                message.setData(bundle);
                mHandler.sendMessage(message);
            }
        }//end onReceiver
    };

    private class ResultHandler extends Handler
    {

        public ResultHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }//end
    }//end

    @Override
    protected void uhfPowerOning() {
        super.uhfPowerOning();
    }

    @Override
    protected void uhfPowerOn() {
        super.uhfPowerOn();
        showToast("Device Power On");
//        btn_start_read.setEnabled(mUHFMgr.isPowerOn());
//        btn_stop_read.setEnabled(mUHFMgr.isPowerOn());
    }

    @Override
    protected void uhfPowerOff() {
        super.uhfPowerOff();
        showToast("Device Power Off");
//        btn_start_read.setEnabled(false);
//        btn_stop_read.setEnabled(false);
    }

    PowerManager.WakeLock wl;
    private void keepScreen()
    {
        Log.d("TAG", "Wake up screen.");

		/*PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, "bright");
    	// 点亮屏幕
		wl.acquire();*/

//    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void releseScreenLock()
    {
//    	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	/*if(wl != null && wl.isHeld()){
    		wl.release();
    		wl = null;
    	}*/

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    private void registerResultReceiver()
    {
        try {
            IntentFilter iFilter = new IntentFilter("nlscan.intent.action.uhf.ACTION_RESULT");
            registerReceiver(mResultReceiver, iFilter);
        } catch (Exception e) {
        }

    }

    private void unRegisterResultReceiver()
    {
        try {
            unregisterReceiver(mResultReceiver);
        } catch (Exception e) {
        }

    }

    /**
     * 上电(连接)
     */
    private void powerOn()
    {
        UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
        er = mUHFMgr.powerOn();
        Toast.makeText(getApplicationContext(), "Power on :"+er.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 下电(断开)
     */
    private void powerOff()
    {
        UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
        er = mUHFMgr.powerOff();
        Toast.makeText(getApplicationContext(), "Power off :"+er.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 开始扫描
     */
    private void startReading()
    {
        UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
        er= mUHFMgr.startTagInventory();
        if(er == UHFReader.READER_STATE.OK_ERR)
            keepScreen();
        else
            Toast.makeText(getApplicationContext(), "Start reading :"+er.toString(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 停止扫描
     */
    private UHFReader.READER_STATE stopReading()
    {
        UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
        er = mUHFMgr.stopTagInventory();
        releseScreenLock();
        return er;
        //Toast.makeText(getApplicationContext(), "Stop reading :"+er.toString(), Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
        {
            if ((System.currentTimeMillis() - exittime) > 2000) {
                Toast.makeText(getApplicationContext(), "Press Again to Exit", Toast.LENGTH_SHORT).show();
                exittime = System.currentTimeMillis();
            }else
                return super.onKeyDown(keyCode, event);
        }

        AudioManager audioManager  = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_UP);
                return true;
        }

        return true;
    }
}
