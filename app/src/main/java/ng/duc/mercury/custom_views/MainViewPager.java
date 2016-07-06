package ng.duc.mercury.custom_views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * Created by ducnguyen on 6/29/16.
 * This new view pager class is designed to disobey child view's request not to intercept
 * but at the same time pass the child's request to this group's parents.
 */
public class MainViewPager extends ViewPager {

	public MainViewPager(Context context) {
		super(context);
	}

	public MainViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Current situation:
	 *  ________________________________________________
	 * |ViewGroup A                                     |
	 * |                                                |
	 * |              ________________________________  |
	 * |             |ViewGroup B                     | |
	 * |             |             ________________   | |
	 * |             |            |View C          |  | |
	 * |             |            |                |  | |
	 * |             |            |                |  | |
	 * |             |            |                |  | |
	 * |             |            |________________|  | |
	 * |             |________________________________| |
	 * |________________________________________________|
	 *
	 *
	 * Mechanism:
	 *  When the user touches the vicinity of View C, the Android framework will notify
	 *  the activity about an ACTION_DOWN event. The activity will then passes that event
	 *  down to the view hierarchy. Since all ViewGroups tend to ignore ACTION_DOWN, and
	 *  only pick other kinds of events, for View C to protect its priority, it will
	 *  request parent view groups to not intercept its touch event.
	 *
	 *  However, we want this ViewPager to intercept, since it is a horizontal ViewPager
	 *  and it must listens to whenever user swipes left or right, so we modify this
	 *  method in a way that it will disobeys the child view request not to intercept,
	 *  while at the same time pass the original requests to this view pager's parent
	 *  view groups.
	 *
	 * @param disallowIntercept true to intercept, false otherwise
	 */
	@Override
	public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

		if (disallowIntercept) {
			super.requestDisallowInterceptTouchEvent(false);
			getParent().requestDisallowInterceptTouchEvent(true);
		}

	}
}
