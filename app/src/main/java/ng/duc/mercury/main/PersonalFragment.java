package ng.duc.mercury.main;

import android.Manifest;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

import ng.duc.mercury.AppConstants.MAIN_ACTIVITY;
import ng.duc.mercury.AppConstants.PREFERENCES;
import ng.duc.mercury.AppConstants.SERVER_RESPONSE;
import ng.duc.mercury.MainActivity;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;
import ng.duc.mercury.custom_views.MPercentLayoutParams;
import ng.duc.mercury.custom_views.PersonalButtonGroup;
import ng.duc.mercury.custom_views.PersonalRecyclerView;
import ng.duc.mercury.custom_views.PersonalTag;
import ng.duc.mercury.data.DataContract.tagEntry;

/**
 * Created by ducnguyen on 6/14/16.
 * This fragment is responsible for showing businesses that user personally tags.
 *
 * This fragment has 3 possible states, corresponding to 3 different fragment layouts:
 *      - user has not signed in or signed up -> redirect user to sign up
 *          + simple relative layout with text notice and sign in and up buttons
 *      - user has signed in but has not tagged any business -> suggest user to tag business
 *          + simple relative layout with text notice and refresh buttons
 *      - user has signed in and tagged business[s] -> list the businesses and the buttons to
 *          show businesses that have specific tag[s] (each businesses will be grouped by tags)
 *          + a recycle view
 *
 * This recycler view has 3 kind of items (the last 2 are basically the same):
 *      - button group
 *      - each business item
 *      - last business item (since we want extra bottom margin)
 *
 *
 * Button group:
 *
 *      - is created programmatically in onCreateView. At this time, the button group is just
 *      an empty group, and is not attached to the view.
 *          + in onCreateView, if savedInstanceState != null (user swiped to another tab then
 *      come back to this tab), then the button group will will be populated with buttons and
 *      their current state. However, the button group is still not attached to the root view
 *      (it will be attached later when list of businesses is loaded from sqlite)
 *
 *      - if savedInstanceState == null, then this fragment will send a request to the server
 *      to check whether the data is most up-to-date (using an intent service).
 *          + this process is facilitated by personalySync in SharedPreferences. personalSync
 *          is a unique int that this fragment will send to the server. If the server has the
 *          same int, then the data is up-to-date. If the server has a different int, then
 *          it will send back full list of tag businesses, along with a new personalSync code
 *          that this fragment will save back to SharedPreferences (so that the next time it
 *          will use this new code to check with the server).
 *          + the total processed is handle in worker thread.
 *
 *      - if the data is most up-to-date or when fragment finishes fetching new data, this
 *      intent service will send a broadcast, which is precoupled with a broadcast listener
 *      (PersonalBroadcastReceiver). This receiver, upon listened to the intent service,
 *      knows that all the data is updated in SharedPreferences and SQLite, will begin to
 *      fire up the CursorLoader.
 *
 *      - the CursorLoader will then retrieve the database. When it gets the cursor from the
 *      database, it will instantiate an adapter using that cursor and the tag button group.
 *      The button group will be attached to the recycler view (also the root view) from the
 *      adapter
 *
 *      - the button group has the ability to notice when user changes tag. Whenever user
 *      changes the tag, the button group will update the query uri of this fragment, and
 *      calls to restart the cursor loader
 *
 * Each business item:
 *      - each business item is attached to the recycler view from the adapter.
 */
