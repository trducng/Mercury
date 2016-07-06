package ng.duc.mercury.custom_views;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by ducnguyen on 6/14/16.
 * This is a test view, which is used to see how nested scrolling events work in Android
 */
public class MainNestedScrollView extends NestedScrollView implements
		GestureDetector.OnGestureListener {

	private GestureDetectorCompat mDetector;
	private int MOTION;

	private int MOTION_SCROLL_UP = 0;
	private int MOTION_SCROLL_DOWN = 1;
	private int MOTION_FLING_UP = 2;
	private int MOTION_FLING_DOWN = 3;

	public MainNestedScrollView(Context context) {
		super(context);
		init();
	}

	public MainNestedScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mDetector = new GestureDetectorCompat(getContext(), this);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		Log.v("MainNestedScrollView", "onInterceptTouchEvent");
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

//		Log.v("MainNestedScrollView", "onTouchEvent");

		this.mDetector.onTouchEvent(ev);
		return super.onTouchEvent(ev);

	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

		if (e2.getY() > e1.getY()) {
			MOTION = MOTION_FLING_UP;
		} else if (e2.getY() < e1.getY()) {
			MOTION = MOTION_FLING_DOWN;
		}

		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		Log.v("TEST", "Show press");
	}

	@Override
	public void onLongPress(MotionEvent e) {
		Log.v("TEST", "Long press");
	}
}
