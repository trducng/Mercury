package ng.duc.mercury;

import android.Manifest;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import ng.duc.mercury.bus.BusInfoActivity;
import ng.duc.mercury.data.MercuryDatabaseOpener;
import ng.duc.mercury.main.AroundFragment;
import ng.duc.mercury.main.PersonalFragment;
import ng.duc.mercury.main.RecommendFragment;

/**
 * Created by ducnguyen on 6/13/16.
 * This is the first activity that will appear when the program
 * launches. It handles three basic tasks:
 *      - Set up navigation drawer for home page and main page
 *      - Set up vertical view pager to navigate between home
 *      page and main page
 *      - Connection to Google Play location services and pass
 *      the GoogleApiClient to home fragment
 *          + The reason we want to connect to Google Play
 *          services in this activity rather than the home
 *          fragment is because here we have better lifecycle
 *          management (use autoenable) and we want to connect
 *          to the services as soon as possible, to know the
 *          location of user, so that during the first animation
 *          in home fragment, we have some time to connect to
 *          the server to download information
 *          + To check: whether the home fragment's GoogleApiClient
 *          works even though it is not accessed in the activity
 *          class.
 * Modified July 26:
 *      - Change vertical view pager to view pager. Reason: should
 *      go straight to the recommendation page. That's going right
 *      to the business. And it lays the AI interface for later
 *      development. The current interface is hard to develop AI
 *      and also does not help improve user interaction (user has
 *      to swipe a lot)
 *      - Remove the home fragment and personal fragment from main
 *      activity. These fragments are redundant for user experience.
 *      Users will not need these fragments much. So, personal
 *      fragment can be accessed through the drawer navigation, while
 *      home fragment is completely removed and replaced by
 *      recommendation fragment.
 *
 *
 * TODO: refine search for both searching businesses/stuff and event/deal
 * TODO: refine location services for recommendation and around
 */


