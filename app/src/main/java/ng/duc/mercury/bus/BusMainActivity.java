package ng.duc.mercury.bus;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ng.duc.mercury.AppConstants.BUSINESS_ACTIVITY;
import ng.duc.mercury.AppConstants.SERVER_RESPONSE;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;
import ng.duc.mercury.custom_views.BusNavDrawer;

/**
 * Created by ducnguyen on 8/10/16.
 * This activity shows business general information, user suggestion, and events
 *
 * TODO: for view pager, bottom dots
 * TODO: dynamic view pager, info can require 1, 2, 3 or more pages
 * TODO: dynamic navigation drawer. Instead of using NavigationView, use your custom layout
 *          (ListView should be OK. Define this class in a separate file so all
 *          business-related activities can have access to it)
 * TODO: provide up navigation
 * TODO: theme in a way that make the background info and nav bar transparent (like partial-full
 *          screen) http://blog.raffaeu.com/archive/2015/04/11/android-and-the-transparent-status-bar.aspx
 */
public class BusMainActivity extends AppCompatActivity {

	private static final String LOG_TAG = BusMainActivity.class.getSimpleName();

	private BusNavDrawer mDrawer;
	private String mBusId;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bus_info);

		Intent intent = getIntent();
		mBusId = intent.getStringExtra(SERVER_RESPONSE.BUS_ID);
		String busNav = intent.getStringExtra(SERVER_RESPONSE.DRAWER);

		Log.v(LOG_TAG, "busNav: " + busNav);
		Log.v(LOG_TAG, "busId: " + mBusId);

		mDrawer = (BusNavDrawer) findViewById(R.id.bus_nav_drawer);

		if (busNav != null) {
			if (mDrawer != null) {
				mDrawer.createBusinessNavigationView(
						busNav, SERVER_RESPONSE.DRAWER_BUSINESS_INFO);
			}
		} else {

			IntentFilter drawerFilter = new IntentFilter(BUSINESS_ACTIVITY.INTENT_FILTER_DRAWER);
			DrawerBroadcastReceiver drawerReceiver = new DrawerBroadcastReceiver();
			LocalBroadcastManager.getInstance(this).registerReceiver(drawerReceiver, drawerFilter);

			Intent drawerIntent = new Intent(this, DrawerSyncService.class);
			drawerIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
								  Utility.BuildURL.busDrawerSync(mBusId));
			startService(drawerIntent);
		}
	}

	public static class DrawerSyncService extends IntentService {

		public DrawerSyncService() {
			super(DrawerSyncService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			// TODO: retrieve the navigation here from the server
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
				busNav = fromServer.getString(SERVER_RESPONSE.ITEM);

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
}
