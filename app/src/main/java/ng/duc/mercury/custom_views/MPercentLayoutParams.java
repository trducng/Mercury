package ng.duc.mercury.custom_views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by ducnguyen on 7/2/16.
 * Layout params that has the ability to change PercentLayoutInfo inside the param object
 */
public class MPercentLayoutParams extends PercentRelativeLayout.LayoutParams implements
		PercentLayoutHelper.PercentLayoutParams {

	private PercentLayoutHelper.PercentLayoutInfo mPercentLayoutInfo;

	public MPercentLayoutParams(Context c, AttributeSet attrs) {
		super(c, attrs);
		mPercentLayoutInfo = PercentLayoutHelper.getPercentLayoutInfo(c, attrs);
	}

	public MPercentLayoutParams(int width, int height) {
		super(width, height);
		mPercentLayoutInfo = new PercentLayoutHelper.PercentLayoutInfo();
	}

	public MPercentLayoutParams(ViewGroup.LayoutParams source) {
		super(source);
	}

	public MPercentLayoutParams(ViewGroup.MarginLayoutParams source) {
		super(source);
	}

	public void setWidthPercentage(float percent) {
		mPercentLayoutInfo.widthPercent = percent;
	}

	public void setHeightPercentage(float percent) {
		mPercentLayoutInfo.heightPercent = percent;
	}

	public void setPercentLayoutInfo(PercentLayoutHelper.PercentLayoutInfo info) {
		mPercentLayoutInfo = info;
	}

	@Override
	public PercentLayoutHelper.PercentLayoutInfo getPercentLayoutInfo() {
		return mPercentLayoutInfo;
	}

	@Override
	protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
		PercentLayoutHelper.fetchWidthAndHeight(this, a, widthAttr, heightAttr);
	}

}
