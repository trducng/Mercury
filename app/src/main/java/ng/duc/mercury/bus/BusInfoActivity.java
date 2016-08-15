package ng.duc.mercury.bus;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ng.duc.mercury.AppConstants.BUSINESS_ACTIVITY;
import ng.duc.mercury.AppConstants.SERVER_RESPONSE;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;
import ng.duc.mercury.custom_views.BusNavDrawer;
import ng.duc.mercury.custom_views.IndicatorViewPager;
/**
 * Created by ducnguyen on 8/10/16.
 * This activity shows business general information, user suggestion, and events
 *
 * v: for view pager, bottom dots
 * TODO: provide up navigation
 * TODO: theme in a way that make the background info and nav bar transparent (like partial-full
 *          screen) http://blog.raffaeu.com/archive/2015/04/11/android-and-the-transparent-status-bar.aspx
 */
public class BusInfoActivity extends AppCompatActivity {

	private static final String LOG_TAG = BusInfoActivity.class.getSimpleName();

	private BusNavDrawer mDrawer;
	private String mBusId;

	private ViewPager mViewPager;
	private BusInfoPagerAdapter mAdapter;
	private IndicatorViewPager mIndicator;

	private DrawerBroadcastReceiver drawerReceiver = null;
	private EventBroadcastReceiver eventReceiver = null;

