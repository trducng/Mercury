package ng.duc.mercury.custom_views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import ng.duc.mercury.R;
import ng.duc.mercury.Utility;

/**
 * Created by ducnguyen on 7/5/16.
 * This view will create a color tag on top of each item in personal view.
 */
public class PersonalTag extends View {

	int numContent;
	int totalWidth;

	GradientDrawable background;

	// if defaultWidth * numContent <= totalWidth then the determined width
	// would be defaultWidth. Otherwise the determined width is
	// totalWidth/numContent
	int defaultWidth;

	public PersonalTag(Context mContext) {
		super(mContext);
	}

	public PersonalTag(Context mContext, String color, int num_content,
	                   int total_width) {

		super(mContext);
		numContent = num_content;
		totalWidth = total_width;
		defaultWidth = total_width / 3;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			background = (GradientDrawable) getResources()
					.getDrawable(R.drawable.personal_tag, null);
		} else {
			background = (GradientDrawable) getResources()
					.getDrawable(R.drawable.personal_tag);
		}

		background.mutate();
		background.setColor(Color.parseColor(color));


		setAttributes();
	}

	private int determineWidth() {

		if (defaultWidth * numContent <= totalWidth) {
			return defaultWidth;
		} else {
			return (int) Math.floor(totalWidth / numContent);
		}

	}

	private void setAttributes() {

		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
				(int) Utility.dpsToPxRaw(determineWidth(), getContext()),
				ViewGroup.LayoutParams.MATCH_PARENT
		);

		setBackground(background);
		this.setLayoutParams(params);
	}
}
