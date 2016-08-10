package ng.duc.mercury.main;

import android.Manifest;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ng.duc.mercury.AppConstants;
import ng.duc.mercury.AppConstants.MAIN_ACTIVITY;
import ng.duc.mercury.AppConstants.SERVER_RESPONSE;
import ng.duc.mercury.MainActivity;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;
import ng.duc.mercury.custom_views.ImageCarousel;
import ng.duc.mercury.data.DataContract;
import ng.duc.mercury.data.DataContract.aroundEntry;


/**
 * Created by ducnguyen on 6/14/16.
 * This fragment handles the Around information in main activity
 * Note: the root view is a recycler view which is supposed to show list of data.
 * However, when there isn't any data (len(data) == 0) the recycler view will only
 * show view notifying that there isn't anything.
 */
public class AroundFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String LOG_TAG = AroundFragment.class.getSimpleName();

	// Keys that will be used while saving instance state
	private final String OLD_LAT = "lat";
	private final String OLD_LON = "lon";
	private final String STATE = "state";

	// Loader codes
	private static final int LOADER_DEAL = 0;
	private static final int LOADER_EVENT = 1;

	private double mLat;
	private double mLon;

	private String currentState;
	private Location lastLocation = null;
	private String userId;

	private SwipeRefreshLayout root;
	private RecyclerView rootView;

	private static final String INTENT_KEY_REFRESH = "ikr";
	private static final String INTENT_KEY_STATE = "iks";

	// Set the adapter with data
	AroundAdapter dealAdapter = null;
	AroundAdapter eventAdapter = null;

	AroundScrollListener dealScrollListener;
	AroundScrollListener eventScrollListener;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		root = (SwipeRefreshLayout)
				inflater.inflate(R.layout.fragment_around, container, false);
		root.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {

				if (currentState == null) return;

				switch (currentState) {

					case SERVER_RESPONSE.AROUND_DEAL:
						Toast.makeText(getActivity(),
								Utility.BuildURL.aroundSync(userId, currentState, 1,
										dealScrollListener.getCurrentPage()).toString(),
								Toast.LENGTH_SHORT)
								.show();
						// loadData(1);
						break;

					case SERVER_RESPONSE.AROUND_EVENT:
						Toast.makeText(getActivity(),
								Utility.BuildURL.aroundSync(userId, currentState, 1,
										eventScrollListener.getCurrentPage()).toString(),
								Toast.LENGTH_SHORT)
								.show();
						// loadData(1);
						break;

				}
				root.setRefreshing(false);
			}
		});

		rootView = (RecyclerView) root.findViewById(R.id.recyclerview_around);
		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(
				getActivity(), LinearLayoutManager.VERTICAL, false);

		dealScrollListener = new AroundScrollListener(mLayoutManager) {
			@Override
			public void onLoadMore(int page) {
				// Call data here
				loadData(0);
			}
		};

		eventScrollListener = new AroundScrollListener(mLayoutManager) {
			@Override
			public void onLoadMore(int page) {
				// Call data here
				loadData(0);
			}
		};

		rootView.setLayoutManager(mLayoutManager);
		rootView.setItemViewCacheSize(6);


		// Get the user id
		SharedPreferences prefs = getActivity()
				.getSharedPreferences(AppConstants.PREFERENCES.GLOBAL, Context.MODE_PRIVATE);
		userId = prefs.getString(AppConstants.PREFERENCES.USER_ID, null);


		// Get the type
		if (savedInstanceState == null) {
			currentState = SERVER_RESPONSE.AROUND_DEAL;
		} else {
			currentState = savedInstanceState.getString(STATE, SERVER_RESPONSE.AROUND_DEAL);
		}

		// Get the last location
		if ((ContextCompat.checkSelfPermission(getActivity(),
				Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) &&
				(((MainActivity) getActivity()).mGoogleApiClient.isConnected())){

			lastLocation = LocationServices.FusedLocationApi.getLastLocation(
					((MainActivity) getActivity()).mGoogleApiClient);
			if (lastLocation != null) {
				mLat = lastLocation.getLatitude();
				mLon = lastLocation.getLongitude();

				if (savedInstanceState != null) {
					Double oldLat = savedInstanceState.getDouble(OLD_LAT);
					Double oldLon = savedInstanceState.getDouble(OLD_LON);
					if (Utility.getDistance(oldLat, oldLon, mLat, mLon) < 1) {
						// If the new location is less than 1 km to the last location, then
						// we ignore updating
						switch (currentState) {
							case SERVER_RESPONSE.AROUND_DEAL:
								getLoaderManager().initLoader(LOADER_DEAL, null,
															  AroundFragment.this);
								break;
							case SERVER_RESPONSE.AROUND_EVENT:
								getLoaderManager().initLoader(LOADER_EVENT, null,
															  AroundFragment.this);
								break;
						}
						return root;
					}
				}
			}
		}

		// Register intent filter and broadcast receiver (which will listen to the signal that
		// the database is updated
		IntentFilter intentFilter = new IntentFilter(MAIN_ACTIVITY.INTENT_AROUND_QUERY);
		AroundBroadcastReceiver receiver = new AroundBroadcastReceiver();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);

		// Intent that will load up new data when user reaches this around fragment
		loadData(0);

		return root;
	}


	/**
	 * Construct intent and automatically call the intent service.
	 * @param refresh   whether to refresh, 1 = refresh, 0 = no refresh, throw otherwise
	 */
	public void loadData(int refresh) {

		if (!((refresh == 0) || (refresh == 1))) {
			throw new IllegalArgumentException("refresh argument can only be either 1 or 0, " +
					"currently " + refresh);
		}

		Intent intent = new Intent(getActivity(), AroundSyncService.class);

		if ((ContextCompat.checkSelfPermission(getActivity(),
				Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED) &&
				(((MainActivity) getActivity()).mGoogleApiClient.isConnected())) {

			lastLocation = LocationServices.FusedLocationApi.getLastLocation(
					((MainActivity) getActivity()).mGoogleApiClient);
		}

		// TODO: <debug> delete these links and uncomment the later intents
		String url;
		if (currentState.equals(SERVER_RESPONSE.AROUND_DEAL)) {
			switch (dealScrollListener.getCurrentPage()) {
				case 0:
					url = "https://www.dropbox.com/s/giuf5x72z7xoqpj/deal0.json?dl=1";
					break;
				case 1:
					url = "https://www.dropbox.com/s/9v8opqmu1kalsnx/deal1.json?dl=1";
					break;
				case 2:
					url = "https://www.dropbox.com/s/vgmahlcgpuzqkbe/deal2.json?dl=1";
					break;
				default:
					url = "https://www.dropbox.com/s/5anqcnqzovkj105/deal3.json?dl=1";
					break;
			}
		} else {
			switch (eventScrollListener.getCurrentPage()) {
				case 0:
					url = "https://www.dropbox.com/s/sjeg8urn8tzl8l3/event0.json?dl=1";
					break;
				case 1:
					url = "https://www.dropbox.com/s/2ui3rgoo4iw980q/event1.json?dl=1";
					break;
				case 2:
					url = "https://www.dropbox.com/s/zh65ohs8bgq4yuj/event2.json?dl=1";
					break;
				default:
					url = "https://www.dropbox.com/s/w08zzvcz0k1jn56/event3.json?dl=1";
					break;

			}
		}

		Log.v(LOG_TAG, "Link: " + url);
		Uri abc = Uri.parse(url);



		if (lastLocation == null) {



			intent.putExtra(MAIN_ACTIVITY.URL_UPDATE, abc);

//			intent.putExtra(MAIN_ACTIVITY.URL_UPDATE,
//							Utility.BuildURL.aroundSync(userId, currentState, 0,
//							dealScrollListener.getCurrentPage()));
		} else {

			intent.putExtra(MAIN_ACTIVITY.URL_UPDATE, abc);

//			intent.putExtra(MAIN_ACTIVITY.URL_UPDATE,
//							Utility.BuildURL.aroundSync(userId, currentState,
//														lastLocation.getLatitude(),
//														lastLocation.getLongitude(),
//                                                        0, eventScrollListener.getCurrentPage()));
			mLat = lastLocation.getLatitude();
			mLon = lastLocation.getLongitude();
		}

		intent.putExtra(MAIN_ACTIVITY.EXTRA, currentState);
		getActivity().startService(intent);
		getActivity().getContentResolver().delete(
									DataContract.aroundEntry.buildGeneralAroundUri(),
									null, null);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(STATE, currentState);
		outState.putDouble(OLD_LAT, mLat);
		outState.putDouble(OLD_LON, mLon);
	}


	// HANDLE CURSOR DATA FROM DATABASE =================================
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		switch (id) {

			case LOADER_DEAL:
				return new CursorLoader(getActivity(),
						aroundEntry.buildDealUri(),
						aroundEntry.PROJECTION,
						aroundEntry.selectTypePage,
						aroundEntry.constructTypePageArg(SERVER_RESPONSE.AROUND_DEAL,
														 dealScrollListener.getCurrentPage()),
						null);

			case LOADER_EVENT:
				return new CursorLoader(getActivity(),
						aroundEntry.buildEventUri(),
						aroundEntry.PROJECTION,
						aroundEntry.selectTypePage,
						aroundEntry.constructTypePageArg(SERVER_RESPONSE.AROUND_EVENT,
														eventScrollListener.getCurrentPage()),
						null);
		}

		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		switch (loader.getId()) {

			case LOADER_DEAL:
				if (dealAdapter == null) { // first time user queries deals from server
					dealAdapter = new AroundAdapter(data);
					Log.v(LOG_TAG, "dealAdapter is null");
					rootView.setAdapter(dealAdapter);
				} else { // user queries deals before
					if (dealScrollListener.getCurrentPage() == 0) {
						// user refreshes all data, so we start new dealAdapter
						dealAdapter = new AroundAdapter(data);
						rootView.setAdapter(dealAdapter);
					} else { // user scrolls to get more data, add more data to current dealAdapter
						Log.v(LOG_TAG, "dealAdapter: add data");
						dealAdapter.addData(data);
					}
				}
				rootView.addOnScrollListener(dealScrollListener);
				break;

			case LOADER_EVENT:
				if (eventAdapter == null) { // first time user queries deals from server
					eventAdapter = new AroundAdapter(data);
					rootView.setAdapter(eventAdapter);
				} else { // user queries deals before
					if (eventScrollListener.getCurrentPage() == 0) {
						// user refreshes all data, so we start new eventAdapter
						eventAdapter = new AroundAdapter(data);
						rootView.setAdapter(eventAdapter);
					} else { // user scrolls to get more data, add more data to current eventAdapter
						eventAdapter.addData(data);
					}
				}
				rootView.addOnScrollListener(eventScrollListener);
				break;
		}


	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		rootView.swapAdapter(null, false);
	}


	// SEPARATE OBJECTS =================================
	public class AroundAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		ArrayList<AroundObject> mData;

		private int marginInfo;
		private int itemWidth;

		AroundObject header;
		ArrayList<String> mHeaderData = new ArrayList<>(
				Collections.singletonList(aroundEntry.COL_CIMG));

		private Picasso picasso;


		public static final int TAB_HOLDER = 0;
		public static final int EMPTY_HOLDER = 1;
		public static final int CAROUSEL_HOLDER = 2;
		public static final int ITEM_HOLDER = 3;

		public AroundAdapter(Cursor data) {
			mData = new ArrayList<>();

			// initialize the first item, which is the header that stores all header item
			header = new AroundObject(1);
			reconstructData(data);

			DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
			float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
			float dpMargin = Utility.pxToDpsRaw(
					(int) getResources().getDimension(R.dimen.activity_horizontal_margin),
					getContext()
			);
			itemWidth = (int) (dpWidth - dpMargin * 2);
			marginInfo = (int) (itemWidth / Math.pow(1.618, 2)) - 34;

			picasso = Picasso.with(getContext());
			picasso.setIndicatorsEnabled(true);
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			switch (viewType) {
				case TAB_HOLDER:
					return new TabHolder(LayoutInflater.from(getContext())
											.inflate(R.layout.item_around_tab, parent, false));

				case EMPTY_HOLDER:
					return new EmptyHolder(LayoutInflater.from(getContext())
									.inflate(R.layout.item_around_empty, parent, false));

				case CAROUSEL_HOLDER:
					return new CarouselHolder(LayoutInflater.from(getContext())
									.inflate(R.layout.item_around_header_carousel, parent, false));

				case ITEM_HOLDER:
					return new ItemHolder(LayoutInflater.from(getContext())
									.inflate(R.layout.item_around, parent, false));

				default:
					throw new UnsupportedOperationException("Around adapter: Unknown view type: "
															+ viewType);
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

			switch (getItemViewType(position)) {

				case TAB_HOLDER:
					break;

				case EMPTY_HOLDER:
					break;

				case CAROUSEL_HOLDER:

					ImageCarousel imageCarousel = ((CarouselHolder) holder).getImageCarousel();
					imageCarousel.setLayout(R.layout.item_around_header_carousel_1);
					imageCarousel.setData(mData.get(0).getMultipleItems());
					imageCarousel.setIds(new ArrayList<>(
							Collections.singletonList(R.id.carousel_1_image)));
					imageCarousel.setType(new ArrayList<>(Collections.singletonList("image")));
					imageCarousel.setUri(mData.get(0).getMultipleUris());
					imageCarousel.runCarousel();

					Log.v(LOG_TAG, "Carousel is created");

					break;

				case ITEM_HOLDER:

					final Map<String, String> singleItem = mData.get(position-1).getSingleItem();
					ItemHolder singleHolder = (ItemHolder) holder;

					singleHolder.getView().setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Toast.makeText(getContext(), "Business Id: "
									+ singleItem.get(aroundEntry.COL_BUSID)
									+ ", event Id: " + singleItem.get(aroundEntry.COL_EVENTID),
									Toast.LENGTH_SHORT).show();
						}
					});

					picasso.load(singleItem.get(aroundEntry.COL_CIMG))
							.fit()
							.centerCrop()
							.into(singleHolder.getBackground());

					singleHolder.getBusinessName().setText(singleItem.get(aroundEntry.COL_NAME));

					singleHolder.getDistance().setText(singleItem.get(aroundEntry.COL_DISTANCE));

					singleHolder.getInfo().setText(singleItem.get(aroundEntry.COL_EVENT));

					singleHolder.getLocation().setText(singleItem.get(aroundEntry.COL_LOCATION));

					break;

				default:
					throw new UnsupportedOperationException("Around adapter: Unknown view type: "
						+ getItemViewType(position));

			}


		}

		/**
		 * Total item count is one unit larger than total data count, since we want to account for
		 * header view
		 * @return      the number of items that recycler view will show
		 */
		@Override
		public int getItemCount() {
			return mData.size() + 1;
		}

		@Override
		public int getItemViewType(int position) {

			switch (position) {

				case 0:

					if (getItemCount() == 1) {
						return EMPTY_HOLDER;
					} else {
						return TAB_HOLDER;
					}

				case 1:

					if (header.getCountMultiple() > 0) {
						return CAROUSEL_HOLDER;
					} else {
						return ITEM_HOLDER;
					}

				default:
					return ITEM_HOLDER;

			}

		}

		/**
		 * Reconstruct data. Basically, group all header items into a single item. At the same time
		 * keep track of whether to delete old items or just to simply add new items. The end result
		 * is to create an Array of Around-Objects.
		 */
		private void reconstructData(Cursor cursor) {

			while (cursor.moveToNext()) {
				HashMap<String, String> tempData = new HashMap<>();
				for (int i=1; i<aroundEntry.PROJECTION.length-1; i++) {
					// from 1 to .length - 1 because we don't want to include the ID and HEADER
					// column inside the data (since it would unnecessarily increase object size
					if (!cursor.isNull(i)){
						tempData.put(aroundEntry.PROJECTION[i], cursor.getString(i));
					}
				}

				// if this item belongs to header, add it to the first item in mData, otherwise,
				// add to mData
				switch (cursor.getInt(aroundEntry.HEADER_IDX)) {
					case 0:
						mData.add(new AroundObject(tempData));
						break;
					case 1:
						header.addToMultipleItems(tempData);
						break;
					default:
						Log.e(LOG_TAG, "Unknown header type. Skip: " +
								cursor.getInt(aroundEntry.HEADER_IDX));
						break;
				}
			}

			header.generateMultiple(mHeaderData);
			header.generateMultipleUris(aroundEntry.COL_BUSID);

			if (header.getCountMultiple() > 0) {
				mData.add(0, header);
			}

		}


		/**
		 * Add data to the current cursor and array list, notify recycler view about the change
		 * @param cursor    the cursor to be added to the old cursor
		 */
		private void addData(Cursor cursor) {

			int oldSize = getItemCount();
			int count = 0;

			while (cursor.moveToNext()) {
				HashMap<String, String> tempData = new HashMap<>();
				for (int i=1; i<aroundEntry.PROJECTION.length-1; i++) {
					// from 1 to .length - 1 because we don't want to include the ID and HEADER
					// column inside the data (since it would unnecessarily increase object size
					if (!cursor.isNull(i)){
						tempData.put(aroundEntry.PROJECTION[i], cursor.getString(i));
					}
				}

				switch (cursor.getInt(aroundEntry.HEADER_IDX)) {
					case 0:
						mData.add(new AroundObject(tempData));
						count++;
						break;
					case 1:
						Log.v(LOG_TAG, "Don't add any more header to existing recycler view");
						break;
					default:
						Log.e(LOG_TAG, "Unknown header type. Skip: " +
								cursor.getInt(aroundEntry.HEADER_IDX));
						break;
				}
			}

			notifyItemRangeInserted(oldSize, count);
		}


		// -------------------------------------- View holders
		public class ItemHolder extends RecyclerView.ViewHolder {

			ImageView background;
			TextView distance;
			TextView info;
			TextView businessName;
			TextView location;

			public ItemHolder(View v) {
				super(v);
				background = (ImageView) v.findViewById(R.id.imageview_around_background);
				distance = (TextView) v.findViewById(R.id.textview_around_distance);
				info = (TextView) v.findViewById(R.id.textview_around_info);
				RelativeLayout.LayoutParams busNameParams =
						(RelativeLayout.LayoutParams) info.getLayoutParams();
				busNameParams.setMargins(0, (int) Utility.dpsToPxRaw(marginInfo, getContext()),
						(int) Utility.dpsToPxRaw(20, getContext()), 0);

				businessName = (TextView) v.findViewById(R.id.textview_around_business);
				location = (TextView) v.findViewById(R.id.textview_around_location);
			}

			public ImageView getBackground() {
				return background;
			}

			public TextView getDistance() {
				return distance;
			}

			public TextView getInfo() {
				return info;
			}

			public TextView getBusinessName() {
				return businessName;
			}

			public TextView getLocation() {
				return location;
			}

			public PercentRelativeLayout getView() {
				return (PercentRelativeLayout) itemView;
			}
		}

		public class CarouselHolder extends RecyclerView.ViewHolder {

			ImageCarousel imageCarousel;

			public CarouselHolder(View v) {
				super(v);
				imageCarousel = (ImageCarousel) v.findViewById(R.id.image_carousel_around_1);
			}

			public ImageCarousel getImageCarousel() {
				return imageCarousel;
			}

			public RelativeLayout getView() {
				return (RelativeLayout) itemView;
			}
		}

		public class EmptyHolder extends RecyclerView.ViewHolder {

			public EmptyHolder(View v) {
				super(v);
				TextView aroundDeal = (TextView) v.findViewById(R.id.textview_around_deal);
				aroundDeal.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!currentState.equals(SERVER_RESPONSE.AROUND_DEAL)) {
							currentState = SERVER_RESPONSE.AROUND_DEAL;
							loadData(0);
						}
					}
				});

				TextView aroundEvent = (TextView) v.findViewById(R.id.textview_around_event);
				aroundEvent.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!currentState.equals(SERVER_RESPONSE.AROUND_EVENT)) {
							currentState = SERVER_RESPONSE.AROUND_EVENT;
							loadData(0);
						}
					}
				});
			}

			public PercentRelativeLayout getView() {
				return (PercentRelativeLayout) itemView;
			}
		}

		public class TabHolder extends RecyclerView.ViewHolder {

			public TabHolder(View v) {
				super(v);
				TextView aroundDeal = (TextView) v.findViewById(R.id.textview_around_deal);
				aroundDeal.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!currentState.equals(SERVER_RESPONSE.AROUND_DEAL)) {
							Log.v(LOG_TAG, "Deal button clicked");
							currentState = SERVER_RESPONSE.AROUND_DEAL;
							rootView.setAdapter(dealAdapter);
							loadData(0);
						}
					}
				});

				TextView aroundEvent = (TextView) v.findViewById(R.id.textview_around_event);
				aroundEvent.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!currentState.equals(SERVER_RESPONSE.AROUND_EVENT)) {
							Log.v(LOG_TAG, "Event button clicked");
							currentState = SERVER_RESPONSE.AROUND_EVENT;
							rootView.setAdapter(eventAdapter);
							loadData(0);
						}
					}
				});
			}

			public LinearLayout getItem() {
				return (LinearLayout) itemView;
			}
		}
	}

	public abstract class AroundScrollListener extends RecyclerView.OnScrollListener {

		// load data when reaches the last visibleThreshold items
		private int visibleThreshold = 5;

		// the total data after the last load
		private int previousTotalItemCount = 0;

		// the current page
		private int currentPage = 0;

		// whether still waiting for the last set of data to load
		private boolean loading = true;

		RecyclerView.LayoutManager mLayoutManager;

		public AroundScrollListener(RecyclerView.LayoutManager layoutManager) {
			mLayoutManager = layoutManager;
		}

		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

			int lastVisibleItemPosition = 0;
			int totalItemCount = mLayoutManager.getItemCount();

			// Assume that layout manager is a Linear layout manager
			lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager)
					.findLastVisibleItemPosition();

			// Edge cases: when the data inside adapter is reset
			if (totalItemCount < previousTotalItemCount) {
				currentPage = 0;
				previousTotalItemCount = totalItemCount;
				if (totalItemCount == 0) loading = true;
			}

			// If data is still loading, see if number of data has changed, if it does,
			// then it can safely be concluded that data has loaded
			if (loading && (totalItemCount > previousTotalItemCount)) {
				loading = false;
				previousTotalItemCount = totalItemCount;
			}

			// If the data finishes loading, then we call to load more data when t
			if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
				currentPage++;
				onLoadMore(currentPage);
				Log.v(LOG_TAG, "Current page: " + currentPage);
				loading = true;
			}
		}

		public int getCurrentPage() {
			return currentPage;
		}

		public void setCurrentPage(int page) {
			currentPage = page;
		}

		public abstract void onLoadMore(int page);
	}

	public static class AroundSyncService extends IntentService {

		public AroundSyncService() {
			super(AroundSyncService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Uri uri = intent.getParcelableExtra(MAIN_ACTIVITY.URL_UPDATE);
			String currentState = intent.getStringExtra(MAIN_ACTIVITY.EXTRA);
			String state = null;
			int refresh = 0;
			ArrayList<ContentValues> data;
			ContentResolver resolver = getContentResolver();

			try {

				String rawStringData = Utility.sendHTTPRequest(uri);

				// Receive the code from server to see if there is new refreshed data
				// If user swipes to refresh, and the server pass "code"=0, then there
				// is nothing new. Just pass.
				JSONObject tempObject = new JSONObject(rawStringData);
				if (tempObject.has(SERVER_RESPONSE.CODE)) {
					if (tempObject.getInt(SERVER_RESPONSE.CODE)
							== SERVER_RESPONSE.CODE_NULL) {
						// User refreshes, but nothing new happens
						return;
					} else if (tempObject.getInt(SERVER_RESPONSE.CODE)
							== SERVER_RESPONSE.CODE_SUCCESS) {

						// Do refresh here. Basically delete all old data and reset relevant page
						// counter, new data will be inserted later
						state = tempObject.getString(SERVER_RESPONSE.TYPE);
						deleteData(state, resolver);
						refresh = 1;

					}
				}

				state = tempObject.getString(SERVER_RESPONSE.TYPE);

				data = Utility.getDataFromJSON(rawStringData,
						new String[] {SERVER_RESPONSE.TYPE, SERVER_RESPONSE.EXTRA});
				ContentValues[] dataArray = new ContentValues[data.size()];
				data.toArray(dataArray);

				switch (state) {

					case SERVER_RESPONSE.AROUND_DEAL:
						resolver.bulkInsert(aroundEntry.buildDealUri(), dataArray);
						break;

					case SERVER_RESPONSE.AROUND_EVENT:
						resolver.bulkInsert(aroundEntry.buildEventUri(), dataArray);
						break;

					default:
						resolver.delete(aroundEntry.buildGeneralAroundUri(),
										null, null);
						resolver.bulkInsert(
								aroundEntry.buildGeneralAroundUri(),
								dataArray);
				}

			} catch (JSONException e) {
				Log.e(LOG_TAG, "Cannot retrieve data from JSON: " + e.getMessage());
			}

			// If cannot retrieve the state (event/deal) from server, accept to use current state
			if (state == null) state = currentState;

			Intent broadcastIntent = new Intent(MAIN_ACTIVITY.INTENT_AROUND_QUERY);
			broadcastIntent.putExtra(AroundFragment.INTENT_KEY_STATE, state);
			broadcastIntent.putExtra(AroundFragment.INTENT_KEY_REFRESH, refresh);
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

		}

		private void deleteData(String state, ContentResolver resolver) {

			switch (state) {
				case SERVER_RESPONSE.AROUND_DEAL:
					resolver.delete(aroundEntry.buildDealUri(), null, null);
					break;
				case SERVER_RESPONSE.AROUND_EVENT:
					resolver.delete(aroundEntry.buildEventUri(), null, null);
					break;
				default:
					resolver.delete(aroundEntry.buildGeneralAroundUri(),
							null, null);
					break;
			}

		}
	}


	public class AroundBroadcastReceiver extends BroadcastReceiver {

		private AroundBroadcastReceiver() {}

		@Override
		public void onReceive(Context context, Intent intent) {

			if (!isAdded()) {
				Log.e(LOG_TAG, "Broadcast receiver - fragment is not attached to activity");
				return;
			}

			String state = intent.getStringExtra(AroundFragment.INTENT_KEY_STATE);
			// Need to know whether data is refreshed to reset page counter
			int refresh = intent.getIntExtra(AroundFragment.INTENT_KEY_REFRESH, -1);

			// Use the above current state to determine which loader (event or deal) to load
			switch (state) {

				case SERVER_RESPONSE.AROUND_DEAL:
					if (refresh == 1) dealScrollListener.setCurrentPage(0);
					getLoaderManager().initLoader(LOADER_DEAL, null, AroundFragment.this);
					break;

				case SERVER_RESPONSE.AROUND_EVENT:
					if (refresh == 1) eventScrollListener.setCurrentPage(0);
					getLoaderManager().initLoader(LOADER_EVENT, null, AroundFragment.this);
					break;

			}
		}
	}


	public class AroundObject {

		private final String LOG_TAG = AroundObject.class.getSimpleName();

		private int mHeader;

		private Map<String, String> mSingleItem = new HashMap<>();

		private ArrayList<HashMap<String, String>> mTempMultipleItems;
		private ArrayList<String> mMultipleItemKeys;
		private ArrayList<ArrayList<String>> mMultipleItems;
		private ArrayList<String> mMultipleItemUris;

		public AroundObject(int type) {
			mHeader = type;
			switch (mHeader) {

				case 0:
					mSingleItem = new HashMap<>();
					break;

				case 1:
					mTempMultipleItems = new ArrayList<>();
					break;

				default:
					Log.w(LOG_TAG, "No idea if this object is in the header or not:" + type);
					mTempMultipleItems = new ArrayList<>();
					mSingleItem = new HashMap<>();
			}
		}

		/**
		 * Public constructor for single item
		 * @param item  single item
		 */
		public AroundObject(HashMap<String, String> item) {
			mHeader = 0;
			mSingleItem = item;
		}

		public void setSingleItem(HashMap<String, String> singleItem) {

			if (mSingleItem != null) {
				Log.w(LOG_TAG, "mSingleItem is not null (already set). This will replace" +
						" the current object value");
			}
			mSingleItem = singleItem;
		}

		public void addToMultipleItems(HashMap<String, String> singleItem) {
			mTempMultipleItems.add(singleItem);
		}

		public boolean isHeader() {
			return mHeader == 1;
		}

		public Map<String, String> getSingleItem() {
			return mSingleItem;
		}

		/**
		 * Since Image Carousel is only friendly with multiple item of types
		 * ArrayList<ArrayList<String>> and we currently have ArrayList<HashMap<String>>,
		 * we need to turn the later to the former.
		 * @param keys  the keys that specify uniform order inside ArrayList<String>
		 */
		public void generateMultiple(ArrayList<String> keys) {

			mMultipleItemKeys = keys;
			mMultipleItems = new ArrayList<>();
			if ((mTempMultipleItems == null) || (mTempMultipleItems.size() == 0)) return;


			for (HashMap<String, String> eachObject : mTempMultipleItems) {
				ArrayList<String> temp = new ArrayList<>();
				for (String key: mMultipleItemKeys) {
					temp.add(eachObject.get(key));
				}
				mMultipleItems.add(temp);
			}
		}

		public void generateMultipleUris(String key) {

			mMultipleItemUris = new ArrayList<>();
			for (HashMap<String, String> eachObject: mTempMultipleItems) {
				mMultipleItemUris.add(eachObject.get(key));
			}
		}


		public ArrayList<String> getMultipleItemKeys() {
			return mMultipleItemKeys;
		}

		public ArrayList<ArrayList<String>> getMultipleItems() {
			return mMultipleItems;
		}

		public int getCountMultiple() {
			return mMultipleItems.size();
		}

		public ArrayList<String> getMultipleUris() {
			return mMultipleItemUris;
		}
	}
}
