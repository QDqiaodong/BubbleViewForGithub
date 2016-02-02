package lenovo.com.bubbleviewforgithub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Scroller;

/**
 * 可以产生形变，回弹的小球
 *
 * 整个view的核心：
 * 		Scroller函数
 * 		1.调用scroller.public void startScroll(int startX, int startY, int dx, int dy, int duration);
 * 		     在指定时间范围内，startX,startY,会移动dx,dy距离
 *
 * 		2.然后调用:invalidate(); 系统内部会调用onDraw();
 *
 * 		3.在onDraw方法内部又会调用  computeScrollOffset()函数。
 * 		   所以，实现scroll.computeScrollOffset(); //如果还没有完全移动完成，就执行XXXXX
 *
 * 		4.如果返回true 继续调用 invalidate();
 *
 *		这样就会在位移的过程中，执行你:  if(scroll.computeScrollOffset()){
 *		 					   				//你自己的方法
 *									   }
 *
 */
public class BallViewTwo extends ImageView {

	private Context mContext;

	//屏幕的宽高
	private int displayHeight;
	private int displayWidth;

	private  int changelength = 30; //第一次形变的大小
	private int mCurrentDirection = -1; //碰撞后，此时的方向
	private int mDuration  = 450;  //变形需要持续的时间
	/**
	 * flag=-1 正常移动
	 * flag=0 压缩变形
	 * flag=1 恢复压缩形变
	 * flag=2 往相反的方向弹
	 * flag=3 弹回原先的位置
	 */
	private int flag = -1;
	private ShotOver mShotOver;  //回调函数
	private int moveToLeft = 100;  //正常状态下，小球移动到x轴的位置
	private int moveToTop = 100;   //正常状态下，小球移动到y轴的位置
	private int centerX = 180;   //小球圆心x
	private int centerY = 180;   //小球圆心y
	private int radius = 180;    //半径
	private int bubbleWidth = radius * 2;  //小球的宽
	private int bubbleHeight = radius * 2; //小球的高

	private Paint paint;   //画笔
	private Scroller scroller; //整个view的核心
	private RectF rectF;    //绘制椭圆
	private int ovalLeft,ovalTop,ovalRight,ovalBottom; //椭圆的左上右下
	private int currY;
	private int currX;
	private int offset;  //发生的移动量

	private int shotBackDuration = 100;  //回弹执行的时间
	private int shotBackChange = 15;     //回弹需要移动的距离
	private int newOvalLeft;
	private int newOvalTop;
	private int newOvalRight;
	private int newOvalBottom;

	private boolean isShotBack = true; //是否开启回弹效果

	/*private int oldOffset = 0;
        private int gradient = 0;*/

	public BallViewTwo(Context context) {
		super(context);
	}


	public BallViewTwo(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		paint = new Paint();
		paint.setColor(Color.RED);
		scroller = new Scroller(context);
		rectF = new RectF();
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		displayHeight = mContext.getResources().getDisplayMetrics().heightPixels;
		displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
		Log.i("qq","ballview-------displayHeight=-"+displayHeight+" displayWidth="+displayWidth);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}


	@Override
	 protected void onDraw(Canvas canvas) {
		switch (flag){
			case -1:
				canvas.translate(moveToLeft, moveToTop);
				canvas.drawCircle(centerX, centerY, radius, paint);
				Log.i("qq", "正常移动小球 小球left="+moveToLeft + " top="+moveToTop);
				break;

			case 0:
				circleToOval(canvas);
				break;

			case 1:
				ovalToCircle(canvas);
				break;
			case 2:
				shotBackLeaveBounds(canvas);
				break;
			case 3:
				shotBackGotoBounds(canvas);
		}
		super.onDraw(canvas);
	}

	/**
	 * 	小球变形完再回弹,靠近边界
	 */
	private void shotBackGotoBounds(Canvas canvas) {
		if(scroller.computeScrollOffset()){
			ovalLeft = scroller.getCurrX();
			ovalTop = scroller.getCurrY();
			canvas.translate(ovalLeft, ovalTop);
			canvas.drawCircle(centerX, centerY, radius, paint);
			invalidate();
			Log.i("shotBack", "远离边界。。moveToLeft=" + ovalLeft + " moveToTop=" + ovalTop);
		}else{
			Log.i("shotBack", "所有效果都结束");
			canvas.translate(ovalLeft, ovalTop);
			canvas.drawCircle(centerX, centerY, radius, paint);
			isShotBack = false;
			startChange(shotBackChange);
		}
	}

