package ng.duc.mercury;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import ng.duc.mercury.AppConstants.PREFERENCES;
import ng.duc.mercury.custom_views.VerticalViewPager;
import ng.duc.mercury.data.MercuryDatabaseOpener;
import ng.duc.mercury.main.HomeFragment;
import ng.duc.mercury.main.MainFragment;
import ng.duc.mercury.main.PersonalFragment;

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
 */

public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.OnConnectionFailedListener,
		GoogleApiClient.ConnectionCallbacks,
		LocationListener {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();

	private DrawerLayout mDrawer;
	public GoogleApiClient mGoogleApiClient;
	private OnLocationListener mLocationListener;
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

	private static HomeFragment homeFragment;
	private static MainFragment mainFragment;


	public interface OnLocationListener {
		void onLocationUpdated(Location location);
		void onLocationFailed();
	}

	private void loadupTest() {

		// This is purely to load up fake data for testing

		// Fake user ID
		SharedPreferences prefs = getSharedPreferences(
				PREFERENCES.GLOBAL, MODE_PRIVATE);
		prefs.edit()
				.putString(PREFERENCES.USER_ID, "A123")
				.putInt(PREFERENCES.PERSONAL_SYNC, 1)
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
		MainActivityPagerAdapter mPagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager());

		mDrawer = (DrawerLayout) findViewById(R.id.drawerlayout_main_activity);
		VerticalViewPager mViewPager = (VerticalViewPager)
				findViewById(R.id.vertical_viewpager_main_activity);

		mViewPager.setAdapter(mPagerAdapter);

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

	public HomeFragment getHomeFragment() {
		return homeFragment;
	}

	public MainFragment getMainFragment() {
		return mainFragment;
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

		SharedPreferences prefs = getSharedPreferences(PREFERENCES.GLOBAL, MODE_PRIVATE);
		Intent intent = new Intent(this, PersonalFragment.PersonalSyncService.class);
		intent.putExtra(
				AppConstants.MAIN_ACTIVITY.URL_PERSONAL,
				Utility.BuildURL.tagSync(
						prefs.getString(PREFERENCES.USER_ID, null),
						prefs.getInt(PREFERENCES.PERSONAL_SYNC, -1)));
		startService(intent);
	}


	// THESE CLASSES ARE HELPER ======================================

	public static class MainActivityPagerAdapter extends FragmentPagerAdapter {

		static final int NUM_ITEMS = 2;
		static final int HOME_PAGE = 0;
		static final int MAIN_PAGE = 1;

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

				case HOME_PAGE:
					homeFragment = new HomeFragment();
					return homeFragment;

				case MAIN_PAGE:
					mainFragment = new MainFragment();
					return mainFragment;

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
}
