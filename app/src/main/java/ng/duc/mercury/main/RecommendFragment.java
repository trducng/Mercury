package ng.duc.mercury.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ng.duc.mercury.AppConstants;
import ng.duc.mercury.MainActivity;
import ng.duc.mercury.R;
import ng.duc.mercury.Utility;
import ng.duc.mercury.custom_views.ImageCarousel;
import ng.duc.mercury.custom_views.MPercentLayoutParams;

/**
 * Created by ducnguyen on 6/14/16.
 * This fragment handles greeting and recommending businesses and products to the user. In
 * other word, this thing, in the far future, would be an AI.
 */
public class RecommendFragment extends Fragment {

	private static final String LOG_TAG = RecommendFragment.class.getSimpleName();

	private RecommendAdapter mAdapter;
	private ArrayList<RecommendObject> mData;

	private RecyclerView rootView;

	Typeface avenirBook;
	Typeface avenirHeavy;


	// TEST
	private class DownloadFilesTask extends AsyncTask<URL, Void, String> {

		protected String doInBackground(URL... urls) {

			String link = "https://www.dropbox.com/s/6hmu5ekt4ntx46t/rec.json?dl=1";
			return Utility.sendHTTPRequest(Uri.parse(link));
		}

		protected void onPostExecute(String result) {

			mData = new ArrayList<>();
			try {
				JSONObject jsonObject = new JSONObject(result);
				JSONArray jsonArray = jsonObject.getJSONArray("item");

				for (int i=0; i<jsonArray.length(); i++) {
					mData.add(new RecommendObject(jsonArray.getJSONObject(i)));
				}

			} catch (JSONException e) {
				Log.e(LOG_TAG, "Problem parsing JSON: " + e.getMessage());
			}


			RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity(),
					LinearLayoutManager.VERTICAL, false);
			rootView.setLayoutManager(mLayoutManager);

			mAdapter = new RecommendAdapter(mData);
			rootView.setAdapter(mAdapter);

		}

	}

	// END TEST


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		rootView = (RecyclerView) inflater.inflate(
								R.layout.fragment_recommend, container, false);