	private static final String INTENT_FILTER_EVENT = "BusInfoActivity.EVENTS";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bus_info);

		Intent intent = getIntent();
		mBusId = intent.getStringExtra(SERVER_RESPONSE.BUS_ID);
		String busNav = intent.getStringExtra(SERVER_RESPONSE.DRAWER);

		mIndicator = (IndicatorViewPager) findViewById(R.id.indicator_viewpager_bus_info);

		mViewPager = (ViewPager) findViewById(R.id.viewpager_bus_info_activity);

		// if num of events information is not included in intent, download it from server
		IntentFilter eventFilter = new IntentFilter(INTENT_FILTER_EVENT);
		eventReceiver = new EventBroadcastReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(eventReceiver, eventFilter);

		Intent eventIntent = new Intent(this, EventSyncService.class);
		eventIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
							 Utility.BuildURL.busNumEventSync(mBusId));
		startService(eventIntent);

		// Construct drawer
		mDrawer = (BusNavDrawer) findViewById(R.id.bus_nav_drawer);

		if (busNav != null) {
			if (mDrawer != null) {
				mDrawer.createBusinessNavigationView(
						busNav, SERVER_RESPONSE.DRAWER_BUSINESS_INFO);
			}
		} else {

			IntentFilter drawerFilter = new IntentFilter(BUSINESS_ACTIVITY.INTENT_FILTER_DRAWER);
			drawerReceiver = new DrawerBroadcastReceiver();
			LocalBroadcastManager.getInstance(this).registerReceiver(drawerReceiver, drawerFilter);

			Intent drawerIntent = new Intent(this, DrawerSyncService.class);
			drawerIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
								  Utility.BuildURL.busDrawerSync(mBusId));
			startService(drawerIntent);
		}


	}

	// SEPARATE OBJECTS =================================

	public class BusInfoPagerAdapter extends FragmentStatePagerAdapter {

		int NUM_ITEMS;
		ArrayList<String> events;

		public BusInfoPagerAdapter(FragmentManager fm, ArrayList<String> ev) {
			super(fm);
			events = ev;
			NUM_ITEMS = events.size() + 1;
		}

		@Override
		public Fragment getItem(int position) {

			if (position == 0) {

				Bundle arg = new Bundle();
				arg.putString(BUSINESS_ACTIVITY.BUNDLE_BUS_ID, mBusId);
				Fragment fragment = new BusInfoFragment();
				fragment.setArguments(arg);
				return fragment;

			} else {

				Bundle arg = new Bundle();
				arg.putString(BUSINESS_ACTIVITY.BUNDLE_BUS_ID, mBusId);

				if (position - 1 < events.size()) {
					arg.putString(BUSINESS_ACTIVITY.BUNDLE_EVENT_ID, events.get(position - 1));
				}

				Fragment fragment = new BusInfoEventFragment();
				fragment.setArguments(arg);
				return fragment;

			}

		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}
	}

	public static class DrawerSyncService extends IntentService {

		public DrawerSyncService() {
			super(DrawerSyncService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Uri url = intent.getParcelableExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE);
			// TODO: <debug> delete this fake data
			url = Uri.parse("https://www.dropbox.com/s/a7hiuiwruidq1b5/busNav.json?dl=1");

			JSONObject fromServer;
			String busNav = null;
			try {
				fromServer = new JSONObject(Utility.sendHTTPRequest(url));
				if (fromServer.has(SERVER_RESPONSE.CODE) &&
					(fromServer.getInt(SERVER_RESPONSE.CODE) == SERVER_RESPONSE.CODE_ERROR)) {
					return;
				}
				busNav = fromServer.getString(SERVER_RESPONSE.RESULT);

			} catch (JSONException e) {
				Log.e(LOG_TAG, "JSONException while downloading data " +
						"in IntentService: " + e.getMessage());
			}

			Intent drawerBroadcastIntent = new Intent(BUSINESS_ACTIVITY.INTENT_FILTER_DRAWER);
			drawerBroadcastIntent.putExtra(BUSINESS_ACTIVITY.INTENT_EXTRA, busNav);
			LocalBroadcastManager.getInstance(this).sendBroadcast(drawerBroadcastIntent);
		}
	}

	public class DrawerBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String busNavigation = intent.getStringExtra(BUSINESS_ACTIVITY.INTENT_EXTRA);
			if (busNavigation != null) {
				mDrawer.createBusinessNavigationView(
						busNavigation, SERVER_RESPONSE.DRAWER_BUSINESS_INFO);
			}
		}
	}

	public static class EventSyncService extends IntentService {


		public EventSyncService() {
			super(EventSyncService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Uri url = intent.getParcelableExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE);
			// TODO: <debug> delete this fake data
			url = Uri.parse("https://www.dropbox.com/s/b8niqc7ola5imtl/busNumEvent.json?dl=1");

			JSONObject fromServer;
			ArrayList<String> events = new ArrayList<>();
			try {
				fromServer = new JSONObject(Utility.sendHTTPRequest(url));
				if (!(fromServer.has(SERVER_RESPONSE.CODE) &&
						(fromServer.getInt(SERVER_RESPONSE.CODE) == SERVER_RESPONSE.CODE_ERROR))) {
					JSONArray ev = fromServer.getJSONArray(SERVER_RESPONSE.RESULT);
					if (ev != null) {
						for (int i=0; i<ev.length(); i++) events.add(ev.getString(i));
					}
				}

			} catch (JSONException e) {
				Log.e(LOG_TAG, "JSONException while downloading data " +
						"in IntentService: " + e.getMessage());
			}

			Intent eventBroadcastIntent = new Intent(INTENT_FILTER_EVENT);
			eventBroadcastIntent.putStringArrayListExtra(BUSINESS_ACTIVITY.INTENT_EXTRA, events);
			LocalBroadcastManager.getInstance(this).sendBroadcast(eventBroadcastIntent);
		}
	}

	public class EventBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			ArrayList<String> events = intent.getStringArrayListExtra(BUSINESS_ACTIVITY.INTENT_EXTRA);
			if (events == null) {
				events = new ArrayList<>();
				Log.e(LOG_TAG, "In event broadcast receiver, events list is null. Even if" +
								" there aren't any events, events list should be an empty array");
			}

			mAdapter = new BusInfoPagerAdapter(getSupportFragmentManager(), events);
			mViewPager.setAdapter(mAdapter);
			mIndicator.setPages(events.size() + 1);
			mIndicator.run();
			mViewPager.addOnPageChangeListener(mIndicator);
		}
	}

	@Override
	protected void onDestroy() {

		// unregister this receiver after it finishes, otherwise it will mess
		// with the state of this activity
		if (eventReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(eventReceiver);
		}
		if (drawerReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(drawerReceiver);
		}
		super.onDestroy();
	}
}
