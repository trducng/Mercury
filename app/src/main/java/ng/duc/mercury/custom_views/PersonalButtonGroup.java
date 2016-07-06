package ng.duc.mercury.custom_views;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import ng.duc.mercury.AppConstants;
import ng.duc.mercury.data.DataContract;

/**
 * Created by ducnguyen on 6/21/16.
 * This view is responsible for holding, representing and
 * managing buttons and their behaviors. How this class works:
 * - Basically it is a relative layout to allow flexible positioning of
 * child views (buttons) - buttons are automatically on the left of each
 * other, until the line cannot hold any button, then the unfitted button
 * will automatically break to the next line
 * - It has widthLeft to check whether a button can fit the current line,
 * if it can, then place that button to the left of the previous button
 * and decrease the value of widthLeft by (the previous button width +
 * that button margin). If the button cannot fit (its width and margin is
 * larger than widthLeft), then that button will be put into the next line,
 * and widthLeft will be reset to the initial value minus the (width +
 * margin) of the first button. This process continues for each button
 * until there isn't any button left to place.
 * - Any time a button is chosen, this group will gather all the currently
 * active button, create a query Uri and call callback listener on this
 * Uri. As a result, when this button group is created, it is necessary
 * to implement the specified interface on the holder, and add that
 * interface to this group.
 */
public class PersonalButtonGroup extends RelativeLayout {

	private final String LOG_TAG = PersonalButtonGroup.class.getSimpleName();
	private ArrayList<PersonalButton> mButtons = new ArrayList<>();
	private HashSet<String> mActiveButtons = new HashSet<>();
	private PersonalButton allButton;
	private Context mContext;
	private Uri uriSQL;
	private OnUriChangedListener mListener;
	private int widthLeft;
	private int margin = 10;
	private SharedPreferences sp = getContext()
			.getSharedPreferences(AppConstants.PREFERENCES.GLOBAL,
								  Context.MODE_PRIVATE);

	public interface OnUriChangedListener {
		void buttonClicked(Uri uri);
	}

	public PersonalButtonGroup(Context context) {
		super(context);
		init(context);
	}