	/**
	 * 小球变形完再回弹，也就是远离边界
	 */
	private void shotBackLeaveBounds(Canvas canvas) {
		if(scroller.computeScrollOffset()){
			ovalLeft = scroller.getCurrX();
			ovalTop = scroller.getCurrY();
			canvas.translate(ovalLeft, ovalTop);
			canvas.drawCircle(centerX, centerY, radius, paint);
			invalidate();
			Log.i("shotBack", "远离边界。。moveToLeft=" + ovalLeft + " moveToTop=" + ovalTop);
		}else{
			canvas.translate(ovalLeft, ovalTop);
			canvas.drawCircle(centerX, centerY, radius, paint);
			flag = 3;
			finishShotBack();
		}
	}


	/**
	 * 	将椭圆恢复成圆形
	 */
	private void ovalToCircle(Canvas canvas) {
		if(scroller.computeScrollOffset()){
			switch (mCurrentDirection){
				case 0:
					currY = scroller.getCurrY();
					offset = newOvalTop - currY;
					ovalLeft = newOvalLeft + offset;
					ovalTop = currY + offset;
					ovalRight = newOvalRight - offset;
					ovalBottom = newOvalBottom + offset+offset;
					rectF.set(ovalLeft, ovalTop, ovalRight, ovalBottom);
					canvas.drawOval(rectF, paint);

					Log.i("qq", "将椭圆----恢复成圆形，方向向北 currY=" + currY + " offset=" + offset);
					Log.i("qq", "将椭圆----恢复成圆形，方向向北 ovalLeft=" + ovalLeft + " ovalTop=" + ovalTop + " ovalRight=" + ovalRight + " ovalBottom=" + ovalBottom);

					break;
				case 1:
					currX = scroller.getCurrX();
					offset = newOvalLeft - currX;
					ovalLeft = currX - offset;
					ovalTop = newOvalTop + offset;
					ovalRight = newOvalRight + offset - offset;
					ovalBottom = newOvalBottom  - offset;
					rectF.set(ovalLeft, ovalTop, ovalRight, ovalBottom);
					canvas.drawOval(rectF, paint);

					break;
				case 2:
					currY = scroller.getCurrY();
					offset = newOvalTop - currY;
					ovalLeft = newOvalLeft + offset;
					ovalTop = currY - offset;
					ovalRight = newOvalRight - offset;
					ovalBottom = newOvalBottom;
					rectF.set(ovalLeft, ovalTop, ovalRight, ovalBottom);
					canvas.drawOval(rectF, paint);

					Log.i("qq", "将椭圆----恢复成圆形，方向向南 currY=" + currY + " offset=" + offset);
					Log.i("qq","将椭圆----恢复成圆形，方向向南 ovalLeft="+ ovalLeft + " ovalTop="+ ovalTop +" ovalRight="+ ovalRight +" ovalBottom="+ ovalBottom);

					break;
				case 3:
					currX = scroller.getCurrX();
					offset = newOvalLeft - currX;
					ovalLeft = currX + offset;
					ovalTop = newOvalTop + offset;
					ovalRight = newOvalRight + offset + offset;
					ovalBottom = newOvalBottom  - offset;
					rectF.set(ovalLeft, ovalTop, ovalRight, ovalBottom);
					canvas.drawOval(rectF, paint);
					break;
			}
			invalidate();
		}else{
			canvas.drawOval(rectF, paint);

			//如果需要回弹的话
			if(isShotBack){
				flag = 2;
				startShotBack();
			}else{
				flag = -1;
				if(mShotOver != null){
					mShotOver.bubbleShotEnd();
				}
			}


		}
	}

