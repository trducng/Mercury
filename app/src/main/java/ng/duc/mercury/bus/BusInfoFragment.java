package ng.duc.mercury.bus;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import ng.duc.mercury.AppConstants.BUSINESS_ACTIVITY;
import ng.duc.mercury.AppConstants.SERVER_RESPONSE;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;
import ng.duc.mercury.custom_views.HeaderListImage;
import ng.duc.mercury.custom_views.HeaderListText;
import ng.duc.mercury.custom_views.ImageCarousel;
import ng.duc.mercury.data.DataContract.busInfoEntry;

/**
 * Created by ducnguyen on 8/11/16.
 * Fragment that shows general business information.
 * Link: http://lorempixel.com/402/402/
 */
public class BusInfoFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String LOG_TAG = BusInfoFragment.class.getSimpleName();

	private static final int LOADER_GENERAL = 0;
	private static final String INTENT_FILTER_GENERAL = "generalFilter";
	private static final String INTENT_FILTER_OTHER = "otherFilter";

	// True means information is being downloading or was downloaded. False otherwise
	private boolean downloadGeneral = false;
	private boolean downloadOther = false;


	private int mOpen = -1;
	private String busId;
	private RecyclerView mRootView;
	private ImageView mBackground;
	private BusInfoAdapter mAdapter = null;

	private GeneralBroadcastReceiver mGeneralReceiver = null;
	private OtherInfoBroadcastReceiver mOtherReceiver = null;

	private Picasso picasso;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		Bundle arg = getArguments();
		if (arg != null) {
			busId = arg.getString(BUSINESS_ACTIVITY.BUNDLE_BUS_ID);
		}
		if (busId == null) {
			throw new IllegalStateException(LOG_TAG + ": Business id cannot be null");
		}

		picasso = Picasso.with(getContext());
		picasso.setIndicatorsEnabled(true);

		View rootView = inflater.inflate(R.layout.fragment_bus_info, container, false);

		mBackground = (ImageView) rootView.findViewById(R.id.image_bus_info);

		mRootView = (RecyclerView) rootView.findViewById(R.id.recyclerview_bus_info);
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
				getActivity(), LinearLayoutManager.VERTICAL, false);
		mRootView.setLayoutManager(layoutManager);
		getLoaderManager().initLoader(LOADER_GENERAL, null, BusInfoFragment.this);


		return rootView;
	}

	@Override
	public void onDestroy() {
		if (mGeneralReceiver != null) {
			LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mGeneralReceiver);
		}
		if (mOtherReceiver != null) {
			LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mOtherReceiver);
		}
		super.onDestroyView();
	}

	public void downloadOtherData() {

		// if the data is being or already downloaded, skip.
		if (downloadOther) return;

		IntentFilter mOtherFilter = new IntentFilter(INTENT_FILTER_OTHER);
		mOtherReceiver = new OtherInfoBroadcastReceiver();
		LocalBroadcastManager.getInstance(getActivity())
				.registerReceiver(mOtherReceiver, mOtherFilter);

		Intent otherIntent = new Intent(getActivity(), OtherInfoIntentService.class);
//		otherIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
//							 Utility.BuildURL.busInfoOtherSync(busId));
		// TODO: <debug> delete this fake url later
		otherIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
				Uri.parse("https://www.dropbox.com/s/jpjl68ssqyr915l/busInfoOther.json?dl=1"));
		getActivity().startService(otherIntent);

	}


	// HANDLE CURSOR DATA FROM DATABASE =================================
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		return new CursorLoader(getActivity(),
				busInfoEntry.buildBusUri(busId),
				busInfoEntry.PROJECTION,
				null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		if (data.getCount() == 0) {

			if (downloadGeneral) return;

			IntentFilter generalFilter = new IntentFilter(INTENT_FILTER_GENERAL);
			mGeneralReceiver = new GeneralBroadcastReceiver();
			LocalBroadcastManager.getInstance(getActivity())
					.registerReceiver(mGeneralReceiver, generalFilter);

			Intent generalIntent = new Intent(getActivity(), MainInfoIntentService.class);
//			generalIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
//					Utility.BuildURL.busInfoSync(busId));
			// TODO: <debug> delete this fake data later
			generalIntent.putExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE,
					Uri.parse("https://www.dropbox.com/s/2d3ewz37wvs13vi/busInfoMain.json?dl=1"));
			getActivity().startService(generalIntent);

		} else {

			Log.v(LOG_TAG, "Loaded: " + data.getCount());

			data.moveToFirst();
			picasso.load(data.getString(busInfoEntry.CIMG_IDX))
					.fit()
					.centerCrop()
					.into(mBackground);

			mAdapter = new BusInfoAdapter(data);

			downloadOtherData();

		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mRootView.swapAdapter(null, false);
	}

	// SEPARATE OBJECTS =================================
	public class BusInfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final String LOG_TAG = BusInfoAdapter.class.getSimpleName();

		private final int MAIN_HOLDER = 1;
		private final int SPECIAL_HOLDER = 2;
		private final int FAV_HOLDER = 3;
		private final int TIP_HOLDER = 4;

		Cursor mainData;
		ArrayList<BusInfoOtherObject> otherData = new ArrayList<>();

		public BusInfoAdapter(Cursor data) {
			mainData = data;

		}

		public void addOtherData(BusInfoOtherObject object) {
			otherData.add(object);
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			switch (viewType) {
				case MAIN_HOLDER:
					return new MainHolder(LayoutInflater
							.from(getContext())
							.inflate(R.layout.item_bus_info_main, mRootView, false));

				case SPECIAL_HOLDER:
					return new SpecialHolder(otherData.get(getIndex(viewType)).getItem());

				case FAV_HOLDER:
					return new FavHolder(otherData.get(getIndex(viewType)).getItem());

				case TIP_HOLDER:
					return new TipsHolder(otherData.get(getIndex(viewType)).getItem());
			}

			return null;
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

			if (position == 0) {

				MainHolder castedHolder = (MainHolder) holder;

				mainData.moveToFirst();


				castedHolder.getName().setText(mainData.getString(busInfoEntry.NAME_IDX));
				castedHolder.getCat().setText(mainData.getString(busInfoEntry.CAT_IDX));
				castedHolder.getAddress().setText(mainData.getString(busInfoEntry.LOC_IDX));
				castedHolder.getContact().setText(mainData.getString(busInfoEntry.CONTACT_IDX));
				castedHolder.getHours().setText(mainData.getString(busInfoEntry.HOURS_IDX));

				int numImage = mainData.getInt(busInfoEntry.IMGS_IDX);

				ArrayList<ArrayList<String>> images = new ArrayList<>();
				for (int i=0; i<numImage; i++) {
					int j = 400 + i;
					ArrayList<String> temp = new ArrayList<>(
							Collections.singletonList("http://lorempixel.com/" + j + "/" + j)
					);
					images.add(temp);
				}
				int layout = R.layout.item_bus_info_carousel_image;
				ArrayList<Integer> ids = new ArrayList<>(
						Collections.singletonList(R.id.image_bus_info_image_carousel)
				);
				ArrayList<String> types = new ArrayList<>(
						Collections.singletonList("image")
				);

				ImageCarousel imageCarousel = castedHolder.getImageCarousel();
				imageCarousel.setData(images);
				imageCarousel.setLayout(layout);
				imageCarousel.setIds(ids);
				imageCarousel.setType(types);

				imageCarousel.runCarousel();


			} else {

				switch (getItemViewType(position)) {

					case SPECIAL_HOLDER:
						if (otherData.get(position-1).getExtra()
								.equals(BusInfoOtherObject.EXTRA_IMAGE)) {
							((HeaderListImage) ((SpecialHolder) holder).itemView).removeAllViews();
							((HeaderListImage) ((SpecialHolder) holder).itemView).run();
						} else {
							((HeaderListText) ((SpecialHolder) holder).itemView).removeAllViews();
							((HeaderListText) ((SpecialHolder) holder).itemView).run();
						}
						break;

					case FAV_HOLDER:
						if (otherData.get(position-1).getExtra()
								.equals(BusInfoOtherObject.EXTRA_IMAGE)) {
							((HeaderListImage) ((FavHolder) holder).itemView).removeAllViews();
							((HeaderListImage) ((FavHolder) holder).itemView).run();
						} else {
							((HeaderListText) ((FavHolder) holder).itemView).removeAllViews();
							((HeaderListText) ((FavHolder) holder).itemView).run();
						}
						break;

					case TIP_HOLDER:
						((TipsHolder) holder).getItem().removeAllViews();
						((TipsHolder) holder).getItem().run();
						break;

				}

			}
		}

		@Override
		public int getItemCount() {
			return otherData.size() + 1;
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0) {
				return MAIN_HOLDER;
			} else {
				switch (otherData.get(position - 1).getHeader()) {
					case SERVER_RESPONSE.BUS_INFO_SPECIAL:
						return SPECIAL_HOLDER;
					case SERVER_RESPONSE.BUS_INFO_FAV:
						return FAV_HOLDER;
					case SERVER_RESPONSE.BUS_INFO_TIPS:
						return TIP_HOLDER;
					default:
						throw new IllegalArgumentException("Cannot recognize position: " + position);
				}
			}
		}

		public int getIndex(int viewType) {

			switch (viewType) {

				case SPECIAL_HOLDER:

					for (int i=0; i<otherData.size(); i++) {
						if (otherData.get(i).getHeader().equals(SERVER_RESPONSE.BUS_INFO_SPECIAL)) {
							return i;
						}
					}

					throw new IllegalArgumentException("Unrecognized view type: " + viewType);

				case FAV_HOLDER:

					for (int i=0; i<otherData.size(); i++) {
						if (otherData.get(i).getHeader().equals(SERVER_RESPONSE.BUS_INFO_FAV)) {
							return i;
						}
					}

					throw new IllegalArgumentException("Unrecognized view type: " + viewType);


				case TIP_HOLDER:

					for (int i=0; i<otherData.size(); i++) {
						if (otherData.get(i).getHeader().equals(SERVER_RESPONSE.BUS_INFO_TIPS)) {
							return i;
						}
					}

					throw new IllegalArgumentException("Unrecognized view type: " + viewType);


				default:
					throw new IllegalArgumentException("Unrecognized view type: " + viewType);
			}
		}

		// View holders ------------------------------------------------------------
		public class MainHolder extends RecyclerView.ViewHolder {

//			ImageView background;

			TextView name;
			TextView cat;
			TextView address;
			TextView contact;
			TextView hours;

			ImageCarousel imageCarousel;

			public MainHolder(View v) {
				super(v);

//				background = (ImageView) v.findViewById(R.id.image_bus_info);

				name = (TextView) v.findViewById(R.id.textview_bus_info_name);
				cat = (TextView) v.findViewById(R.id.textview_bus_info_cat);
				address = (TextView) v.findViewById(R.id.textview_bus_info_address);
				contact = (TextView) v.findViewById(R.id.textview_bus_info_contact);
				hours = (TextView) v.findViewById(R.id.textview_bus_info_hours);

				imageCarousel = (ImageCarousel) v.findViewById(R.id.image_carousel_bus_info);
			}

//			public ImageView getBackground() {
//				return background;
//			}

			public TextView getName() {
				return name;
			}

			public TextView getAddress() {
				return address;
			}

			public TextView getCat() {
				return cat;
			}

			public TextView getContact() { return contact; }

			public TextView getHours() {
				return hours;
			}

			public ImageCarousel getImageCarousel() { return imageCarousel; }
		}

		public class SpecialHolder extends RecyclerView.ViewHolder {

			public SpecialHolder(View v) {
				super(v);
			}
		}

		public class FavHolder extends RecyclerView.ViewHolder {

			public FavHolder(View v) {
				super(v);
			}

		}

		public class TipsHolder extends RecyclerView.ViewHolder {

			public TipsHolder(View v) {
				super(v);
			}

			public TipsObject getItem() {
				return (TipsObject) itemView;
			}
		}
	}

	public static class MainInfoIntentService extends IntentService {

		public MainInfoIntentService() {
			super(MainInfoIntentService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Uri url = intent.getParcelableExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE);
			JSONObject tempObject;

			try {
				String rawStringData = Utility.sendHTTPRequest(url);
				tempObject = new JSONObject(rawStringData);

				if ((tempObject.has(SERVER_RESPONSE.CODE)) &&
						(tempObject.getInt(SERVER_RESPONSE.CODE) == SERVER_RESPONSE.CODE_ERROR)) {
					Log.e(LOG_TAG, "Error retrieving business general info: " + rawStringData);
					return;
				}

				ArrayList<ContentValues> rawData = Utility.getDataFromJSON(tempObject, null);
				ContentValues[] data = new ContentValues[rawData.size()];
				rawData.toArray(data);

				int i = getContentResolver().bulkInsert(busInfoEntry.buildGeneralUri(), data);
				Log.v(LOG_TAG, "Inserted: " + i);

			} catch (JSONException e) {
				Log.e(LOG_TAG, "Cannot retrieve data from JSON: " + e.getMessage());
			}

			Intent generalIntent = new Intent(INTENT_FILTER_GENERAL);
			LocalBroadcastManager.getInstance(this).sendBroadcast(generalIntent);

		}
	}

	public class GeneralBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			downloadGeneral = true;
			getLoaderManager().initLoader(LOADER_GENERAL, null, BusInfoFragment.this);
		}
	}

	public static class OtherInfoIntentService extends IntentService {

		public OtherInfoIntentService() {
			super(OtherInfoIntentService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			Uri url = intent.getParcelableExtra(BUSINESS_ACTIVITY.INTENT_URL_UPDATE);
			String rawStringData = Utility.sendHTTPRequest(url);

			Intent otherBroadcastIntent = new Intent(INTENT_FILTER_OTHER);
			otherBroadcastIntent.putExtra(BUSINESS_ACTIVITY.INTENT_EXTRA, rawStringData);
			LocalBroadcastManager.getInstance(this).sendBroadcast(otherBroadcastIntent);

		}
	}

	public class OtherInfoBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			downloadOther = true;

			String rawStringData = intent.getStringExtra(BUSINESS_ACTIVITY.INTENT_EXTRA);
			JSONArray tempObject;

			try {
				tempObject = new JSONObject(rawStringData).getJSONArray(SERVER_RESPONSE.RESULT);

				for (int i=0; i<tempObject.length(); i++) {

					JSONObject object = tempObject.getJSONObject(i);
					JSONArray data;
					JSONArray tempKeys;
					ArrayList<String> keys;

					BusInfoOtherObject adapterObject = new BusInfoOtherObject(
							object.getString(SERVER_RESPONSE.EXTRA),
							object.getString(SERVER_RESPONSE.TYPE)
					);

					switch (object.getString(SERVER_RESPONSE.TYPE)) {

						case SERVER_RESPONSE.BUS_INFO_FAV:
							data = object.getJSONArray(SERVER_RESPONSE.RESULT);

							tempKeys = data.getJSONObject(0).names();
							keys = new ArrayList<>();
							for (int j=0; j<tempKeys.length(); j++) {
								keys.add(tempKeys.getString(j));
							}

							for (int j=0; j<data.length(); j++) {
								JSONObject singleObject = data.getJSONObject(j);
								for (String key : keys) {

									switch (key) {
										case SERVER_RESPONSE.ITEM_NAME:
											adapterObject.addName(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.ITEM_PRICE:
											adapterObject.addContent(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.ITEM_IMAGE:
											adapterObject.addImage(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.ITEM_ID:
											if (singleObject.getInt(SERVER_RESPONSE.ITEM_CLICKABLE)
													== SERVER_RESPONSE.ITEM_CLICKABLE_POSITIVE) {
												adapterObject.addUri(singleObject.getString(key));
											} else {
												adapterObject.addUri("-");
											}
											break;
									}
								}
							}

							// add the data to adapter
							mAdapter.addOtherData(adapterObject);

							break;


						case SERVER_RESPONSE.BUS_INFO_SPECIAL:
							data = object.getJSONArray(SERVER_RESPONSE.RESULT);

							tempKeys = data.getJSONObject(0).names();
							keys = new ArrayList<>();

							for (int j=0; j<tempKeys.length(); j++) {
								keys.add(tempKeys.getString(j));
							}

							for (int j=0; j<data.length(); j++) {
								JSONObject singleObject = data.getJSONObject(j);

								for (String key : keys) {
									switch (key) {
										case SERVER_RESPONSE.ITEM_NAME:
											adapterObject.addName(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.ITEM_PRICE:
											adapterObject.addContent(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.ITEM_IMAGE:
											adapterObject.addImage(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.ITEM_ID:
											if (singleObject.getInt(SERVER_RESPONSE.ITEM_CLICKABLE)
													== SERVER_RESPONSE.ITEM_CLICKABLE_POSITIVE) {
												adapterObject.addUri(singleObject.getString(key));
											} else {
												adapterObject.addUri("-");
											}
											break;
									}
								}
							}

							// Add to adapter
							mAdapter.addOtherData(adapterObject);
							break;


						case SERVER_RESPONSE.BUS_INFO_TIPS:
							data = object.getJSONArray(SERVER_RESPONSE.RESULT);

							tempKeys = data.getJSONObject(0).names();
							keys = new ArrayList<>();

							for (int j=0; j<tempKeys.length(); j++) {
								keys.add(tempKeys.getString(j));
							}

							for (int j=0; j<data.length(); j++) {
								JSONObject singleObject = data.getJSONObject(j);
								for (String key : keys) {
									switch (key) {
										case SERVER_RESPONSE.REC_USER:
											adapterObject.addName(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.REC_CONTENT:
											adapterObject.addContent(singleObject.getString(key));
											break;
										case SERVER_RESPONSE.REC_IMG:
											adapterObject.addImage(singleObject.getString(key));
											break;
									}
								}
							}

							// Add to adapter
							mAdapter.addOtherData(adapterObject);
							break;
					}
				}

			} catch (JSONException e) {
				Log.e(LOG_TAG, "Cannot retrieve data from JSON: " + e.getMessage());
			}


			// kick off the data into recycler view
			mRootView.setAdapter(mAdapter);
		}
	}

	public class BusInfoOtherObject {

		private final String LOG_TAG = BusInfoOtherObject.class.getSimpleName();

		public static final String EXTRA_IMAGE = "image";
		public static final String EXTRA_TEXT = "text";
		public static final String TYPE_TIPS = "tips";

		private String mExtra;
		private HeaderListImage mHeaderListImage;
		private HeaderListText mHeaderListText;
		private TipsObject mTipsObject;
		private String mHeader;

		public BusInfoOtherObject(String extra, String header) {
			if (!extra.equals(EXTRA_IMAGE) && !extra.equals(EXTRA_TEXT)
					&& !extra.equals(TYPE_TIPS)) {
				throw new IllegalStateException(LOG_TAG + ": Unrecognized state: " + extra);
			}

			mExtra = extra;
			mHeader = header;
			switch (mExtra) {
				case EXTRA_IMAGE:
					mHeaderListImage = new HeaderListImage(getActivity());
					mHeaderListImage.setHeader(header);
					break;
				case EXTRA_TEXT:
					mHeaderListText = new HeaderListText(getActivity());
					mHeaderListText.setHeader(header);
					break;
				case TYPE_TIPS:
					mTipsObject = new TipsObject(getActivity());
					mTipsObject.setHeader(header);
					break;
			}
		}

		public void addName(String name) {

			switch (mExtra) {

				case EXTRA_IMAGE:
					mHeaderListImage.addName(name);
					break;

				case EXTRA_TEXT:
					mHeaderListText.addDataLeft(name);
					break;

				case TYPE_TIPS:
					mTipsObject.addName(name);
					break;

			}

		}

		public void addContent(String price) {

			switch (mExtra) {

				case EXTRA_IMAGE:
					mHeaderListImage.addPrice(price);
					break;

				case EXTRA_TEXT:
					mHeaderListText.addDataRight(price);
					break;

				case TYPE_TIPS:
					mTipsObject.addComment(price);
					break;
			}
		}

		public void addImage(String image) {

			switch (mExtra) {

				case EXTRA_IMAGE:
					mHeaderListImage.addImage(image);
					break;
				case EXTRA_TEXT:
					Log.e(LOG_TAG, "Attempted to add image to text");
					break;
				case TYPE_TIPS:
					mTipsObject.addImage(image);
					break;
			}

		}

		public void addUri(String uri) {

			switch (mExtra) {

				case EXTRA_IMAGE:
					mHeaderListImage.addUri(uri);
					break;

				case EXTRA_TEXT:
					mHeaderListText.addUri(uri);
					break;
			}
		}

		public String getExtra() {
			return mExtra;
		}

		public String getHeader() {
			return mHeader;
		}

		public LinearLayout getItem() {
			switch (mExtra) {
				case EXTRA_IMAGE:
					return mHeaderListImage;
				case EXTRA_TEXT:
					return mHeaderListText;
				case TYPE_TIPS:
					return mTipsObject;
				default:
					return null;
			}
		}
	}

	public class TipsObject extends LinearLayout {

		private final String LOG_TAG = TipsObject.class.getSimpleName();

		private ArrayList<String> mImages = new ArrayList<>();
		private ArrayList<String> mComments = new ArrayList<>();
		private ArrayList<String> mNames = new ArrayList<>();
		private String mHeader = null;

		public TipsObject(Context context) {
			this(context, null);
		}

		public TipsObject(Context context, AttributeSet attr) {
			this(context, attr, 0);
		}

		public TipsObject(Context context, AttributeSet attr, int defStyleAttr) {
			super(context, attr, defStyleAttr);
		}

		@TargetApi(21)
		public TipsObject(Context context, AttributeSet attr, int defStyleAttr,
		                  int defStyleRes) {
			super(context, attr, defStyleAttr, defStyleRes);
		}

		public void addImage(String image) {
			mImages.add(image);
		}

		public void addComment(String comment) {
			mComments.add(comment);
		}

		public void addName(String name) {
			mNames.add(name);
		}

		public void setHeader(String header) {
			mHeader = header;
		}

		public void setImages(ArrayList<String> images) {
			mImages = images;
		}

		public void setComments(ArrayList<String> comments) {
			mComments = comments;
		}

		public void setNames(ArrayList<String> names) {
			mNames = names;
		}

		public void print() {
			Log.v(LOG_TAG, "Images: " + mImages);
			Log.v(LOG_TAG, "Comments: " + mComments);
			Log.v(LOG_TAG, "Names: " + mNames);
		}

		public void init() {

			this.setOrientation(VERTICAL);
			this.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
			));
		}

		public void run() {

			Picasso picasso = Picasso.with(getContext());
			picasso.setIndicatorsEnabled(true);

			if (mHeader == null) {
				throw new IllegalStateException("Null header for tips");
			}

			init();
			LayoutInflater inflater = LayoutInflater.from(getContext());

			TextView header = (TextView) inflater
					.inflate(R.layout.view_headerlist_header, this, false);
			header.setText(mHeader);
			this.addView(header);

			for (int i=0; i<mComments.size(); i++) {

				RelativeLayout childView = (RelativeLayout) inflater
						.inflate(R.layout.item_tips, this, false);

				picasso.load(mImages.get(i))
						.fit()
						.centerCrop()
						.into(((ImageView) childView.findViewById(R.id.imageview_tips)));

				((TextView) childView.findViewById(R.id.textview_tips)).setText(mComments.get(i));
				((TextView) childView.findViewById(R.id.textview_tips_user)).setText(mNames.get(i));

				this.addView(childView);
			}
		}
	}

}
