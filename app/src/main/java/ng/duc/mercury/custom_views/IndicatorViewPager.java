package ng.duc.mercury.custom_views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

import ng.duc.mercury.R;

/**
 * Created by ducnguyen on 8/14/16.
 * This is the dot indicator that goes with View Pager. Looks as follow:
 *  _______________________
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |                       |
 * |         o o o         |
 * |_______________________|
 *
 * To use this class, just supply the number of dots.
 *
 */
public class IndicatorViewPager extends LinearLayout
		implements ViewPager.OnPageChangeListener {

	private static final String LOG_TAG = IndicatorViewPager.class.getSimpleName();
	private ArrayList<CircleButton> mButtons = new ArrayList<>();

	private int mPages;

	public IndicatorViewPager(Context context) {
		this(context, null);
	}

	public IndicatorViewPager(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public IndicatorViewPager(Context context, AttributeSet attr, int defStyleAttr) {
		super(context, attr, defStyleAttr);
	}

	@TargetApi(21)
	public IndicatorViewPager(Context context, AttributeSet attr,
	                          int defStyleAttr, int defStyleRes) {
		super(context, attr, defStyleAttr, defStyleRes);
	}

	public void setPages(int pages) {
		mPages = pages;
	}

	public void run() {

		for (int i=0; i<mPages; i++) {
			CircleButton button = new CircleButton(getContext(), i==0);
			mButtons.add(button);
			this.addView(button);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		for (CircleButton button : mButtons) {
			button.toggle(false);
		}

		mButtons.get(position).toggle(true);
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}

	private class CircleButton extends View {

		private final String LOG_TAG = CircleButton.class.getSimpleName();

		private GradientDrawable backgroundFill;
		private GradientDrawable backgroundTransparent;

		public CircleButton(Context context) {
			this(context, null);
		}

		public CircleButton(Context context, boolean selected) {
			this(context);
			init(selected);
		}

		public CircleButton(Context context, AttributeSet attr) {
			this(context, attr, 0);
		}

		public CircleButton(Context context, AttributeSet attr, int defStyleAttr) {
			super(context, attr, defStyleAttr);
		}

		@TargetApi(21)
		public CircleButton(Context context, AttributeSet attr,
		                    int defStyleAttr, int defStyleRes) {
			super(context, attr, defStyleAttr, defStyleRes);
		}

		public void init(boolean selected) {

			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					(int) Math.ceil(getContext().getResources()
							.getDimension(R.dimen.indicator_viewpager_circle_radius)),
					(int) Math.ceil(getContext().getResources()
							.getDimension(R.dimen.indicator_viewpager_circle_radius))
			);
			layoutParams.setMargins(5, 0, 5, 0);

			this.setLayoutParams(layoutParams);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				backgroundFill = (GradientDrawable) getResources()
						.getDrawable(R.drawable.indicator_vewpager_circle_fill, null);
				backgroundTransparent = (GradientDrawable) getResources()
						.getDrawable(R.drawable.indicator_viewpager_circle_transparent, null);
			} else {
				backgroundFill = (GradientDrawable) getResources()
						.getDrawable(R.drawable.indicator_vewpager_circle_fill);
				backgroundTransparent = (GradientDrawable) getResources()
						.getDrawable(R.drawable.indicator_viewpager_circle_transparent);
			}

			if (selected) {
				setBackground(backgroundFill);
			} else {
				setBackground(backgroundTransparent);
			}
		}


		public void toggle(boolean selected) {

			if (selected) {
				setBackground(backgroundFill);
			} else {
				setBackground(backgroundTransparent);
			}
		}
	}
}
