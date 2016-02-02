package lenovo.com.bubbleviewforgithub;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/***
 *  1.根据重力感应移动小球
 *  2.一般重力感应使用的重力加速，这样的话每次移动距离不是固定的
 *  3.此应用，将速度固定。
 *  4.小球碰撞到边缘时，会发生形变。（小球压缩形变）
 *  5.可以点击按钮添加，发生形变后回弹效果
 */
public class MainActivity extends Activity implements View.OnClickListener{

    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private boolean init = false;

    //因为布局是填充父窗体的，且设置了出掉状态栏，所有求出的宽高就是屏幕的宽高。
    private int container_width = 0;
    private int container_height = 0;

    //小球的宽高
    private int ball_width = 360;
    private int ball_height = 360;

    //自定义球
    private BallViewTwo ball;

    //小球的起始位置
    private float ballX = 100;
    private float ballY = 100;

    private  int currentState = -1; //初始方向
    private int shotDirection = -1; //小球发生碰撞时的那刻的方向

    //初始化 东 西 南 北 四个方向
    private final int NORTH = 0;
    private final int EAST = 1;
    private final int SOUTH = 2;
    private final int WEST = 3;

    private int constantsSpeed = 100; //每次斜边移动的距离
    private final int SPEED = 10;//比例
    private boolean canMove = true; //小球是否可以移动


    private int durationPiece = 150;  //执行动画的时间块
    private int duration = 450;      //初始速度下的执行时间
    private boolean isShotBack = false;
    /**
     * -1:小球正常移动
     * 0:小球正在碰撞
     * 1：小球碰撞刚结束
     */
    private int shot = -1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //初始化重力感应
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void init(){
        View container = findViewById(R.id.ball_container);
        findViewById(R.id.accelerate).setOnClickListener(this);  //加速移动
        findViewById(R.id.reduce).setOnClickListener(this);     //减少移动
        findViewById(R.id.normal).setOnClickListener(this);     //正常速度移动
        findViewById(R.id.isShowBack).setOnClickListener(this); //是否弹一弹

        container_width = container.getWidth();
        container_height = container.getHeight();
        ball = (BallViewTwo) findViewById(R.id.ball);

        /**
         * 碰撞监听
         */
        ball.setShotOver(new BallViewTwo.ShotOver() {

            @Override
            public void bubbleShotStart(int direction) {
                shotDirection = direction;
                shot = 0;  //正在压缩变形
                canMove = false;
                // Log.i("shotDirection", "小球发生碰撞时的方向==" + shotDirection);
            }

            @Override
            public void bubbleShotEnd() {
                shot = 1; //刚压缩变形结束
            }
        });
    }


    /**
     * 移动小球
     * @param x
     * @param y
     */
    void moveTo(float x, float y) {
        ballX +=x;
        ballY +=y;

        if (ballX < 0 ){
            ballX = 0;
        }

        if (ballY < 0){
            ballY = 0;
        }

        if (ballX > container_width - ball_width){
            ballX = container_width - ball_width;
        }

        if (ballY > container_height - ball_height){
            ballY = container_height - ball_height;
        }

        ball.moveTo((int) ballX, (int) ballY, currentState,duration,isShotBack);

    }


    SensorEventListener listener = new SensorEventListener(){
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!init)
                return;

                currentState = calc(event);

                //如果当前方向不等于碰撞方向 并且圆刚碰撞结束
                if(shotDirection !=currentState && shot==1){
                  canMove = true;
                }
                 Log.i("direction", "当前方向==" + currentState+" canMove="+canMove);