public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.OnConnectionFailedListener,
		GoogleApiClient.ConnectionCallbacks,
		LocationListener {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();

	public GoogleApiClient mGoogleApiClient;
	// @test
//	private OnLocationListener mLocationListener;
	private OnLocationListener mLocationListener = new OnLocationListener() {
		@Override
		public void onLocationUpdated(Location location) {

		}

		@Override
		public void onLocationFailed() {

		}
	};

	private LocationRequest mLocationRequest;
	private boolean mGoogleApiAvail;
	private LocationManager locationManager;
	private android.location.LocationListener androidLocationListener;

	// Id code for first time location request (when user first runs app)
	private final int LOCATION_PERMISSION_CODE = 0;
	// Id code for Google API connection failure. This code will then be used in
	// onActivityResult(..., int)
	private static final int GPS_CONNECTION_ERROR = 1;
	// Key that points to error code in getArguments().getInt(key, value)
	private static final String GPS_CONNECTION_ERROR_POINT = "error_code";

	// Track whether user has allowed location updates
	private boolean mRequestingLocationUpdates;
	// Track whether error resolution has been requested
	private boolean mResolvingError = false;

	// Keys used in save instance state
	private static final String SAVE_LOCATION_UDPATES = "request_updates";
	private static final String SAVE_STATE_RESOLVE_ERROR = "resolve_error";

	private DrawerLayout mDrawer;
	private SearchView searchView;
	private static FloatingActionButton mFAB;
	private static ViewPager mViewPager;

	private static RecommendFragment recommendFragment;
	private static AroundFragment aroundFragment;

	public interface OnLocationListener {
		void onLocationUpdated(Location location);
		void onLocationFailed();
	}

	private void loadupTest() {

		// This is purely to load up fake data for testing

		// Fake user ID
		SharedPreferences prefs = getSharedPreferences(
				AppConstants.PREFERENCES.GLOBAL, MODE_PRIVATE);
		prefs.edit()
				.clear()
				.putString(AppConstants.PREFERENCES.USER_ID, "A123")
//				.putString(AppConstants.PREFERENCES.USER_NAME, "Duc Nguyen")
//				.putString(AppConstants.PREFERENCES.USER_PIC,
//						   "https://www.dropbox.com/s/yxxdtkhgsxwgos2/ava.jpg?dl=1")
				.putInt(AppConstants.PREFERENCES.PERSONAL_SYNC, 1)
				.apply();
	}

	/**
	 * This method is called when the activity first created. This is when
	 * basic tasks should have been done (create views, bind data to list..)
	 * @param savedInstanceState    the state of previously frozen activity
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// TODO: <debug> delete this
		MercuryDatabaseOpener open = new MercuryDatabaseOpener(this);
		open.onCreate(open.getWritableDatabase());
		loadupTest();
		// =================

		// Check if device has google play services
		mGoogleApiAvail = (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) ==
				ConnectionResult.SUCCESS);

		setContentView(R.layout.activity_main);

		mFAB = (FloatingActionButton) findViewById(R.id.fab_main_activity);
		mDrawer = (DrawerLayout) findViewById(R.id.drawerlayout_main_activity);
//		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
//		setSupportActionBar(toolbar);
//		getSupportActionBar().setDisplayShowTitleEnabled(false);
//		if (toolbar != null) {
//			toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					if (mDrawer != null) {
//						mDrawer.openDrawer(GravityCompat.START);
//						Log.v(LOG_TAG, "Navigation icon is clicked!");
//					} else {
//						Log.v(LOG_TAG, "Nothing is clicked!");
//					}
//				}
//			});
//		}


		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) findViewById(R.id.menu_search_bar);
		if (searchView != null) {
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		}

		mViewPager = (ViewPager)
				findViewById(R.id.viewpager_main_activity);

		if (mViewPager != null) {
			mViewPager.addOnPageChangeListener(new MainActivityPagerListener());
			MainActivityPagerAdapter mPagerAdapter = new
					MainActivityPagerAdapter(getSupportFragmentManager());
			mViewPager.setAdapter(mPagerAdapter);
			searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					Log.v(LOG_TAG, "SearchView focus: " + hasFocus);
				}
			});
			mFAB.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mViewPager.setCurrentItem((mViewPager.getCurrentItem() + 1) % 2);
					searchView.clearFocus();
				}
			});
		}

		// Check if (first run time) && (location is denied) => prompt location activation dialog
		SharedPreferences prefs = getSharedPreferences("ng.duc.mercury", MODE_PRIVATE);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			mRequestingLocationUpdates = false;
			if (prefs.getBoolean("firstrun", true)) {
				// Open location setting dialog box
				ActivityCompat.requestPermissions(this,
						new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
						LOCATION_PERMISSION_CODE);
			}
		} else {
			mRequestingLocationUpdates = true;
		}

		// Load state
		if (savedInstanceState != null) {
			mResolvingError = savedInstanceState
					.getBoolean(SAVE_STATE_RESOLVE_ERROR, false);
			mRequestingLocationUpdates = savedInstanceState
					.getBoolean(SAVE_LOCATION_UDPATES, false);
		}

		if (mGoogleApiAvail) {

			Log.v("LOCATION", "mGoogleApiAvail: true");

			// Set up Google Play services connection
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
			createLocationRequest();

		} else {

			Log.v("LOCATION", "mGoogleApiAvail: false");
			kickOffAndroidLocationService();

		}

		LinearLayout toolbar = (LinearLayout) findViewById(R.id.toolbar_main);
		if (toolbar != null ) {
			toolbar.requestFocus();
		}

	}

	private void kickOffAndroidLocationService() {

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) {

			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			androidLocationListener = new android.location.LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					mLocationListener.onLocationUpdated(location);
				}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {

				}

				@Override
				public void onProviderEnabled(String provider) {

				}

				@Override
				public void onProviderDisabled(String provider) {

				}

			};
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					60000, 5, androidLocationListener);

		}
	}

	/**
	 * This method is called when the activity is becoming visible to user.
	 */
	@Override
	protected void onStart() {

		if (mGoogleApiAvail) {
			mGoogleApiClient.connect();
		}
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			stopLocationUpdates();
		}
	}

	/**
	 * This method is called when activity is no longer visible to user (because
	 * new activity has been created, existing one has been brought to front, or
	 * this activity is destroyed)
	 */
	@Override
	protected void onStop() {

		searchView.setQuery("", false);
		searchView.clearFocus();

		if (mGoogleApiAvail) {
			mGoogleApiClient.disconnect();
		}
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SAVE_STATE_RESOLVE_ERROR, mResolvingError);
		outState.putBoolean(SAVE_LOCATION_UDPATES, mRequestingLocationUpdates);


	}

	public RecommendFragment getRecommendFragment() {
		return recommendFragment;
	}

	public AroundFragment getAroundFragment() {
		return aroundFragment;
	}

	public void setOnLocationListener(OnLocationListener listener) {
		mLocationListener = listener;
	}

	public void onDismissed() {
		mResolvingError = false;
	}

	/**
	 * This method create a location settings request to the device. This allows more
	 * finer control over the result of the setting
	 */
	private void createLocationRequest() {

		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(60000);
		mLocationRequest.setFastestInterval(1000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	}

	private void startLocationUpdates() {

		try {
			if (mGoogleApiAvail && mGoogleApiClient.isConnected()) {

				Log.v("LOCATION", "startLocationUpdates - mGoogleApiAvail: " + mGoogleApiAvail);
				Log.v("LOCATION", "startLocationUpdates - mGoogleApiClient.isConnected(): " + mGoogleApiClient.isConnected());
				LocationServices.FusedLocationApi.requestLocationUpdates(
						mGoogleApiClient, mLocationRequest, this);
			} else {
				Log.v("LOCATION", "startLocationUpdates - mGoogleApiAvail: " + mGoogleApiAvail);
				Log.v("LOCATION", "startLocationUpdates - mGoogleApiClient.isConnected(): " + mGoogleApiClient.isConnected());
				kickOffAndroidLocationService();
			}

		} catch (SecurityException e) {
			Log.e(LOG_TAG, "Cannot requestLocationUpdates due: " + e.getMessage());
		}
	}

	private void stopLocationUpdates() {

		if (mGoogleApiAvail && mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(
					mGoogleApiClient, this);
			Log.v("LOCATION", "stopLocationUpdates - mGoogleApiAvail: " + mGoogleApiAvail);
			Log.v("Location", "stopLocationUpdates - mGoogleApiClient.isConnected(): " + mGoogleApiClient.isConnected());
		} else {

			Log.v("LOCATION", "stopLocationUpdates - mGoogleApiAvail: " + mGoogleApiAvail);
			Log.v("Location", "stopLocationUpdates - mGoogleApiClient.isConnected(): " + mGoogleApiClient.isConnected());
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) {
				locationManager.removeUpdates(androidLocationListener);
			}
		}

	}


	// THESE METHODS ARE CALLBACK =================================

	// -------------------------------------- When location changes
	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			mLocationListener.onLocationUpdated(location);
		} else {
			mLocationListener.onLocationFailed();
		}
	}

	public void onToolbarIconsClicked(View view) {

		if (view.getId() == R.id.menu_navigations) {
			mDrawer.openDrawer(GravityCompat.START);
		} else if (view.getId() == R.id.menu_settings) {
			Toast.makeText(this, "Settings is clicked", Toast.LENGTH_SHORT).show();
			// TODO: <debug> quick way to access new activity
			Intent intent = new Intent(this, BusInfoActivity.class);
			intent.putExtra(AppConstants.SERVER_RESPONSE.BUS_ID, "Hihi123");
//			intent.putExtra(AppConstants.SERVER_RESPONSE.DRAWER, "01");
//			intent.putExtra(AppConstants.SERVER_RESPONSE.BUS_INFO_NUM_EVENTS, 3);
			startActivity(intent);
		}
	}


	// ----------------------- When connect to Google Play Services
	/**
	 * This method is called when GoogleApiClient is connected
	 * @param bundle    the bundle associates with connection
	 */
	@Override
	public void onConnected(@Nullable Bundle bundle) {

		// Check whether user allows manually allowed location services
		// and begin request location updates

		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

		if (mResolvingError) {
			return;
		}

		if (connectionResult.hasResolution()) {
			// has resolution
			// call connectionResult.startResolutionForResult();
			try {
				mResolvingError = true;
				connectionResult.startResolutionForResult(this, GPS_CONNECTION_ERROR);
			} catch (IntentSender.SendIntentException e) {
				mGoogleApiClient.connect();
			}
		} else {
			// does not have resolution
			// call GoogleApiAvailability.getErrorDialog()
			ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putInt(GPS_CONNECTION_ERROR_POINT, connectionResult.getErrorCode());
			errorDialogFragment.setArguments(bundle);
			errorDialogFragment.show(getSupportFragmentManager(), "errorDialog");
			mResolvingError = true;
		}
	}


	// ------------------- Actions to perform when request permission
	/**
	 * This method will be called when the user chooses an option in a permission request dialog.
	 * This method will be called when the permission dialog is requested by
	 * ActivityCompat.requestPermissions
	 * @param requestCode   the id of the request being asked
	 * @param permissions   the permissions requested
	 * @param grantResults  the result for each of the permissions
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions, @NonNull int[] grantResults) {

		switch (requestCode) {

			case LOCATION_PERMISSION_CODE: {

				if ((grantResults.length > 0) &&
						(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
					// Permission granted
					mRequestingLocationUpdates = true;
					Log.v("LOCATION", "onRequestPermissionResult allowed");
					startLocationUpdates();

				} else {
					Log.v("LOCATION", "onRequestPermissionResuilt denied");
					// Permisison denied
					if (ActivityCompat.shouldShowRequestPermissionRationale(this,
							Manifest.permission.ACCESS_FINE_LOCATION)) {
						Toast.makeText(this, "Mercury needs your location to better " +
										"recommend you businesses and calculate distance",
								Toast.LENGTH_LONG).show();
						ActivityCompat.requestPermissions(this,
								new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
								LOCATION_PERMISSION_CODE);
					} else {
						mLocationListener.onLocationFailed();
					}
				}
			}
		}
	}

	/**
	 * This method will be called by the system after ConnectionResult.startResolutionForResult()
	 * or getErrorDialog() is called.
	 * @param requestCode   the specific request id
	 * @param resultCode    the specific result for that request
	 * @param data          the associated data
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
			case GPS_CONNECTION_ERROR: {
				mResolvingError = false;
				if (resultCode == RESULT_OK) {
					if (!mGoogleApiClient.isConnecting() &&
							!mGoogleApiClient.isConnected()) {
						mGoogleApiClient.connect();
					}
				} else {

					// In case we we cannot connect to Google Play location services, revert
					// back to Android location services
					Log.v("LOCATION", "GPS_CONNECTION_ERROR error");
					startLocationUpdates();
				}
				break;
			}

		}
	}


	// ------------------- Actions to perform when user wants to sign up or in
	// TODO: create separate sign-in/up pages and link to these buttons
	public void signInNudge(View v) {
		Toast.makeText(this, "User hits sign in!", Toast.LENGTH_SHORT).show();
	}

	public void signUpNudge(View v) {
		Toast.makeText(this, "User hits sign up!", Toast.LENGTH_SHORT).show();
	}

	public void refreshPersonal(View v) {

		Toast.makeText(this, "User hits refresh!", Toast.LENGTH_SHORT).show();

		SharedPreferences prefs = getSharedPreferences(AppConstants.PREFERENCES.GLOBAL, MODE_PRIVATE);
		Intent intent = new Intent(this, PersonalFragment.PersonalSyncService.class);
		intent.putExtra(
				AppConstants.MAIN_ACTIVITY.URL_UPDATE,
				Utility.BuildURL.tagSync(
						prefs.getString(AppConstants.PREFERENCES.USER_ID, null),
						prefs.getInt(AppConstants.PREFERENCES.PERSONAL_SYNC, -1)));
		startService(intent);
	}


	// THESE CLASSES ARE HELPER ======================================

	public static class MainActivityPagerAdapter extends FragmentPagerAdapter {

		static final int NUM_ITEMS = 2;
		static final int RECOMMENDATION = 0;
		static final int AROUND = 1;

		public MainActivityPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}

		@Override
		public Fragment getItem(int position) {

			switch(position) {

				case RECOMMENDATION:
					recommendFragment = new RecommendFragment();
					return recommendFragment;

				case AROUND:
					aroundFragment = new AroundFragment();
					return aroundFragment;

				default:
					throw new UnsupportedOperationException("There is no " +
							"position: " + String.valueOf(position));
			}
		}
	}

	public static class ErrorDialogFragment extends DialogFragment {

		public ErrorDialogFragment() {}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			int errorCode = this.getArguments().getInt(GPS_CONNECTION_ERROR_POINT);
			return GoogleApiAvailability.getInstance().getErrorDialog(
					this.getActivity(), errorCode, GPS_CONNECTION_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((MainActivity) this.getActivity()).onDismissed();
		}
	}

	public static class MainActivityPagerListener extends ViewPager.SimpleOnPageChangeListener {

		private final int RECOMMEND_STATE = 0;
		private final int AROUND_STATE = 1;

		public MainActivityPagerListener() {}

		@Override
		public void onPageSelected(int position) {
			super.onPageSelected(position);

			switch (position) {
				case RECOMMEND_STATE:
					mFAB.setImageResource(R.drawable.right_arrow);
					break;
				case AROUND_STATE:
					mFAB.setImageResource(R.drawable.left_arrow);
					break;
				default:
					Log.e(LOG_TAG, "View pager listener cannot recognize state: " + position);
			}
		}
	}

	public static class ScrollingFABBehavior extends
			CoordinatorLayout.Behavior<FloatingActionButton> {

		private int toolbarHeight;

		public ScrollingFABBehavior() {super();}

		public ScrollingFABBehavior(Context context, AttributeSet attrs) {
			super(context, attrs);
			toolbarHeight = getToolbarHeight(context);
		}

		@Override
		public boolean layoutDependsOn(CoordinatorLayout parent,
		                               FloatingActionButton child,
		                               View dependency) {
			return dependency instanceof AppBarLayout;
		}

		@Override
		public boolean onDependentViewChanged(CoordinatorLayout parent,
		                                      FloatingActionButton child,
		                                      View dependency) {

			if (dependency instanceof AppBarLayout) {
				CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)
						child.getLayoutParams();
				int fabBottomMargin = lp.bottomMargin;
				int distanceToScroll = child.getHeight() + fabBottomMargin;
				float ratio = dependency.getY()/(float)toolbarHeight;
				child.setTranslationY(-distanceToScroll * ratio);
			}

			return true;

		}

		public static int getToolbarHeight(Context context) {
			final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
					new int[]{R.attr.actionBarSize});
			int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
			styledAttributes.recycle();

			return toolbarHeight;
		}
	}
}