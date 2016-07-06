package ng.duc.mercury.main;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
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

import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;

import ng.duc.mercury.AppConstants;
import ng.duc.mercury.MainActivity;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;

/**
 * Created by ducnguyen on 6/14/16.
 * This fragment is responsible for the first visual scene that the users see when
 * they first open the app.
 * To handle Scene compatibility problem (added API level 19, while this program also
 * supports API level 17), use a check to figure out which API level the device is running.
 * For device running 19 and above, put the Scene code in if tag. For device below 19,put
 * the normal ViewGroup.addView() in the else tag.
 * TODO: potential conflict between recycler view scroll and vertical view pager scroll
 * TODO: click to business page, location URI
 */
public class HomeFragment extends Fragment implements
		MainActivity.OnLocationListener {

	private static final String LOG_TAG = HomeFragment.class.getSimpleName();

	private Scene mScene1;  // normal scene
	private Scene mScene2;  // when user is near place(s)
	private Scene mScene3;  // when user is not near any place
	private ViewGroup rootScene;
	private RelativeLayout scene1Container;
	private RelativeLayout scene2Container;
	private RelativeLayout scene3Container;

	// This variable keeps track whether scene2 was instantiated before
	private boolean instantiated = false;
	private ArrayList<ContentValues> listBus;
	private RecyclerView recyclerView;
	// Keeps track whether there is a change in listBus between two consecutive updates from server
	private HashSet<String> lastBusId = new HashSet<>();
	private HashSet<String> currentBusId = new HashSet<>();

	// Used to calculate and keep track total width of recycler. Remember, though stored as
	// dps, these numbers are converted to px
	private float recyclerMargin;
	private float recyclerInnerMargin;
	// It is a square so width and height are equal
	private float recyclerItemWidth;
	private float recyclerWidthLong;
	private float screenWidth;

	// TODO: delete these variables and their uses later
	private boolean fakeLocation = true;

	public HomeFragment() {
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Resources re = getActivity().getResources();

		recyclerMargin = re.getDimension(R.dimen.home_scrollview_margin);
		recyclerInnerMargin = re.getDimension(R.dimen.home_scrollview_image_margin);
		recyclerItemWidth = re.getDimension(R.dimen.home_scrollview_height);

		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

		screenWidth = metrics.widthPixels;
	}

	@Nullable
	@Override
	@TargetApi(19)
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		((MainActivity) getActivity()).setOnLocationListener(this);
		 rootScene = (ViewGroup)
				inflater.inflate(R.layout.fragment_home, container, false);

//		rootScene = (ViewGroup) rootView.findViewById(R.id.fragment_home);

		// Kick in the first scene

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

			mScene1 = Scene.getSceneForLayout(rootScene, R.layout.home_scene1, getActivity());
			Transition scene1Trans = TransitionInflater.from(getActivity())
					.inflateTransition(R.transition.home_scene1_logo_fadein);
			TransitionManager.go(mScene1, scene1Trans);

		} else {

			scene1Container = (RelativeLayout)
					inflater.inflate(R.layout.home_scene1, rootScene, false);
			rootScene.addView(scene1Container);

		}


		// This intent filter is needed to listen to any broadcast in the system that
		// has ID "...Update Home Intent". In this specific situation, this intent
		// filter will listen for IntentService's broadcast when it finishes
		// downloading data
		IntentFilter intentFilter = new IntentFilter(
				AppConstants.MAIN_ACTIVITY.INTENT_HOME_UPDATE);

		// This receiver will run when the intent filter catches a event. As a result
		// this receiver is associated specifically in the intent filter
		HomeImageBroadcastReceiver receiver = new HomeImageBroadcastReceiver();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				receiver, intentFilter);


