package ng.duc.mercury.custom_views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import ng.duc.mercury.Utility;

/**
 * Created by ducnguyen on 6/28/16.
 * This modified recycler view allows the parent scrolling view to catch the
 * scroll when the user wants to scroll up while this view is already on top
 */
public class PersonalRecyclerView extends RecyclerView implements
		GestureDetector.OnGestureListener {

	private float oldX;
	private float oldY;

	private GestureDetectorCompat mDetector;

	private static final String LOG_TAG = PersonalRecyclerView.class.getSimpleName();

	public PersonalRecyclerView(Context context) {
		super(context);
	}

	public PersonalRecyclerView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public PersonalRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mDetector = new GestureDetectorCompat(getContext(), this);
	}

	//	@Override
//	public boolean onTouchEvent(MotionEvent e) {
//
//		if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
//			X = e.getX();
//			Y = e.getY();
//		}
//
//		LinearLayoutManager layout = (LinearLayoutManager) getLayoutManager();
//		if (layout == null) return super.onTouchEvent(e);
//
//		if (layout.findFirstCompletelyVisibleItemPosition() != 0) {
//			getParent().requestDisallowInterceptTouchEvent(true);
//
//		} else {
//			if ((e.getActionMasked() == MotionEvent.ACTION_MOVE) &&
//				(e.getY() > Y)){
//				Log.v(LOG_TAG, "onTouchEvent is false");
//				getParent().requestDisallowInterceptTouchEvent(false);
//				return false;
//			}
//		}
//
//		return super.onTouchEvent(e);
//
//	}


	@Override
	public boolean onTouchEvent(MotionEvent e) {
		// Uncomment this line for gesture detector to work
		this.mDetector.onTouchEvent(e);

//		int action = e.getActionMasked();
//
//		switch (action) {
//
//			case MotionEvent.ACTION_DOWN: {
//				oldX = e.getX();
//				oldY = e.getY();
//				break;
//			}
//
//			case MotionEvent.ACTION_MOVE: {
//
//			}
//
//
//
//		}

		return super.onTouchEvent(e);
	}

	// TODO: touch event might work now, but a complete overhaul later would bring us more control on how to improve
	@Override
	public boolean onDown(MotionEvent e) {

		boolean a = false;

//		getParent().requestDisallowInterceptTouchEvent(true);
//		Log.v(LOG_TAG, "onDown: " + e.toString());

		LinearLayoutManager layout = (LinearLayoutManager) getLayoutManager();
		if ((layout != null) &&
			(layout.findFirstCompletelyVisibleItemPosition() == 0)){
			getParent().requestDisallowInterceptTouchEvent(false);
			a = true;
		}

		if (e.getX() <= Utility.dpsToPxRaw(64, getContext())) {
			getParent().requestDisallowInterceptTouchEvent(false);
			a = true;
		}

		if (!a) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

//		Log.v(LOG_TAG, "In control");

		LinearLayoutManager layout = (LinearLayoutManager) getLayoutManager();
		if (layout == null) return true;

		if (layout.findFirstCompletelyVisibleItemPosition() != 0) {
			if (Math.abs(velocityX) > Math.abs(velocityY)) {
				getParent().requestDisallowInterceptTouchEvent(false);
//				Log.v(LOG_TAG, "AbsX = " + Math.abs(velocityX)
//						+ " AbsY = " + Math.abs(velocityY));
//				Log.v(LOG_TAG, "requestIntercept = false");
			}
		} else {
			getParent().requestDisallowInterceptTouchEvent(false);
			if (velocityY >= 0) {
				return false;
			}
		}

		// Turn this back to true to fling
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {

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
	public void onShowPress(MotionEvent e) {

	}
}