//		avenirBook = Typeface.createFromAsset(getActivity().getAssets(),
//				"fonts/AvenirLTStd-Book.otf");
//		avenirHeavy = Typeface.createFromAsset(getActivity().getAssets(),
//				"fonts/AvenirLTStd-Heavy.otf");

		new DownloadFilesTask().execute();

		return rootView;
	}


	public class RecommendAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final String LOG_TAG = RecommendAdapter.class.getSimpleName();

		private static final int ITEM_GREET = 0;
		private static final int ITEM_WISH = 1;
		private static final int ITEM_INFO = 2;
		private static final int ITEM_SINGLE = 3;
		private static final int ITEM_MULTIPLE = 4;

		private ArrayList<RecommendObject> mData;

		private int marginName;
		private int marginAddress;
		private int itemWidth;

		private double mLat;
		private double mLong;

		private Picasso picasso;

		public RecommendAdapter(ArrayList<RecommendObject> data) {
			mData = data;

			if ((ContextCompat.checkSelfPermission(getActivity(),
					Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) &&
					(((MainActivity) getActivity()).mGoogleApiClient.isConnected())){

				Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
						((MainActivity) getActivity()).mGoogleApiClient);
				mLat = lastLocation.getLatitude();
				mLong = lastLocation.getLongitude();
			}

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
		}


		// -------------------------------------- Main logic
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			int layoutId;
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());

			switch (viewType) {

				case ITEM_GREET:
					layoutId = R.layout.item_recommend_greet;
					return new GreetHolder(inflater.inflate(layoutId, parent, false));

				case ITEM_WISH:
					layoutId = R.layout.item_recommend_wish;
					return new WishHolder(inflater.inflate(layoutId, parent, false));

				case ITEM_INFO:
					layoutId = R.layout.item_recommend_info;
					return new InfoHolder(inflater.inflate(layoutId, parent, false));

				case ITEM_SINGLE:
					layoutId = R.layout.item_recommend_single;
					return new SingleHolder(inflater.inflate(layoutId, parent, false));

				case ITEM_MULTIPLE:
					layoutId = R.layout.item_recommend_multiple;
					return new MultipleHolder(inflater.inflate(layoutId, parent, false));

				default:
					throw new UnsupportedOperationException("Unknown recommend object type in "
							+ LOG_TAG + ": " + viewType);
			}

		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

			switch (getItemViewType(position)) {

				case ITEM_GREET:
					((GreetHolder) holder).getItem().setText(mData.get(position).getText());
					break;

				case ITEM_WISH:
					((WishHolder) holder).getItem().setText(mData.get(position).getText());
					break;

				case ITEM_INFO:
					((InfoHolder) holder).getItem().setText(mData.get(position).getText());
					break;

				case ITEM_SINGLE:

					SingleHolder singleHolder = (SingleHolder) holder;
					Map<String, String> singleObject = mData.get(position).getSingleItem();

					singleHolder.itemView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Toast.makeText(getContext(), "Single item", Toast.LENGTH_SHORT).show();
						}
					});

					picasso.load(singleObject.get("covImg"))
							.fit()
							.centerCrop()
							.into(singleHolder.getBackgroundImg());

					if ((mLat != -1000) && (mLong != -1000)) {
						singleHolder.getDistance().setText(Utility.formatKM(Utility.getDistance(
								mLat, mLong,
								Double.parseDouble(singleObject.get("lat")),
								Double.parseDouble(singleObject.get("long"))
						)));
					}

					singleHolder.getCost()
							.setText(singleObject.get("cost"));

					singleHolder.getBusName()
							.setText(singleObject.get("busName"));

					singleHolder.getBusCat()
							.setText(singleObject.get("cat"));

					singleHolder.getBusAddr()
							.setText(singleObject.get("loc"));

					singleHolder.getPopPercent()
							.setText(singleObject.get("plus"));


					float pop = Float.parseFloat(singleObject.get("popbar"));

					MPercentLayoutParams posParams = new MPercentLayoutParams(
							0, (int) Utility.dpsToPxRaw(5, getActivity()));
					posParams.addRule(RelativeLayout.BELOW,
							R.id.textview_bus_address_personal_item_special);
					posParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					posParams.setMargins(0, (int) Utility.dpsToPxRaw(5, getActivity()),
							(int) Utility.dpsToPxRaw(20, getActivity()), 0);
					posParams.setWidthPercentage(pop);
					singleHolder.getPositiveBar().setLayoutParams(posParams);


					MPercentLayoutParams negParams = new MPercentLayoutParams(
							0, (int) Utility.dpsToPxRaw(5, getActivity()));
					negParams.addRule(RelativeLayout.BELOW,
							R.id.textview_bus_address_personal_item_special);
					negParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					negParams.setMargins(0, (int) Utility.dpsToPxRaw(5, getActivity()),
							(int) Utility.dpsToPxRaw(20, getActivity()), 0);
					negParams.setWidthPercentage((1 - pop) * pop);
					singleHolder.getNegativeBar().setLayoutParams(negParams);


					String binRepresent =
							Integer.toBinaryString(Integer.parseInt(singleObject.get("ser")));
					singleHolder.getServiceHolder().removeAllViews();

					for (int index=binRepresent.length()-1; index>=0; index--) {

						if (binRepresent.charAt(index) == 49) {

							switch (binRepresent.length() - 1 - index) {

								case AppConstants.SERVER_RESPONSE.BUS_SER_DELIVERY: {
									Button dButton = (Button)
											LayoutInflater.from(getActivity())
													.inflate(R.layout.button_personal,
															singleHolder.getServiceHolder(), false);
//									dButton.setTypeface(avenirBook);
									dButton.setText("ORDER");

									singleHolder.getServiceHolder().addView(dButton);

									dButton.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											Toast.makeText(getContext(),
													"Delivery is clicked",
													Toast.LENGTH_SHORT)
													.show();
										}
									});

									break;
								}

								case AppConstants.SERVER_RESPONSE.BUS_SER_RESERVE: {

									Button rButton = (Button)
											LayoutInflater.from(getActivity())
													.inflate(R.layout.button_personal,
															singleHolder.getServiceHolder(), false);
//									rButton.setTypeface(avenirBook);
									rButton.setText("RESERVE");

									singleHolder.getServiceHolder().addView(rButton);

									rButton.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											Toast.makeText(getContext(),
													"Reservation is clicked",
													Toast.LENGTH_SHORT)
													.show();
										}
									});

									break;
								}

							}
						}
					}



					break;

				case ITEM_MULTIPLE:
					((MultipleHolder) holder).getText().setText(mData.get(position).getText());
					ImageCarousel carousel = ((MultipleHolder) holder).getCarousel();

					ArrayList<Integer> ids = new ArrayList<>(
							Arrays.asList(R.id.carousel_1_image, R.id.carousel_1_name,
										  R.id.carousel_1_price, R.id.carousel_1_rate));

					carousel.setLayout(R.layout.item_recommend_multiple_carousel_1);
					carousel.setData(mData.get(position).getMultipleItems());
					carousel.setIds(ids);
					carousel.setUri(mData.get(position).getMultipleUris());
					carousel.setType(new ArrayList<String>(
							Arrays.asList("image", "text", "text", "text")
					));
					carousel.runCarousel();

					break;

			}

		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		@Override
		public int getItemViewType(int position) {

			switch (mData.get(position).getType()) {

				case "greet":
					return ITEM_GREET;
				case "wish":
					return ITEM_WISH;
				case "info":
					return ITEM_INFO;
				case "single":
					return ITEM_SINGLE;
				case "multiple":
					return ITEM_MULTIPLE;
				default:
					throw new UnsupportedOperationException("Unknown recommend object type in "
									+ LOG_TAG + ": " + mData.get(position).getType());

			}
		}

		// -------------------------------------- View holders
		public class GreetHolder extends RecyclerView.ViewHolder {

			public GreetHolder(View v) {
				super(v);
			}

			public TextView getItem() {
				return (TextView) itemView;
			}
		}

		public class WishHolder extends RecyclerView.ViewHolder {

			public WishHolder(View v) {
				super(v);
			}

			public TextView getItem() {
				return (TextView) itemView;
			}
		}

		public class InfoHolder extends RecyclerView.ViewHolder {

			public InfoHolder(View v) {
				super(v);
			}

			public TextView getItem() {
				return (TextView) itemView;
			}
		}

		public class MultipleHolder extends RecyclerView.ViewHolder {

			private TextView mText;
			private ImageCarousel mCarousel;

			public MultipleHolder(View v) {
				super(v);

				mText = (TextView) v.findViewById(R.id.textview_recommend);
				mCarousel = (ImageCarousel) v.findViewById(R.id.image_carousel_recommend);
			}

			public TextView getText() {
				return mText;
			}

			public ImageCarousel getCarousel() {
				return mCarousel;
			}
		}

		public class SingleHolder extends RecyclerView.ViewHolder {

			ImageView backgroundImg;

			LinearLayout serviceHolder;

			TextView distance;
			TextView cost;
			TextView busName;
			TextView busCat;
			TextView busAddr;
			TextView popPercent;

			View positiveBar;
			View negativeBar;

			public SingleHolder(View v) {
				super(v);

				backgroundImg = (ImageView) v.findViewById(R.id.imageview_personal_item_special);
				serviceHolder = (LinearLayout)
						v.findViewById(R.id.linearlayout_service_wrapper_personal_item_special);
				distance = (TextView)
						v.findViewById(R.id.textview_bus_distance_personal_item_special);
				cost = (TextView) v.findViewById(R.id.textview_bus_cost_personal_item_special);
				busName = (TextView) v.findViewById(R.id.textview_bus_name_personal_item_special);
//				busName.setTypeface(avenirHeavy);
				RelativeLayout.LayoutParams busNameParams =
						(RelativeLayout.LayoutParams) busName.getLayoutParams();
				busNameParams.setMargins(0, (int) Utility.dpsToPxRaw(marginName, getContext()),
						(int) Utility.dpsToPxRaw(20, getContext()), 0);
				busName.setLayoutParams(busNameParams);
				busCat = (TextView)
						v.findViewById(R.id.textview_bus_category_personal_item_special);
				busAddr = (TextView)
						v.findViewById(R.id.textview_bus_address_personal_item_special);
				RelativeLayout.LayoutParams busAddrParams =
						(RelativeLayout.LayoutParams) busAddr.getLayoutParams();
				busAddrParams.setMargins(0, (int) Utility.dpsToPxRaw(marginAddress, getContext()),
						(int) Utility.dpsToPxRaw(20, getContext()), 0);
				busAddr.setLayoutParams(busAddrParams);
				popPercent = (TextView)
						v.findViewById(R.id.textview_bus_pop_percent_personal_item_special);

				positiveBar = v.findViewById(R.id.view_positive_bar_personal_item_special);
				negativeBar = v.findViewById(R.id.view_negative_bar_personal_item_special);
			}

			public ImageView getBackgroundImg() {
				return backgroundImg;
			}

			public LinearLayout getServiceHolder() {
				return serviceHolder;
			}

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
	}

	public class RecommendObject {

		private final String LOG_TAG = RecommendObject.class.getSimpleName();

		private String mType;
		private String mText;
		private ArrayList<ArrayList<String>> mMultipleItems;
		private ArrayList<String> mUri;
		private Map<String, String> mSingleItem;

		public RecommendObject(JSONObject object) {

			try {
				mType = object.getString("type");

				switch (mType) {
					case "greet":
						mText = object.getString("string");
						break;
					case "wish":
						mText = object.getString("string");
						break;
					case "info":
						mText = object.getString("string");
						break;
					case "multiple":
						mText = object.getString("string");
						mMultipleItems = new ArrayList<>();
						mUri = new ArrayList<>();
						JSONArray array = object.getJSONArray("content");
						for (int i=0; i<array.length(); i++) {
							ArrayList<String> temp = new ArrayList<>();
							JSONObject tempObject = array.getJSONObject(i);
							// Get each detail from an object
							temp.add(tempObject.getString("image"));
							temp.add(tempObject.getString("name"));
							temp.add(tempObject.getString("price"));
							temp.add(tempObject.getString("star"));
							mMultipleItems.add(temp);
							mUri.add(tempObject.getString("busID"));
						}
						break;
					case "single":
						mText = object.getString("string");
						mSingleItem = new HashMap<>();
						JSONObject tempObject = object.getJSONObject("content");
						// Get each detail from the single object
						mSingleItem.put("busName", tempObject.getString("busName"));
						mSingleItem.put("cat", tempObject.getString("cat"));
						mSingleItem.put("cost", tempObject.getString("cost"));
						mSingleItem.put("popbar", tempObject.getString("popbar"));
						mSingleItem.put("plus", tempObject.getString("plus"));
						mSingleItem.put("loc", tempObject.getString("loc"));
						mSingleItem.put("ser", tempObject.getString("ser"));
						mSingleItem.put("covImg", tempObject.getString("covImg"));
						mSingleItem.put("busId", tempObject.getString("busID"));
						mSingleItem.put("lat", tempObject.getString("lat"));
						mSingleItem.put("long", tempObject.getString("long"));
						break;
				}
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Error initiating object: " + e.getMessage());
			}
		}

		public String getType() {
			return mType;
		}

		public String getText() {
			return mText;
		}

		public ArrayList<ArrayList<String>> getMultipleItems() {
			return mMultipleItems;
		}

		public ArrayList<String> getMultipleUris() {
			return mUri;
		}

		public Map<String, String> getSingleItem() {
			return mSingleItem;
		}

	}
}
