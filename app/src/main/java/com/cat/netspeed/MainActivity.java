package com.cat.netspeed;

import android.app.Activity;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends Activity
{

    private TextView tv_type,tv_now_speed,tv_ave_speed;
    private TextView totalRxBytes, totalTxBytes,mobileRxBytes,mobileTxBytes;
    private Button btn;
    private ImageView needle;
    private boolean flag;
    private int last_degree = 0, cur_degree;

    private double lastTotalRxBytes;
    private long lastTimeStamp = 0;
    // 应用启动时间
    private long startTimeStamp ;
    // 应用启动时的总流量
    private double startTotalRxTxBytes;
    // 开始结束标识
    private boolean isStart = true;

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what==0x123)
            {
                String[] args = msg.obj.toString().split(";");
                // 当前网速
                String arg1 = args[0];
                // 平均网速
                String arg2 = args[1];
                tv_now_speed.setText(arg1 + "MB/s");
                tv_ave_speed.setText(arg2 + "MB/s");

                totalRxBytes.setText(Long.toString(TrafficStats.getTotalRxBytes()));
                totalTxBytes.setText(Long.toString(TrafficStats.getTotalTxBytes()));
                mobileRxBytes.setText(Long.toString(TrafficStats.getMobileRxBytes()));
                mobileTxBytes.setText(Long.toString(TrafficStats.getMobileTxBytes()));

                // 仪表盘
                startAnimation(Double.parseDouble(arg1));
            }
            else if(msg.what==0x100) // 结束统计
            {
                flag = false;
                tv_now_speed.setText("0KB/s");
                tv_ave_speed.setText("0KB/s");
                startAnimation(0);
                btn.setTextColor(Color.BLACK);
                btn.setText("开始");
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //tv_type = (TextView) findViewById(R.id.connection_type);
        tv_now_speed = (TextView) findViewById(R.id.now_speed);
        tv_ave_speed = (TextView) findViewById(R.id.ave_speed);
        needle = (ImageView) findViewById(R.id.needle);
        // 开始按钮
        btn = (Button) findViewById(R.id.start_btn);
        isStart = true;

        totalRxBytes = findViewById(R.id.totalRxBytes);
        totalTxBytes = findViewById(R.id.totalTxBytes);
        mobileRxBytes = findViewById(R.id.mobileRxBytes);
        mobileTxBytes = findViewById(R.id.mobileTxBytes);

        // 开始按钮点击事件
        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                if (isStart == true) // 点击开始
                {
                    // 初始化参数
                    isStart = false;
                    flag = true;
                    startTimeStamp = System.currentTimeMillis();
                    lastTimeStamp = System.currentTimeMillis();
                    startTotalRxTxBytes = AppUtils.div((getMobileTxBytes() + getMobileRxBytes()),1024,2); // MB
                    //startTotalRxTxBytes = AppUtils.div((getTotalRxBytes() + getTotalTxBytes()),1024,2); // MB
                    lastTotalRxBytes = startTotalRxTxBytes;
                    // For Test
                    //startTotalRxTxBytes = Double.parseDouble("1");

                    // 获取连接方式
                    //ConnectivityManager connectivityManager=(ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    //NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    //tv_type.setText(networkInfo.getTypeName());

                    btn.setTextColor(Color.BLUE);
                    btn.setText("结束");
                    // 开始统计网速
                    new GetInfoThread().start();
                }
                else // 点击结束
                {
                    isStart = true;
                    handler.sendEmptyMessage(0x100);
                }
            }
        });
    }

    private long getTotalUidRxBytes() {
        return TrafficStats.getUidRxBytes(this.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 :(TrafficStats.getTotalRxBytes()/1024);//转为KB
    }
    public long getTotalRxBytes(){  //获取总的接受字节数，包含Mobile和WiFi等
        return TrafficStats.getTotalRxBytes()==TrafficStats.UNSUPPORTED?0:(TrafficStats.getTotalRxBytes()/1024);
    }
    public long getTotalTxBytes(){  //总的发送字节数，包含Mobile和WiFi等
        return TrafficStats.getTotalTxBytes()==TrafficStats.UNSUPPORTED?0:(TrafficStats.getTotalTxBytes()/1024);
    }
    public long getMobileRxBytes(){  //获取通过Mobile连接收到的字节总数，不包含WiFi
        //return 1024*10*10;
        return TrafficStats.getMobileRxBytes() == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getMobileRxBytes()/1024);
    }
    public long getMobileTxBytes(){  //获取通过Mobile连接发送的字节总数，不包含WiFi
        //return 500;
        return TrafficStats.getMobileTxBytes() == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getMobileTxBytes()/1024);
    }

    /**
     * 计算网速
     */
    class GetInfoThread extends Thread
    {
        @Override
        public void run()
        {
            // TODO Auto-generated method stub
            try
            {
                while(flag)
                {
                    Thread.sleep(1000);

                    // 当前接收和发送的总流量 单位：MB
                    double nowTotalRxBytes = AppUtils.div((getMobileRxBytes() + getMobileTxBytes()), 1024, 2);
                    //double nowTotalRxBytes = AppUtils.div((getTotalRxBytes() + getTotalTxBytes()), 1024, 2);
                    long nowTimeStamp = System.currentTimeMillis();
                    double tmp1 = AppUtils.sub(nowTotalRxBytes, lastTotalRxBytes);
                    double tmp2 = AppUtils.mul(tmp1, 1000);//毫秒转换
                    double curSpeed = AppUtils.div(tmp2, (nowTimeStamp - lastTimeStamp), 2);

                    // 平均速度 MB
                    tmp1 = AppUtils.sub(nowTotalRxBytes, startTotalRxTxBytes);
                    tmp2 = AppUtils.mul(tmp1, 1000);//毫秒转换
                    double avgSpeed = AppUtils.div(tmp2, (nowTimeStamp - startTimeStamp), 2);

                    lastTimeStamp = nowTimeStamp;
                    lastTotalRxBytes = nowTotalRxBytes;
                    // 格式化速度值
                    String parVal = Double.toString(curSpeed) + ";" + Double.toString(avgSpeed);
                    if (isStart == true) // 因为此方法中有sleep延时，点击过结束按钮数据要清零
                    {
                        parVal = "0;0";
                    }
                    // For Test
                    //parVal = "5632;256";
                    Message msg = new Message();
                    msg.obj = parVal;
                    msg.what=0x123;
                    handler.sendMessage(msg);
                }

                if(flag)
                {
                    handler.sendEmptyMessage(0x100);
                }
            } catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onBackPressed()
    {
        // TODO Auto-generated method stub
        flag = false;
        super.onBackPressed();
    }

    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        flag = true;
        super.onResume();
    }

    private void startAnimation(double cur_speed)
    {
        //cur_degree = getDegree(cur_speed);
        cur_degree = getDegreeMB(cur_speed);

        RotateAnimation rotateAnimation = new RotateAnimation(last_degree, cur_degree, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(1000);
        last_degree = cur_degree;
        needle.startAnimation(rotateAnimation);
    }

    // 仪表盘以G为单位
    private int getDegreeMB(double cur_speed)
    {
        double maxVal = 6.0*1024;
        int ret = 0;
        if (cur_speed >=0 && cur_speed <= maxVal)
        {
            ret = (int)(180.0 * cur_speed / (6.0*1024));
        }
        else
        {
            ret = 180;
        }

        return ret;
    }

    private int getDegree(double cur_speed)
    {
        int ret = 0;
        if(cur_speed>=0 && cur_speed<=512)
        {
            ret = (int) (15.0*cur_speed/128.0);
        }
        else if(cur_speed>=512 && cur_speed<=1024)
        {
            ret = (int) (60+15.0*cur_speed/256.0);
        }
        else if(cur_speed>=1024 && cur_speed<=10*1024)
        {
            ret = (int) (90+15.0*cur_speed/1024.0);
        }else {
            ret = 180;
        }
        return ret;
    }

}