                //可以移动的话才计算移动速度，调用移动方法
                if(canMove){

                    //如果刚碰撞结束，根据位置，将其挪动一段距离
                    if(shot == 1){
                        switch (shotDirection){
                            case 0:
                                moveTo(0,20);
                                break;
                            case 1:
                                moveTo(-20,0);
                                break;
                            case 2:
                                moveTo(0,-20);
                                break;
                            case 3:
                                moveTo(20,0);
                                break;
                        }
                        shot = -1;

                     //直接移动小球
                    }else{
                        constantSpeed(event);
                    }

                }else{
                    Log.i("qq","正在执行“弹”，所以先不移动小球");
                }

        }
    };

    /**
     * 计算x,y轴应该移动的值（为了使每次移动距离保持不变）
     * @param event
     */
    public void constantSpeed(SensorEvent event){
        double movey;
        double movex;
        float x = event.values[SensorManager.DATA_X]*SPEED;
        float y = event.values[SensorManager.DATA_Y]*SPEED;
        float tan = x / y;

        if( x == 0 && y!=0){
            movex = 0;
            movey = constantsSpeed;
        }else if(x != 0 && y == 0){
            movex = constantsSpeed;
            movey = 0;
        } else if(x == 0 && y == 0){
            movex = 0;
            movey = 0;
        } else {
            double temp = constantsSpeed / (tan*tan+1);
            movey = Math.sqrt(temp); //开根号
            movex = movey * tan;
        }

        if(x < 0 ){
            movex = -Math.abs(movex);
           // Log.i("qd","转化x");
        }else if(x > 0){
            movex = Math.abs(movex);
        }

        if(y < 0 ){
            movey= -Math.abs(movey);
          //  Log.i("qd","转化y");
        } else if(y > 0){
            movey= Math.abs(movey);
        }

        moveTo(-(float) movex,(float)movey);
    }


    //注册重力感应监听
    public void register(){
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    //解除重力感应监听
    public void unregister(){
        sensorManager.unregisterListener(listener);
        Log.i("vv", "结束监听");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && !init){
            init();
            init = true;
        }
    }

    /**
     *  计算当前球的方向
     */
    public int calc(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        int temp_vertical = -1;  //上下偏
        int temp_horizontal = -1;//左右偏
        if(x > 0 ){
            //左偏，理解为西
            temp_horizontal = WEST;
        }else if(x < 0){
            //右偏，理解为东
            temp_horizontal = EAST;
        }

        if(y > 0 ){
            //下偏，理解为南
            temp_vertical = SOUTH;
        }else if(y < 0){
            //上偏，理解为北
            temp_vertical = NORTH;
        }

            //东北角
        if( temp_horizontal == EAST &&  temp_vertical == NORTH){
            if(Math.abs(x) > Math.abs(y)){
                currentState = EAST;
            }else if(Math.abs(x) < Math.abs(y)){
                currentState = NORTH;
            }
            //东南
        }else if(temp_horizontal == EAST &&  temp_vertical == SOUTH){
            if(Math.abs(x) > Math.abs(y)){
                currentState = EAST;
            }else if(Math.abs(x) < Math.abs(y)){
                currentState = SOUTH;
            }


            //西北
        }else if(temp_horizontal == WEST &&  temp_vertical == NORTH){
            if(Math.abs(x) > Math.abs(y)){
                currentState = WEST;
            }else if(Math.abs(x) < Math.abs(y)){
                currentState = NORTH;
            }

            //西南
        }else if(temp_horizontal == WEST &&  temp_vertical == SOUTH){
            if(Math.abs(x) > Math.abs(y)){
                currentState = WEST;
            }else if(Math.abs(x) < Math.abs(y)){
                currentState = SOUTH;
            }
        }
        return currentState;
    }



    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregister();
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        unregister();
    }


    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        super.onRestart();
        register();
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        register();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.accelerate:
                adjustSpeedAndDuration(5);
                break;
            case R.id.reduce:
                adjustSpeedAndDuration(-5);
                break;
            case R.id.normal:
                constantsSpeed = 10;
                duration = 450;
                break;
            case R.id.isShowBack:
                isShotBack = !isShotBack;
                break;
        }
    }

    /**
     *改变小球的移动速度和变形时间
     *因为移动速度越快，碰撞时间越短
     */
    public void adjustSpeedAndDuration(int change){
        constantsSpeed += change;

        if(change < 0){
            duration += durationPiece;
        }else{
            duration -= durationPiece;
        }

        if(constantsSpeed <= 5){
            constantsSpeed = 5;
            duration = 750;
        }else if(constantsSpeed >= 25){
            constantsSpeed = 25;
            duration = 150;
        }
    }
}