public class PersonalFragment extends Fragment implements
		PersonalButtonGroup.OnUriChangedListener,
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String LOG_TAG = PersonalFragment.class.getSimpleName();

	public PersonalButtonGroup mButtonGroup;
	public Uri mUri = tagEntry.buildGeneralTag();
	public MainPersonalAdapter mPersonalAdapter;
	public PersonalRecyclerView rootView;

	private static final int LOADER_PERSONAL = 0;


	private final String ACTIVE_BUTTONS = "active";
	private final String ALL_BUTTONS = "all";
	private final String QUERY_URI = "uri";

	private SharedPreferences prefs;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
                @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		prefs = getActivity()
				.getSharedPreferences(PREFERENCES.GLOBAL, Context.MODE_PRIVATE);

		if (prefs.getString(PREFERENCES.USER_ID, null) == null) {
			// if USER_ID is null then probably the user has not logged in or
			// signed up. Direct the user to sign up / sign in
			return inflater.inflate(R.layout.fragment_personal_not_signed_in, container, false);
		}

		HashSet<String> stringSet = (HashSet<String>)
				prefs.getStringSet(PREFERENCES.PERSONAL_BUTTON_GROUP, null);
		if (stringSet == null) {
			// if stringSet is null then probably the user has logged in
			// but hasn't tagged any business
			return inflater.inflate(R.layout.fragment_personal_not_tag, container, false);
		}

		rootView = (PersonalRecyclerView) inflater.inflate(R.layout.fragment_personal,
												container, false);
		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(
				getActivity(), LinearLayoutManager.VERTICAL, false);
		rootView.setLayoutManager(mLayoutManager);
		rootView.setItemViewCacheSize(5);


		// We create the button and initialize it but we hasn't put it inside
		// root view, that's why this group will not show up. We will indirectly
		// put this button group into root view later when we put this group inside
		// the recycler view adapter
		mButtonGroup = (PersonalButtonGroup) inflater.inflate(R.layout.main_personal_button,
				rootView, false);


		if (savedInstanceState != null) {
			// Save this fragment state when user swipes to another fragment (make this fragment
			// destroyed) and then swipes back to this fragment.
			// In this case, we do not need to call Intent services to update the
			// data with the server

			mButtonGroup.setOnButtonClicked(this);
			mButtonGroup.loadButtons(
					(HashSet<String>) savedInstanceState.getSerializable(ACTIVE_BUTTONS),
					(HashSet<String>) savedInstanceState.getSerializable(ALL_BUTTONS)
			);

			mUri = Uri.parse(savedInstanceState.getString(QUERY_URI));

			getLoaderManager().initLoader(LOADER_PERSONAL, null, PersonalFragment.this);

			return rootView;
		}



		// Register intent filter and receiver in order to fire cursor loader whenever the
		// system make sure that the database is up-to-date (this is to avoid null pointer
		// problem - database and shared preferences are still being processed in the
		// worker thread, but it is still accessed in the main thread)
		IntentFilter intentFilter = new IntentFilter(
				MAIN_ACTIVITY.INTENT_PERSONAL_QUERY
		);

		PersonalBroadcastReceiver receiver = new PersonalBroadcastReceiver();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);


		// TODO: load up the PREFERENCES before start... what about user who does not sign up?
		// For user who does not sign up, let them in but don't allow them to access
		// function that is reserved only for registered members:
		// bookmark, order, reserve, message, loyalty
		Intent intent = new Intent(getActivity(), PersonalSyncService.class);
		intent.putExtra(
				MAIN_ACTIVITY.URL_PERSONAL,
				Utility.BuildURL.tagSync(
						prefs.getString(PREFERENCES.USER_ID, null),
						prefs.getInt(PREFERENCES.PERSONAL_SYNC, -1)));
		getActivity().startService(intent);


		return rootView;
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// save the state of buttons and the current query uri
		outState.putSerializable(ACTIVE_BUTTONS,
				mButtonGroup.getActiveButtons());
		outState.putSerializable(ALL_BUTTONS,
				mButtonGroup.getAllButtons());
		outState.putString(QUERY_URI, mUri.toString());
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		return new CursorLoader(getActivity(),
				mUri,
				tagEntry.PROJECTION,
				null,
				null,
				null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		// Create adapter, set that adapter with data
		// Set the recycler view with adapter
		mPersonalAdapter = new MainPersonalAdapter(data, mButtonGroup);
		if (rootView.getAdapter() == null) {
			rootView.setAdapter(mPersonalAdapter);
		} else {
			rootView.swapAdapter(mPersonalAdapter, false);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		rootView.swapAdapter(null, false);
	}

	/**
	 * This method is called inside button group whenever a button is
	 * clicked or when the button group is reloaded
	 * @param uri   the uri that can be used to query database
	 */
	@Override
	public void buttonClicked(Uri uri) {
		mUri = uri;
		getLoaderManager().restartLoader(LOADER_PERSONAL, null, this);
	}


	/**
	 * This is the item holder for personal fragment. It takes normal
	 * item template as well as a group of button.
	 */
	public class MainPersonalAdapter
			extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		// Differentiate two types of view
		private static final int BUTTON_HOLDER = 0;
		private static final int ITEM_HOLDER = 1;
		private static final int ITEM_HOLDER_LAST = 2;

		private Cursor mData;
		private PersonalButtonGroup mGroup;
		private double mLat = -1000;
		private double mLong = -1000;

		Typeface avenirBook;
		Typeface avenirHeavy;
		Typeface avenirLight;

		private int marginName;
		private int marginAddress;
		private int itemWidth;

		private Picasso picasso;

		public class ButtonViewHolder extends RecyclerView.ViewHolder {

			PersonalButtonGroup buttonGroup;

			public ButtonViewHolder(PersonalButtonGroup v) {
				super(v);
				buttonGroup = v;
			}

		}

		public class ItemViewHolder extends RecyclerView.ViewHolder {

			ImageView backgroundImg;

			LinearLayout serviceHolder;
			LinearLayout tagHolder;

			PercentRelativeLayout itemRoot;

			TextView distance;
			TextView cost;
			TextView busName;
			TextView busCat;
			TextView busAddr;
			TextView popPercent;

			View positiveBar;
			View negativeBar;


			public ItemViewHolder(PercentRelativeLayout v) {
				super(v);

				backgroundImg = (ImageView) v.findViewById(R.id.imageview_personal_item_special);

				serviceHolder = (LinearLayout)
						v.findViewById(R.id.linearlayout_service_wrapper_personal_item_special);
				tagHolder = (LinearLayout)
						v.findViewById(R.id.linearlayout_tag_wrapper_personal_item_special);

				itemRoot = v;

				distance = (TextView)
						v.findViewById(R.id.textview_bus_distance_personal_item_special);
				distance.setTypeface(avenirLight);
				cost = (TextView) v.findViewById(R.id.textview_bus_cost_personal_item_special);
				cost.setTypeface(avenirLight);
				busName = (TextView) v.findViewById(R.id.textview_bus_name_personal_item_special);
				busName.setTypeface(avenirHeavy);
				RelativeLayout.LayoutParams busNameParams =
						(RelativeLayout.LayoutParams) busName.getLayoutParams();
				busNameParams.setMargins(0, (int) Utility.dpsToPxRaw(marginName, getContext()),
										 (int) Utility.dpsToPxRaw(20, getContext()), 0);
				busName.setLayoutParams(busNameParams);
				busCat = (TextView)
						v.findViewById(R.id.textview_bus_category_personal_item_special);
				busCat.setTypeface(avenirLight);
				busAddr = (TextView)
						v.findViewById(R.id.textview_bus_address_personal_item_special);
				busAddr.setTypeface(avenirLight);
				RelativeLayout.LayoutParams busAddrParams =
						(RelativeLayout.LayoutParams) busAddr.getLayoutParams();
				busAddrParams.setMargins(0, (int) Utility.dpsToPxRaw(marginAddress, getContext()),
										 (int) Utility.dpsToPxRaw(20, getContext()), 0);
				busAddr.setLayoutParams(busAddrParams);
				popPercent = (TextView)
						v.findViewById(R.id.textview_bus_pop_percent_personal_item_special);
				popPercent.setTypeface(avenirLight);

				positiveBar = v.findViewById(R.id.view_positive_bar_personal_item_special);
				negativeBar = v.findViewById(R.id.view_negative_bar_personal_item_special);

			}

			public ImageView getBackgroundImg() {
				return backgroundImg;
			}

			public LinearLayout getServiceHolder() {
				return serviceHolder;
			}

			public LinearLayout getTagHolder() {
				return tagHolder;
			}

			public PercentRelativeLayout getItemRoot() { return itemRoot; }

			public TextView getDistance() {
				return distance;
			}

			public TextView getCost() {
				return cost;
			}

			public TextView getBusName() {
				return busName;
			}

			public TextView getBusCat() {
				return busCat;
			}

			public TextView getBusAddr() {
				return busAddr;
			}

			public TextView getPopPercent() {
				return popPercent;
			}

			public View getPositiveBar() {
				return positiveBar;
			}

			public View getNegativeBar() {
				return negativeBar;
			}
		}

		public MainPersonalAdapter(Cursor data, PersonalButtonGroup group) {

			mData = data;
			mGroup = group;

			if ((ContextCompat.checkSelfPermission(getActivity(),
					Manifest.permission.ACCESS_FINE_LOCATION)
									== PackageManager.PERMISSION_GRANTED) &&
					(((MainActivity) getActivity()).mGoogleApiClient.isConnected())){

				Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
						((MainActivity) getActivity()).mGoogleApiClient);
				mLat = lastLocation.getLatitude();
				mLong = lastLocation.getLongitude();
			}

			avenirBook = Typeface.createFromAsset(getActivity().getAssets(),
												  "fonts/AvenirLTStd-Book.otf");
			avenirHeavy = Typeface.createFromAsset(getActivity().getAssets(),
												  "fonts/AvenirLTStd-Heavy.otf");
			avenirLight = Typeface.createFromAsset(getActivity().getAssets(),
												  "fonts/AvenirLTStd-Light.otf");

			DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
			float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
			float dpMargin = Utility.pxToDpsRaw(
					(int) getResources().getDimension(R.dimen.activity_horizontal_margin),
					getContext()
			);
			itemWidth = (int) (dpWidth - dpMargin * 2);
			marginName = (int) (itemWidth / Math.pow(1.618, 3)) - 34;

			marginAddress = (int) (((itemWidth / 1.618) - (marginName + 34 + 16)
									- 35 - 43) / 2);

			picasso = Picasso.with(getContext());
			picasso.setIndicatorsEnabled(true);

			while (mData.moveToNext()) {
				Log.v(LOG_TAG, "mData tag " + mData.getString(tagEntry.TAG_IDX));
			}
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			switch (viewType) {
				case BUTTON_HOLDER:
					return new ButtonViewHolder(mGroup);
				case ITEM_HOLDER:
					PercentRelativeLayout itemRoot = (PercentRelativeLayout)
							LayoutInflater.from(getContext())
							.inflate(R.layout.personal_item_special, parent, false);
					return new ItemViewHolder(itemRoot);
				case ITEM_HOLDER_LAST:
					PercentRelativeLayout lastItemRoot = (PercentRelativeLayout)
							LayoutInflater.from(getContext())
							.inflate(R.layout.personal_last_item_special, parent, false);
					return new ItemViewHolder(lastItemRoot);
				default:
					throw new UnsupportedOperationException("Unknown type: " + viewType);
			}

		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

			Log.v(LOG_TAG, String.valueOf(position));

			switch (getItemViewType(position)) {

				case BUTTON_HOLDER:

					if (getItemCount() > 1) {
						mData.moveToFirst();
					}

					break;

				// Default accounts for both ITEM_HOLDER & ITEM_HOLDER_LAST because they have
				// the same structure (just different in margin)
				default:

					mData.moveToPosition(position - 1);
					Log.v(LOG_TAG, "Name: " + mData.getString(tagEntry.NAME_IDX)
								+ " - BusID: " + mData.getString(tagEntry.BUSID_IDX));

					ItemViewHolder castedHolder = (ItemViewHolder) holder;

					final String busID = mData.getString(tagEntry.BUSID_IDX);
					final String tag = mData.getString(tagEntry.TAG_IDX);

					castedHolder.getItemRoot().setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Toast.makeText(getActivity(), "Position " + busID + " with tag Id " +
									tag + " is clicked!",
									Toast.LENGTH_SHORT).show();
						}
					});

					// load image
					picasso.load(mData.getString(tagEntry.CIMG_IDX))
							.fit()
							.centerCrop()
							.into(castedHolder.getBackgroundImg());

					// load tags
					castedHolder.getTagHolder().removeAllViews();
					String[] tags = mData.getString(tagEntry.TAG_IDX).split(",");
					for (String eachTag : tags) {
						castedHolder.getTagHolder()
								.addView(new PersonalTag(getContext(),
										prefs.getString(SERVER_RESPONSE.TAG_COLOR
												+ tagEntry.deformatTag(eachTag),
												null),
										tags.length,
										itemWidth/3));
					}

					if ((mLat != -1000) && (mLong != -1000)) {
						castedHolder.getDistance().setText(Utility.formatKM(Utility.getDistance(
								mLat, mLong,
								mData.getDouble(tagEntry.LAT_IDX),
								mData.getDouble(tagEntry.LONG_IDX)
						)));
					}

					castedHolder.getCost()
							.setText(Utility.formatCurrency("$",
									mData.getString(tagEntry.COST_IDX)));

					castedHolder.getBusName()
							.setText(mData.getString(tagEntry.NAME_IDX));

					castedHolder.getBusCat()
							.setText(mData.getString(tagEntry.CAT_IDX));

					castedHolder.getBusAddr()
							.setText(mData.getString(tagEntry.LOC_IDX));

					castedHolder.getPopPercent()
							.setText(mData.getString(tagEntry.POSIT_IDX));



					// TODO: change this dpsToPxRaw number argument to dimen file

					MPercentLayoutParams posParams = new MPercentLayoutParams(
							0, (int) Utility.dpsToPxRaw(5, getActivity()));
					posParams.addRule(RelativeLayout.BELOW,
							R.id.textview_bus_address_personal_item_special);
					posParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					posParams.setMargins(0, (int) Utility.dpsToPxRaw(5, getActivity()),
										(int) Utility.dpsToPxRaw(20, getActivity()), 0);
					posParams.setWidthPercentage(mData.getFloat(tagEntry.POP_IDX));
					castedHolder.getPositiveBar().setLayoutParams(posParams);


					MPercentLayoutParams negParams = new MPercentLayoutParams(
							0, (int) Utility.dpsToPxRaw(5, getActivity()));
					negParams.addRule(RelativeLayout.BELOW,
							R.id.textview_bus_address_personal_item_special);
					negParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					negParams.setMargins(0, (int) Utility.dpsToPxRaw(5, getActivity()),
							(int) Utility.dpsToPxRaw(20, getActivity()), 0);
					negParams.setWidthPercentage(
							(1 - mData.getFloat(tagEntry.POSIT_IDX) / 100) *
							(mData.getFloat(tagEntry.POP_IDX))
					);
					castedHolder.getNegativeBar().setLayoutParams(negParams);


					String binRepresent =
							Integer.toBinaryString(mData.getInt(tagEntry.SERVS_IDX));
					Log.v(LOG_TAG, "Binary representation of " + busID + " is " + binRepresent
								+ " from " + mData.getInt(tagEntry.SERVS_IDX));
					castedHolder.getServiceHolder().removeAllViews();

					for (int index=binRepresent.length()-1; index>=0; index--) {

						if (binRepresent.charAt(index) == 49) {

							switch (binRepresent.length() - 1 - index) {

								case SERVER_RESPONSE.BUS_SER_DELIVERY: {
									Button dButton = (Button)
											LayoutInflater.from(getActivity())
													.inflate(R.layout.button_personal,
															castedHolder.getServiceHolder(), false);
									dButton.setTypeface(avenirBook);
									dButton.setText("ORDER");

									castedHolder.getServiceHolder().addView(dButton);

									dButton.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											Toast.makeText(getContext(),
													"Delivery from " + busID + " is clicked",
													Toast.LENGTH_SHORT)
													.show();
										}
									});

									break;
								}

								case SERVER_RESPONSE.BUS_SER_RESERVE: {

									Button rButton = (Button)
											LayoutInflater.from(getActivity())
													.inflate(R.layout.button_personal,
															castedHolder.getServiceHolder(), false);
									rButton.setTypeface(avenirBook);
									rButton.setText("RESERVE");

									castedHolder.getServiceHolder().addView(rButton);

									rButton.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											Toast.makeText(getContext(),
													"Reservation from " + busID + " is clicked",
													Toast.LENGTH_SHORT)
													.show();
										}
									});

									break;
								}

							}
						}
					}

			}
		}


		@Override
		public int getItemCount() {
			return mData.getCount() + 1;
		}

		@Override
		public int getItemViewType(int position) {

			if (position == 0) {
				return BUTTON_HOLDER;
			} else if (position == mData.getCount()) {
				return ITEM_HOLDER_LAST;
			} else {
				return ITEM_HOLDER;
			}
		}
	}


	/**
	 * This is an intent service that will try to sync user tagged
	 * businesses with the database. Whenever this fragment is opened,
	 * this fragment will send a small request to the server to inquire
	 * whether the data is up-to-date:
	 *  (1) if the data is up-to-date, ignored
	 *  (2) if the data is not up-to-date, update the whole database
	 *      beware in this time not to overthink an overly optimized
	 *      solution
	 *
	 * The main benefit of this intent service is to check whether the
	 * data is out of sync.
	 */
	public static class PersonalSyncService extends IntentService {

		public PersonalSyncService() {
			super(PersonalSyncService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			JSONObject fromServer;
			ArrayList<ContentValues> data;

//			Uri uri = intent.getParcelableExtra(AppConstants.MAIN_ACTIVITY.URL_PERSONAL);
			// TODO: delete this fake data
			Uri uri = Uri.parse("https://www.dropbox.com/s/c989wf3izrtqpkc/personal0.json?dl=1");

			try {
				fromServer = new JSONObject(Utility.sendHTTPRequest(uri));

				// if there is anything to update, update the database, else just do nothing
				if (fromServer.getInt(SERVER_RESPONSE.CODE) == SERVER_RESPONSE.CODE_SUCCESS) {

					// get main data and input to sqlite
					data = Utility.getDataFromJSON(fromServer, null);

					// format tag
					// The reason we want to format tag is because each business can have
					// several tags, and we want to be able to query the database correctly
					// by enclosing each tag with a ||<tag>||. This will eliminate false
					// retrieval.
					// For example, if a user has two businesses each with tag "a" and
					// tag "ba". If we query "a" from SQLite, it will return both "a"
					// and "ba". On the other hand, if we enclose each tag with a
					// ||<tag>||, then ||a|| is completely different from ||ba||. Hence
					// eliminate false retrieval.
					// Android's SQliteDatabase seems not to support regex
					String[] tags;
					ArrayList<ContentValues> formattedData = new ArrayList<>();
					// tagSet to save in SharedPreferences
					// get all tags (note - this should be inefficient since it will loop through
					// all tagged items in the new data, which likely will result in a lot of
					// unnecessary iterations, but for now users don't use this feature much to
					// care about it)
					HashSet<String> tagSet = new HashSet<>();
					for (ContentValues eachContent : data) {

						tags = eachContent.getAsString(tagEntry.COL_TAG).split(",");
						String formattedTag = "";
						for (String eachTag : tags) {
							tagSet.add(eachTag);
							formattedTag = formattedTag + tagEntry.formatTag(eachTag) + ",";
						}

						ContentValues newEachContent = new ContentValues(eachContent);
						newEachContent.put(tagEntry.COL_TAG,
								formattedTag.substring(0, formattedTag.length() - 1));
						formattedData.add(newEachContent);
					}

					// Update the new data to the database
					ContentValues[] insertData = new ContentValues[formattedData.size()];
					formattedData.toArray(insertData);
					getContentResolver().delete(tagEntry.buildGeneralTag(), null, null);
					getContentResolver().bulkInsert(
							tagEntry.buildGeneralTag(), insertData);




					// get all colors associated with tags
					JSONObject tagColor = fromServer.getJSONObject(SERVER_RESPONSE.TAG_COLOR);

					// get shared preferences to store the state of this batch of tags and to
					// store all the tag groups
					SharedPreferences.Editor spEditor = getSharedPreferences(
							PREFERENCES.GLOBAL, MODE_PRIVATE
					).edit();
					spEditor
							.putInt(PREFERENCES.PERSONAL_SYNC,
									fromServer.getInt(SERVER_RESPONSE.EXTRA))
							.putStringSet(PREFERENCES.PERSONAL_BUTTON_GROUP,
										  tagSet);

					for (String eachTag : tagSet) {
						Log.v(LOG_TAG, "eachTag :" + eachTag);
						spEditor.putString(SERVER_RESPONSE.TAG_COLOR + eachTag,
								tagColor.getString(eachTag));
					}

					spEditor.apply();

				}

				Intent broadcastIntent = new Intent(MAIN_ACTIVITY.INTENT_PERSONAL_QUERY);
				LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);


			} catch (JSONException e) {
				Log.e(PersonalFragment.LOG_TAG, "JSONException while downloading data " +
						"in IntentService: " + e.getMessage());
			}

			// TODO: initialize this fragment's cursor loader when there is no data and the data is refreshed

			// ===================

		}
	}


	/**
	 * This broadcast receivers listen to personal sync service and begin
	 * loading data from the database whenever the data is ready.
	 */
	public class PersonalBroadcastReceiver extends BroadcastReceiver {

		private PersonalBroadcastReceiver() {}

		@Override
		public void onReceive(Context context, Intent intent) {

			getLoaderManager().initLoader(LOADER_PERSONAL, null, PersonalFragment.this);

			HashSet<String> stringSet = (HashSet<String>)
					prefs.getStringSet(PREFERENCES.PERSONAL_BUTTON_GROUP, null);
			if (stringSet != null) {
				mButtonGroup.createAllButtons(new ArrayList<>(stringSet));
				mButtonGroup.setOnButtonClicked(PersonalFragment.this);
			}

		}
	}
}