	/**
	 * 圆挤压成椭圆
	 */
	private void  circleToOval(Canvas canvas){
		if(scroller.computeScrollOffset()){
			switch (mCurrentDirection){
				case 0:
					currY = scroller.getCurrY();
					offset = currY - ovalTop;
					newOvalLeft = ovalLeft - offset;
					newOvalTop = currY - offset;
					newOvalRight = ovalRight + offset;
					newOvalBottom = ovalBottom - offset - offset;
					rectF.set(newOvalLeft, newOvalTop, newOvalRight, newOvalBottom);
					canvas.drawOval(rectF, paint);
					break;
				case 1:
					currX = scroller.getCurrX();
					offset = currX - ovalLeft;
					newOvalLeft = currX + offset;
					newOvalTop = ovalTop - offset;
					newOvalRight = ovalRight - offset + offset;
					newOvalBottom = ovalBottom + offset;
					rectF.set(newOvalLeft, newOvalTop , newOvalRight, newOvalBottom );
					canvas.drawOval(rectF, paint);

					break;
				case 2:
					currY = scroller.getCurrY();
					offset = currY - ovalTop;
					newOvalLeft = ovalLeft - offset;
					newOvalTop = currY + offset;
					newOvalRight = ovalRight + offset;
					newOvalBottom = ovalBottom;
					rectF.set(newOvalLeft, newOvalTop , newOvalRight, newOvalBottom );
					canvas.drawOval(rectF, paint);

					Log.i("qq", "圆挤压成椭圆，方向向南 currY=" + currY + " offset=" + offset);
					Log.i("qq","圆挤压成椭圆，方向向南 newOvalLeft="+ newOvalLeft + " newOvalTop="+ newOvalTop +" newOvalRight="+ newOvalRight +" newOvalBottom="+ newOvalBottom);

					break;
				case 3:
					currX = scroller.getCurrX();
					offset = currX - ovalLeft;
					newOvalLeft = currX - offset;
					newOvalTop = ovalTop - offset;
					newOvalRight = ovalRight - offset - offset;
					newOvalBottom = ovalBottom + offset;
					rectF.set(newOvalLeft, newOvalTop , newOvalRight, newOvalBottom );
					canvas.drawOval(rectF, paint);
					break;
			}
			invalidate();
		}else{
			canvas.drawOval(rectF, paint);
			reverse();
		}
	}


	/**
	 * 	碰撞变形结束后，开启弹一弹效果
	 */
	public void startShotBack(){
		switch (mCurrentDirection){
			case 0:
				scroller.startScroll(ovalLeft,ovalTop,0,shotBackChange,shotBackDuration );
				break;
			case 1:
				scroller.startScroll(ovalLeft,ovalTop,-shotBackChange,0,shotBackDuration );
				break;
			case 2:
				scroller.startScroll(ovalLeft,ovalTop,0,-shotBackChange,shotBackDuration );
				break;
			case 3:
				scroller.startScroll(ovalLeft,ovalTop,shotBackChange,0,shotBackDuration );
				break;
		}
		invalidate();
	}

	/**
	 * 	结束 “弹的一段距离”
	 */
	public void finishShotBack(){
		switch (mCurrentDirection){
			case 0:
				scroller.startScroll(ovalLeft,ovalTop,0,-shotBackChange,shotBackDuration );
				break;
			case 1:
				scroller.startScroll(ovalLeft,ovalTop,shotBackChange,0,shotBackDuration );
				break;
			case 2:
				scroller.startScroll(ovalLeft,ovalTop,0,shotBackChange,shotBackDuration );
				break;
			case 3:
				scroller.startScroll(ovalLeft,ovalTop,-shotBackChange,0,shotBackDuration );
				break;
		}
		invalidate();
	}

	/**
	 * 移动小球
	 */
	public void moveTo(int l,int t,int direction,int duration,boolean shotBack){
		isShotBack = shotBack;
		mDuration  = duration;
		mCurrentDirection = direction;
		moveToLeft = l;
		moveToTop = t;

		if(t == 0 ) {
			mCurrentDirection = 0;
			startChange(30);
		}else if(l == displayWidth-bubbleWidth ){
			mCurrentDirection = 1;
			startChange(30);
		}else if(t == displayHeight - bubbleHeight){
			mCurrentDirection = 2;
			startChange(30);
		}else if( l == 0){
			mCurrentDirection = 3;
			startChange(30);
		}else{
			invalidate();
		}

	}

	/**
	 * 开始变形
	 */
	private void startChange(int change){
		changelength = change;
		if(mShotOver!= null){
			mShotOver.bubbleShotStart(mCurrentDirection);
		}
		flag = 0;
		//发生变形时，先初始化椭圆刚发生变形时的位置
		ovalLeft = moveToLeft;
		ovalTop = moveToTop;
		ovalRight = moveToLeft + bubbleWidth;
		ovalBottom = ovalTop + bubbleHeight;

		switch (mCurrentDirection){
			case 0:
				scroller.startScroll(moveToLeft, moveToTop, 0, changelength, mDuration );
				break;
			case 1:
				scroller.startScroll(moveToLeft, moveToTop, changelength, 0, mDuration );
				break;
			case 2:
				scroller.startScroll(moveToLeft, moveToTop, 0, changelength, mDuration );
				break;
			case 3:
				scroller.startScroll(moveToLeft, moveToTop, changelength, 0, mDuration );
				break;
		}

		Log.i("qq", "小球开始变形，方向=" + mCurrentDirection + " 当前小球的坐标ovalLeft=" + ovalLeft+" ovalTop="+ovalTop+" ovalRight="+ovalRight+" ovalBottom="+ovalBottom);
		invalidate();
	}