	public PersonalButtonGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public PersonalButtonGroup(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public PersonalButtonGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		this(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		calculateWidthLeftBegin();
		allButton = new PersonalButton(mContext, this, "All", "#444444");
		// TODO: change the color above later
	}

	private void calculateWidthLeftBegin() {
		DisplayMetrics metrics = new DisplayMetrics();
		((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(metrics);

		widthLeft = metrics.widthPixels - margin;
	}

	public void createAllButtons(ArrayList<String> views) {

		Collections.sort(views, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return lhs.compareToIgnoreCase(rhs);
			}
		});

		int id_old_below = 0;
		int id_below = 0;
		int id_left = 0;

		for (int i=0; i<views.size(); i++) {

			if (i == 0) {

				// Log.v(LOG_TAG, "Supposed to be 0: " + i);

				id_left = id_below = View.generateViewId();
				PersonalButton button = new PersonalButton(mContext,
						this, views.get(i),
						sp.getString(AppConstants.SERVER_RESPONSE.TAG_COLOR + views.get(i), null));
				button.measure(-2, -2);
				button.setId(id_below);

				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				params.addRule(ALIGN_PARENT_TOP);
				params.addRule(ALIGN_PARENT_RIGHT);
				params.setMargins(0, margin, margin, 0);

				button.setLayoutParams(params);

				mButtons.add(button);
				mActiveButtons.add(views.get(i));
				this.addView(button);

				// Log.v(LOG_TAG, "Begin: " + button.getMeasuredWidth() + " " + widthLeft);
//				Log.v("POSITION", "Begin button: " + views.get(i));
				widthLeft = widthLeft - button.getMeasuredWidth() - margin;

			} else {

				// Log.v(LOG_TAG, "Supposed to not be 0: " + i);

				PersonalButton button = new PersonalButton(mContext,
						this, views.get(i),
						sp.getString(AppConstants.SERVER_RESPONSE.TAG_COLOR + views.get(i), null));
				button.measure(-2, -2);

				if ((button.getMeasuredWidth()+margin) <= widthLeft) {

					// Log.v(LOG_TAG, "Marked as true: " + button.getMeasuredWidth()
					//		+ " " + widthLeft);

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					params.addRule(LEFT_OF, id_left);
					if (id_old_below != 0) {
						params.addRule(BELOW, id_old_below);
					}
					params.setMargins(0, margin, margin, 0);

					id_left = View.generateViewId();
					button.setId(id_left);
					button.setLayoutParams(params);

					mButtons.add(button);
					mActiveButtons.add(views.get(i));
					this.addView(button);

//					Log.v("POSITION", views.get(i) + " is at the left of " + views.get(i-1));

					widthLeft = widthLeft - button.getMeasuredWidth() - margin;

				} else {

					// Log.v(LOG_TAG, "Marked as false: " + button.getMeasuredWidth()
					//		+ " " + widthLeft);

					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					params.addRule(ALIGN_PARENT_RIGHT);
					params.addRule(BELOW, id_below);
					params.setMargins(0, margin, margin, 0);

					id_old_below = id_below;
					id_left = id_below = View.generateViewId();
					button.setId(id_below);
					button.setLayoutParams(params);

					mButtons.add(button);
					mActiveButtons.add(views.get(i));
					this.addView(button);

//					Log.v("POSITION", "Begin new line: " + views.get(i));

					calculateWidthLeftBegin();
					widthLeft = widthLeft - button.getMeasuredWidth() - margin;
				}
			}
		}

		RelativeLayout.LayoutParams allButtonParams = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		allButtonParams.addRule(ALIGN_PARENT_RIGHT);
		allButtonParams.addRule(BELOW, id_below);
		allButtonParams.setMargins(0, margin, margin, 0);
		allButton.setLayoutParams(allButtonParams);

		this.addView(allButton);
	}

	public void handleButtonClick(String name, boolean toggle) {

		// Will do something here
		if (toggle) {

			if (name.equals("All")) {

				for (PersonalButton button: mButtons) {
					button.toggle(true);
					mActiveButtons.add(button.getValue());
				}

			} else {
				mActiveButtons.add(name);
				if (mActiveButtons.size() == mButtons.size()) {
					allButton.toggle(true);
				}
			}

		} else {

			if (name.equals("All")) {

				for (PersonalButton button: mButtons) {
					button.toggle(false);
					mActiveButtons = new HashSet<>();
				}

			} else {
				mActiveButtons.remove(name);
				allButton.toggle(false);
			}

		}

		// Create uri from mActiveButtons and send request to
		// query database
		uriSQL = DataContract.tagEntry.buildSpecificTags(mActiveButtons);
		mListener.buttonClicked(uriSQL);

	}

	public void setOnButtonClicked(OnUriChangedListener listener) {
		mListener = listener;
	}

	public void loadButtons(HashSet<String> active,
	                        HashSet<String> allButtons) {

		mButtons = new ArrayList<>();

		createAllButtons(new ArrayList<>(allButtons));

		mActiveButtons = active;

		for (PersonalButton eachButton : mButtons) {
			if (!active.contains(eachButton.getValue())) {
				eachButton.toggle(false);
			}
		}

		if (mActiveButtons.size() == mButtons.size()) {
			allButton.toggle(true);
		} else {
			allButton.toggle(false);
		}

		uriSQL = DataContract.tagEntry.buildSpecificTags(mActiveButtons);
		mListener.buttonClicked(uriSQL);
	}

	public void initButtons(ArrayList<String> views) {

		createAllButtons(views);
		uriSQL = DataContract.tagEntry.buildGeneralTag();
		mListener.buttonClicked(uriSQL);
	}

	public HashSet<String> getActiveButtons() {
		return mActiveButtons;
	}

	public HashSet<String> getAllButtons() {

		HashSet<String> array = new HashSet<>();

		for (PersonalButton button: mButtons) {
			array.add(button.getValue());
		}

		return array;
	}
}
