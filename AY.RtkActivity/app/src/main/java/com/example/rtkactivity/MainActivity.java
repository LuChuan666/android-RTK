package com.example.rtkactivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.qxwz.sdk.configs.AccountInfo;
import com.qxwz.sdk.configs.SDKConfig;
import com.qxwz.sdk.core.CapInfo;
import com.qxwz.sdk.core.Constants;
import com.qxwz.sdk.core.IRtcmSDKCallback;
import com.qxwz.sdk.core.RtcmSDKManager;
import com.qxwz.sdk.types.KeyType;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static com.qxwz.sdk.core.Constants.QXWZ_SDK_CAP_ID_NOSR;
import static com.qxwz.sdk.core.Constants.QXWZ_SDK_STAT_AUTH_SUCC;
import static com.qxwz.sdk.types.KeyType.QXWZ_SDK_KEY_TYPE_DSK;

public class MainActivity extends Activity  implements IRtcmSDKCallback {

    Button bt_RTK;
    private static final String TAG = "qxwz";

    private static final String AK = "A4880i0plcp6";
    private static final String AS = "ad7c319411fc056ad67a600a8a872319bb85625179788b728c50cbb106ba7315";
    private static final String DEVICE_ID = "1910061127";
    private static final String DEVICE_TYPE = "ELE";

    private static final String GGA = "$GPGGA,000001,3112.518576,N,12127.901251,E,1,8,1,0,M,-32,M,3,0*4B";

    private boolean isStart = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_RTK = (Button) findViewById(R.id.ButtonRTK);



        bt_RTK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //按钮操作程序

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SDKConfig.Builder builder = SDKConfig.builder()
                .setAccountInfo(
                        AccountInfo.builder()
                                .setKeyType(KeyType.QXWZ_SDK_KEY_TYPE_AK)
                                .setKey(AK)
                                .setSecret(AS)
                                .setDeviceId(DEVICE_ID)
                                .setDeviceType(DEVICE_TYPE)
                                .build())
                .setRtcmSDKCallback(this);
        RtcmSDKManager.getInstance().init(builder.build());
        RtcmSDKManager.getInstance().auth();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isStart = false;
        //能⼒启动成功后可通过停⽌能⼒终⽌相关服务
        RtcmSDKManager.getInstance().stop(QXWZ_SDK_CAP_ID_NOSR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //初始化后的任何阶段都可以关闭SDK
        RtcmSDKManager.getInstance().cleanup();
    }

    @Override
    public void onData(int type, byte[] bytes) {
       // Log.d(TAG, "rtcm data received, data length is " + bytes.length);
        Log.d(TAG, "onData, dataType:" + type + ", len:" + bytes.length);
        try {
            String sss = new String(bytes,"UTF-8");
            String sssa = byte2HexStr(bytes);

            Log.d(TAG, "received data "+ sssa);
        }catch (UnsupportedEncodingException e){
            Log.d(TAG, "rtcm data received, failed "  + e.toString());
        }

    }

    @Override
    public void onStatus(int status) {
        Log.d(TAG, "status changed to " + status);
    }

    //SDK需完成鉴权后才能提供服务，鉴权⽅法是异步⽅法，鉴权结束后通过调⽤初始化时传⼊的回调接⼝实例中的onAuth⽅法通知⽤户鉴权结果
    @Override
    public void onAuth(int code, List<CapInfo> caps) {
        if (code == QXWZ_SDK_STAT_AUTH_SUCC) {
            Log.d(TAG, "auth successfully.");
            for (CapInfo capInfo : caps) {
                Log.d(TAG, "capInfo:" + capInfo.toString());
            }
            /* if you want to call the start api in the callback function, you must invoke it in a new thread. */
            new Thread() {
                public void run() {
  //鉴权成功后可启动能⼒获取相应服务，能⼒启动接⼝是异步⽅法，能⼒启动结束后通过调⽤初始化时传⼊的回调接⼝实例中的onStart⽅法通知⽤户启动结果
                    RtcmSDKManager.getInstance().start(QXWZ_SDK_CAP_ID_NOSR);
                }
            }.start();
        } else {
            Log.d(TAG, "failed to auth, code is " + code);
        }
    }

    @Override
    public void onStart(int code, int capId) {
        if (code == Constants.QXWZ_SDK_STAT_CAP_START_SUCC) {
            Log.d(TAG, "start successfully.");
            isStart = true;
// 启动成功后可通过每隔⼀段时间(间隔时间不短于1秒)上传GGA获取差分数据，差分数据通过调⽤初始化时传⼊的回调接⼝实例中的onData⽅法传递给⽤户
            new Thread() {
                public void run() {
                    while (isStart) {
                        RtcmSDKManager.getInstance().sendGga(GGA);
                        SystemClock.sleep(1000);
                    }
                }
            }.start();
        } else {
            Log.d(TAG, "failed to start, code is " + code);
        }
    }
    public static String byte2HexStr(byte[] b)
    {
        String stmp="";
        StringBuilder sb = new StringBuilder("");
        for (int n=0;n<b.length;n++)
        {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length()==1)? "0"+stmp : stmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }
}