	/**
	 * 回复变形前的状态
	 */
	private void reverse(){
		flag = 1;
		switch (mCurrentDirection){
			case 0:
				scroller.startScroll(newOvalLeft,newOvalTop,0,-changelength, mDuration );
				break;
			case 1:
				scroller.startScroll(newOvalLeft,newOvalTop,-changelength,0, mDuration );
				break;
			case 2:
				scroller.startScroll(newOvalLeft,newOvalTop,0,-changelength, mDuration );
				break;
			case 3:
				scroller.startScroll(newOvalLeft,newOvalTop,-changelength,0, mDuration );
				break;
		}

		invalidate();
	}

	public void setShotOver(ShotOver shotOver){
		mShotOver = shotOver;
	}

	/**
	 * 碰撞变形效果完成
	 */
	public interface ShotOver{
		void bubbleShotStart(int direction);
		void bubbleShotEnd();
	}

	/**
	 * (辅助函数）//没有实现效果
	 * 圆挤压成椭圆时,变形加速减少
	 *//*
	public void circleToOvalAndAcceleratedReduce(Canvas canvas){

		if(gradient == 0){
			newOvalLeft = ovalLeft;
			newOvalTop = ovalTop;
			newOvalRight = ovalRight;
			newOvalBottom = ovalBottom;
		}

		if((offset - oldOffset) >= 10 && gradient == 0){

			Log.i("qq", "移动距离大于10时绘制一次");

			newOvalLeft = ovalLeft - offset;
			newOvalTop = currY - offset;
			newOvalRight = ovalRight + offset;
			newOvalBottom = ovalBottom - offset - offset;
			rectF.set(newOvalLeft, newOvalTop, newOvalRight, newOvalBottom);
			canvas.drawOval(rectF, paint);
			oldOffset = offset;
			gradient = 1;
		}else if((offset - oldOffset) >= 7 && gradient == 1){
			Log.i("qq", "移动距离大于7时绘制一次");
			newOvalLeft = ovalLeft - offset;
			newOvalTop = currY - offset;
			newOvalRight = ovalRight + offset;
			newOvalBottom = ovalBottom - offset - offset;
			rectF.set(newOvalLeft, newOvalTop, newOvalRight, newOvalBottom);
			canvas.drawOval(rectF, paint);
			gradient = 2;
			oldOffset = offset;
		}else if((offset - oldOffset) >= 4 && gradient == 2){
			Log.i("qq","移动距离大于4时绘制一次");
			newOvalLeft = ovalLeft - offset;
			newOvalTop = currY - offset;
			newOvalRight = ovalRight + offset;
			newOvalBottom = ovalBottom - offset - offset;
			rectF.set(newOvalLeft, newOvalTop, newOvalRight, newOvalBottom);
			canvas.drawOval(rectF, paint);
			gradient = 3;
			oldOffset = offset;

		}else if((offset - oldOffset) >= 2 && gradient == 3){
			Log.i("qq","移动距离大于2时绘制一次");
			newOvalLeft = ovalLeft - offset;
			newOvalTop = currY - offset;
			newOvalRight = ovalRight + offset;
			newOvalBottom = ovalBottom - offset - offset;
			rectF.set(newOvalLeft, newOvalTop, newOvalRight, newOvalBottom);
			canvas.drawOval(rectF, paint);
			gradient = -1;
			oldOffset = offset;

		}else{
			Log.i("qq", "移动绘制一次");
			rectF.set(newOvalLeft, newOvalTop, newOvalRight, newOvalBottom);
			canvas.drawOval(rectF, paint);
		}

		Log.i("qq", "圆挤压成椭圆，方向向北 currY=" + currY + " offset=" + offset);
		Log.i("qq", "圆挤压成椭圆，方向向北 newOvalLeft=" + newOvalLeft + " newOvalTop=" + newOvalTop + " newOvalRight=" + newOvalRight + " newOvalBottom=" + newOvalBottom);
	}*/

}

