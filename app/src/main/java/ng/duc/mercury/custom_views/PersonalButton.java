package ng.duc.mercury.custom_views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import ng.duc.mercury.R;
import ng.duc.mercury.Utility;

/**
 * Created by ducnguyen on 6/21/16.
 * This class is responsible for creating the button used
 * in personal fragment.
 */
public class PersonalButton extends TextView {

	private static final String LOG_TAG = PersonalButton.class.getSimpleName();
	private Context mContext;
	private String mValue;
	private PersonalButtonGroup mGroup;
	private boolean mCheck = true;


	GradientDrawable backgroundFill;
	GradientDrawable backgroundTransparent;

	public PersonalButton(Context context) {
		super(context);
	}

	public PersonalButton(Context context, PersonalButtonGroup group,
	                      String name, String color) {
		super(context);
		mContext = context;
		mValue = name;
		mGroup = group;
		setAttributes();
		init();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			backgroundFill = (GradientDrawable) getResources()
					.getDrawable(R.drawable.personal_button, null);
			backgroundTransparent = (GradientDrawable) getResources()
					.getDrawable(R.drawable.personal_button, null);
		} else {
			backgroundFill = (GradientDrawable) getResources()
					.getDrawable(R.drawable.personal_button);
			backgroundTransparent = (GradientDrawable) getResources()
					.getDrawable(R.drawable.personal_button);
		}

		backgroundTransparent.mutate();
		backgroundFill.mutate();

		backgroundFill.setColor(Color.parseColor(color));
		backgroundTransparent.setStroke(
				(int) Utility.dpsToPxRaw(1, getContext()),
				Color.parseColor(color)
		);
	}

	public String getValue() {
		return mValue;
	}

	private void init() {
		this.setText(mValue);
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isChecked()) {
					toggle(false);
					mGroup.handleButtonClick(mValue, false);
				} else {
					toggle(true);
					mGroup.handleButtonClick(mValue, true);
				}
			}
		});
	}

	public boolean isChecked() {
		return mCheck;
	}

	@TargetApi(Build.VERSION_CODES.M)
	private void setAttributes() {

		// We don't need to set layout here, as it will be set and
		// measured by the parent group

		setPadding(10, 10, 10, 10);
		setBackground(backgroundFill);
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
			setTextColor(getResources().getColor(R.color.white));
		} else {
			setTextColor(getResources().getColor(R.color.white, null));
		}
	}

	public void toggle(boolean check) {
		mCheck = check;
		if (check) {
			setBackground(backgroundFill);
		} else {
			setBackground(backgroundTransparent);
		}
	}
}