//		return rootView;
		return rootScene;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/**
	 * This method will be called by the Location services when new location is
	 * updated. When a new location is updated, this method will:
	 * 1. Send an URI request to the server
	 * 2. Get back JSON result
	 * 3. Populate the linear layout inside horizontal scroll view
	 * @param location  the new location
	 */
	@Override
	public void onLocationUpdated(Location location) {

		updateListBus(location);

	}

	/**
	 * Invoked by Google Play location services when it cannot connect to the API or when
	 * the user does not allow location permission
	 */
	@Override
	public void onLocationFailed() {
		moveToScene3();
	}

	/**
	 * Transition from the current scene to scene 3 (scene that does not have image recycler
	 * view and text view)
	 */
	@TargetApi(19)
	private void moveToScene3() {

		// User currently is not near any place, use scene3
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

			mScene3 = Scene.getSceneForLayout(rootScene,
					R.layout.home_scene3, getActivity());
			TransitionManager.go(mScene3);

		} else {

			rootScene.removeAllViews();
			scene3Container = (RelativeLayout) LayoutInflater.from(getActivity())
					.inflate(R.layout.home_scene3, rootScene, false);
			rootScene.addView(scene3Container);

		}


		// start over since the continuous updates of scene 2 are now interrupted
		currentBusId.clear();
		lastBusId.clear();

	}

	/**
	 * Update recycler width whenever recycler data change. This is important
	 * to set the width of recycler as MATCH_PARENT or WRAP_CONTENT:
	 * - MATCH_PARENT: when recycler width is larger than screen width
	 * - WRAP_CONTENT: when recycler width is smaller than screen width
	 */
	private void updateRecyclerWidth() {

		recyclerWidthLong = ( listBus.size() - 2 ) * ( recyclerInnerMargin + recyclerItemWidth )
					+ ( recyclerItemWidth + recyclerMargin )
					+ ( recyclerItemWidth + recyclerInnerMargin + recyclerMargin );

		if (recyclerWidthLong >= screenWidth) {
			recyclerView.getLayoutParams().width = RecyclerView.LayoutParams.MATCH_PARENT;
		} else {
			recyclerView.getLayoutParams().width = RecyclerView.LayoutParams.WRAP_CONTENT;
		}

	}

	/**
	 * This method contact server and download appropriate results whenever location is
	 * changed. It is invoked in onLocationUpdated
	 * @param location  the new location reported by Google Api location services
	 */
	private void updateListBus(Location location) {

		// TODO: <debug> delete these fake locations in production code

		String url1 = "https://www.dropbox.com/s/3pv7o02l6aefmy8/homepage.json?dl=1";
		String url2 = "https://www.dropbox.com/s/ytv3k5dthriduf2/homepage2.json?dl=1";

		Intent intent = new Intent(getActivity(), HomeImageIntentService.class);
		if (fakeLocation) {
			intent.putExtra(AppConstants.MAIN_ACTIVITY.URL_HOME, url1);
			fakeLocation = false;
		} else {
			intent.putExtra(AppConstants.MAIN_ACTIVITY.URL_HOME, url2);
		}
		getActivity().startService(intent);
	}

	/**
	 * This function updates business ID and will be called when scene 2 is updated
	 */
	private void updateListBusId() {

		if ((listBus == null) || (listBus.size() == 0)) {
			lastBusId.clear();
			currentBusId.clear();
		} else {
			lastBusId.clear();
			lastBusId.addAll(currentBusId);
			currentBusId.clear();
			for (ContentValues eachContent : listBus) {
				currentBusId.add(eachContent.getAsString(
						AppConstants.SERVER_RESPONSE.BUS_ID));
			}
		}
	}

	/**
	 * Check whether the latest update from server provides any new information.
	 * Basically, in this method, if the current business ID is different from
	 * the last business ID, then lastBusId.addAll(currentBusId) would have a
	 * larger size than lastBusId (since all the difference will be then added)
	 * @return  true if there is new information, false otherwise
	 */
	private boolean checkNew() {

		updateListBusId();
		int length = lastBusId.size();
		lastBusId.addAll(currentBusId);

		return lastBusId.size() > length;

	}


	/**
	 * This adapter is responsible for feeding image data and associated information
	 * to each item in recycler view. The image data is provided to this adapter
	 * upon construction. When data change, we create new adapter, pass in
	 * the new data and tell recycler view to swap adapter.
	 */
	public class HomeBusAdapter extends RecyclerView.Adapter<HomeBusAdapter.ViewHolder> {

		// Keep track the type of view, as we want to have different margin for
		// the first and last item
		private static final int FIRST_ITEM = 0;
		private static final int MIDDLE_ITEMS = 1;
		private static final int LAST_ITEM = 2;

		private ArrayList<ContentValues> mDataset;

		public class ViewHolder extends RecyclerView.ViewHolder {

			ImageView image;
			TextView text;

			public ViewHolder(View v) {
				super(v);
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast.makeText(getContext(),
								"Item: " + getAdapterPosition() +
								" | BusID: " + mDataset.get(getAdapterPosition())
										.getAsString(AppConstants.SERVER_RESPONSE.BUS_ID), Toast.LENGTH_SHORT)
							 .show();
					}
				});
				image = (ImageView) v.findViewById(R.id.image_recyclerview);
				text = (TextView) v.findViewById(R.id.text_recyclerview);
			}

			public ImageView getImage() {
				return image;
			}

			public TextView getText() {
				return text;
			}
		}

		public HomeBusAdapter(ArrayList<ContentValues> dataset) {
			mDataset = dataset;
		}

		@Override
		public HomeBusAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			int layoutId = -1;

			switch (viewType) {

				case FIRST_ITEM:
					layoutId = R.layout.home_image_bus_first;
					break;

				case MIDDLE_ITEMS:
					layoutId = R.layout.home_image_bus;
					break;

				case LAST_ITEM:
					layoutId = R.layout.home_image_bus_last;
					break;

			}

			LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
					.inflate(layoutId, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {

			ContentValues value = mDataset.get(position);
			Picasso.with(getContext())
					.load(value.getAsString(AppConstants.SERVER_RESPONSE.BUS_COVER_IMG))
					.into(holder.getImage());

			holder.getText()
					.setText(value.getAsString(AppConstants.SERVER_RESPONSE.BUS_NAME));
		}

		@Override
		public int getItemCount() {
			return mDataset.size();
		}

		@Override
		public int getItemViewType(int position) {

			if (position == 0) {
				return FIRST_ITEM;
			} else if (position == (getItemCount() - 1)) {
				return LAST_ITEM;
			} else {
				return MIDDLE_ITEMS;
			}
		}
	}


	/**
	 * This is an intent service that will work in the background to connect to the Internet
	 * and download data about nearest business location. This intent service is "static"
	 * because it will be instantiated in AndroidManifest and not in this class.
	 */
	public static class HomeImageIntentService extends IntentService {

		public HomeImageIntentService() {
			super(HomeImageIntentService.class.getName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {

			ArrayList<ContentValues> data;
			Uri uri = Uri.parse(intent.getStringExtra(AppConstants.MAIN_ACTIVITY.URL_HOME));
			try {
				data = Utility.getDataFromJSON(Utility.sendHTTPRequest(uri), null);
			} catch (JSONException e) {
				Log.e(HomeFragment.LOG_TAG, "JSONException while download data in " +
						"IntentService: " + e.getMessage());
				data = null;
			}

			// Send a broadcast when the data is fetched. This will prompt LocalBroadcastManager
			// to notify any registered broadcast receiver to act
			Intent broadcastIntent = new Intent(AppConstants.MAIN_ACTIVITY.INTENT_HOME_UPDATE)
					.putExtra(AppConstants.MAIN_ACTIVITY.UPDATE_HOME_ARRAYLIST, data);
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
		}
	}

	/**
	 * This broadcast receiver will be invoked Broadcast Manager receives an intent that
	 * matches this receiver's registered intent filter
	 */
	public class HomeImageBroadcastReceiver extends BroadcastReceiver {

		// This will prevent other files and applications to instantiate this class
		private HomeImageBroadcastReceiver() {}

		@TargetApi(19)
		@Override
		public void onReceive(Context context, Intent intent) {

			listBus = intent.getParcelableArrayListExtra(AppConstants.MAIN_ACTIVITY.UPDATE_HOME_ARRAYLIST);

			if (listBus.size() > 0) {
				// User currently near some places, use scene2

				if (!instantiated) {

					// if scene 2 never instantiated, create everything.
					scene2Container = (RelativeLayout) LayoutInflater.from(getActivity())
							.inflate(R.layout.home_scene2, rootScene, false);
					recyclerView = (RecyclerView)
							scene2Container.findViewById(R.id.horizontal_recyclerview_home);
					recyclerView.setItemViewCacheSize(10);

					RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(
							getActivity(), LinearLayoutManager.HORIZONTAL, false);
					recyclerView.setLayoutManager(mLayoutManager);

					HomeBusAdapter adapter = new HomeBusAdapter(listBus);
					updateRecyclerWidth();
					recyclerView.setAdapter(adapter);

					// turn the instantiation switch to true
					instantiated = true;
					updateListBusId();

				} else {

					// if server does not provide any new information, then don't change anything
					if (!checkNew()) {
						return;
					}

					// if scene 2 was already instantiated and we just change the image,
					// which means just swap the adapter
					updateRecyclerWidth();
					recyclerView.swapAdapter(new HomeBusAdapter(listBus), false);

				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

					mScene2 = new Scene(rootScene, (View) scene2Container);
					TransitionManager.go(mScene2);

				} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

					mScene2 = new Scene(rootScene, (ViewGroup) scene2Container);
					TransitionManager.go(mScene2);

				} else {

					rootScene.removeAllViews();
					rootScene.addView(scene2Container);

				}

			} else {
				moveToScene3();
			}
		}
	}

}
