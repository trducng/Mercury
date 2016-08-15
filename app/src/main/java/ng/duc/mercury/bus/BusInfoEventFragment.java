package ng.duc.mercury.bus;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import ng.duc.mercury.AppConstants;
import ng.duc.mercury.AppConstants.BUSINESS_ACTIVITY;
import ng.duc.mercury.AppConstants.SERVER_RESPONSE;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;

/**
 * Created by ducnguyen on 8/11/16.
 * Fragment that shows a single event in business info activity
 * TODO: make the image header smooth. The problem now is
 */
public class BusInfoEventFragment extends Fragment
			implements NestedScrollView.OnScrollChangeListener {

	private static final String LOG_TAG = BusInfoEventFragment.class.getSimpleName();

	private static final String INTENT_FILTER_EVENT = "BusInfoEventFragment.EVENT";
	private String busId;
	private String eventId;

	private BusEventBroadcastReceiver eventBroadcastReceiver = null;

	private CoordinatorLayout mRootView;
	private PercentFrameLayout mPercentFrameLayout;
	private TextView mEventName;
	private TextView mEventCat;

	private int maxTranslation;
	private int maxRatioAlpha;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		// retrieve necessary information to download event data
		Bundle arg = getArguments();
		if (arg != null) {
			busId = arg.getString(BUSINESS_ACTIVITY.BUNDLE_BUS_ID);
			eventId = arg.getString(BUSINESS_ACTIVITY.BUNDLE_EVENT_ID);
		} else {
			throw new IllegalStateException(LOG_TAG + ": Bus and event id are null");
		}
		String userId = getActivity()
				.getSharedPreferences(AppConstants.PREFERENCES.GLOBAL, Context.MODE_PRIVATE)
				.getString(AppConstants.PREFERENCES.USER_ID, null);


		// initiate the fragment view
		mRootView = (CoordinatorLayout) inflater
				.inflate(R.layout.fragment_bus_info_event, container, false);

		// set scroll view for this
		((NestedScrollView) mRootView.findViewById(R.id.nested_scrollview_bus_info_event))
				.setOnScrollChangeListener(this);

		mPercentFrameLayout = (PercentFrameLayout)
				mRootView.findViewById(R.id.image_bus_info_event_header_holder);
		mEventCat = (TextView) mRootView.findViewById(R.id.text_bus_info_event_cat);
		mEventName = (TextView) mRootView.findViewById(R.id.text_bus_info_event_name);

		// download event data. Important, we have to have eventId in intent filter
		// so that the receivers of different events do not confuse with each other
		IntentFilter eventFilter = new IntentFilter(INTENT_FILTER_EVENT + eventId);
		eventBroadcastReceiver = new BusEventBroadcastReceiver();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(eventBroadcastReceiver,
																		  eventFilter);

		Intent eventIntent = new Intent(getActivity(), BusEventSyncService.class);
		eventIntent.putExtra(BUSINESS_ACTIVITY.INTENT_EXTRA, eventId);
//		eventIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
//							 Utility.BuildURL.busInfoEventId(busId, eventId, userId));

		switch (eventId) {
			case "id1":
				eventIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
						Uri.parse("https://www.dropbox.com/s/cvct3jd4ibotbdz/busInfoEvent1.json?dl=1"));
				break;
			default:
				eventIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
						Uri.parse("https://www.dropbox.com/s/a2zq5ril8g7ttie/busInfoEvent2.json?dl=1"));
				break;
		}

		getActivity().startService(eventIntent);

		maxTranslation = getMaxTranslation();
		maxRatioAlpha = maxTranslation * 3 / 4;

		return mRootView;
	}

	public void setView(JSONObject object) {

		Iterator<String> keys = object.keys();
		String key = null;

		try {

			while (keys.hasNext()) {

				key = keys.next();
				switch (key) {
					case SERVER_RESPONSE.EVENT_IMG:
						Picasso.with(getActivity())
								.load(object.getString(key))
								.fit()
								.centerCrop()
								.into(((ImageView) mRootView
										.findViewById(R.id.image_bus_info_event_header)));
						break;
					case SERVER_RESPONSE.EVENT_NAME:
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_name))
								.setText(object.getString(key));
						break;
					case SERVER_RESPONSE.EVENT_CAT:
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_cat))
								.setText(object.getString(key));
						break;
					case SERVER_RESPONSE.EVENT_TIME:
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_time))
								.setText(object.getString(key));
						break;
					case SERVER_RESPONSE.EVENT_GOING:
						if (object.getInt(key) == SERVER_RESPONSE.EVENT_GOING_GO) {
							if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
								((TextView) mRootView.findViewById(R.id.text_bus_info_event_going))
										.setTextColor(getResources().getColor(R.color.green, null));
							} else {
								((TextView) mRootView.findViewById(R.id.text_bus_info_event_going))
										.setTextColor(getResources().getColor(R.color.green));
							}
						}
						break;

					case SERVER_RESPONSE.EVENT_DESCRIPTION:
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_description_header))
								.setText(R.string.event_description);
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_description_content))
								.setText(object.getString(key));
						break;

					case SERVER_RESPONSE.EVENT_SCHEDULE:
						if (object.isNull(key)) {
							fakeDeleteView((TextView) mRootView
										.findViewById(R.id.text_bus_info_event_schedule_header));
							fakeDeleteView((TextView) mRootView
									.findViewById(R.id.text_bus_info_event_schedule_content));
							break;
						}

						((TextView) mRootView.findViewById(R.id.text_bus_info_event_schedule_header))
								.setText(R.string.event_schedule);
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_schedule_content))
								.setText(object.getString(key));
						break;

					case SERVER_RESPONSE.EVENT_TRANSPORTATION:
						if (object.isNull(key)) {
							fakeDeleteView((TextView) mRootView
									.findViewById(R.id.text_bus_info_event_transport_header));
							fakeDeleteView((TextView) mRootView
									.findViewById(R.id.text_bus_info_event_transport_header));
							break;
						}

						((TextView) mRootView.findViewById(R.id.text_bus_info_event_transport_header))
								.setText(R.string.event_trans);
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_transport_content))
								.setText(object.getString(key));
						break;
					case SERVER_RESPONSE.EVENT_OTHER_INFO:
						((TextView) mRootView.findViewById(R.id.text_bus_info_event_other_header))
								.setText(R.string.event_other);
						((TextView) mRootView.findViewById(R.id.tex_bus_info_event_other_content))
								.setText(object.getString(key));
						break;
					default:
						throw new IllegalArgumentException(LOG_TAG + ": Unknown event key: " + key);

				}

				keys.remove();
			}

		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot retrieve key: " + key);
		}

		mRootView.requestLayout();
	}

	public int getMaxTranslation() {

		final TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(
				new int[]{R.attr.actionBarSize});
		int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
		styledAttributes.recycle();

		DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
		return (int) (Math.ceil(displayMetrics.widthPixels) / 1.5 - toolbarHeight);
	}

	/**
	 * Make the text view 0 height and eliminate all margins. So that there isn't any extra space
	 * between the view above and the view below that text view.
	 * Note: we can just delete the text view, because this text view is in relative layout, and
	 * so the view below this text view needs this text view to position itself
	 * @param v     the text view we want to set zero height
	 */
	public void fakeDeleteView(TextView v) {
		v.setTextSize(0);
		PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams)
																v.getLayoutParams();
		params.setMargins(0, 0, 0, 0);
		v.setLayoutParams(params);
	}

	@Override
	public void onDestroy() {
		if (eventBroadcastReceiver != null) {
			LocalBroadcastManager
					.getInstance(getActivity())
					.unregisterReceiver(eventBroadcastReceiver);
		}
		super.onDestroy();
	}

	@Override
	public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
	                           int oldScrollX, int oldScrollY) {

		if (scrollY <= maxTranslation) {
			mPercentFrameLayout.setTranslationY(-scrollY);
		} else {
			mPercentFrameLayout.setTranslationY(-maxTranslation);
		}

		if (scrollY <= maxRatioAlpha) {
			float ratio = (float) ((double) scrollY) / maxRatioAlpha;
			mEventName.setAlpha(1 - ratio);
			mEventCat.setAlpha(1 - ratio);
		} else {
			mEventCat.setAlpha(0);
			mEventName.setAlpha(0);
		}
	}

	public static class BusEventSyncService extends IntentService {

		public BusEventSyncService() {
			super(BusEventSyncService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Uri url = intent.getParcelableExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE);
			String eventId = intent.getStringExtra(BUSINESS_ACTIVITY.INTENT_EXTRA);

			String rawString = Utility.sendHTTPRequest(url);

			Intent eventBroadcastIntent = new Intent(INTENT_FILTER_EVENT + eventId);
			eventBroadcastIntent.putExtra(BUSINESS_ACTIVITY.INTENT_EXTRA, rawString);
			LocalBroadcastManager.getInstance(this).sendBroadcast(eventBroadcastIntent);
		}
	}

	public class BusEventBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String rawString = intent.getStringExtra(BUSINESS_ACTIVITY.INTENT_EXTRA);
			if (rawString == null) {
				Log.e(LOG_TAG, ": Cannot load event");
			} else {

				JSONObject event;

				try {

					event = new JSONObject(rawString)
							.getJSONObject(SERVER_RESPONSE.RESULT);
					setView(event);


				} catch (JSONException e) {
					Log.e(LOG_TAG, "Cannot load event from json: " + e.getMessage());
				}
			}

		}
	}

}
