package ng.duc.mercury.custom_views;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ng.duc.mercury.AppConstants;
import ng.duc.mercury.R;

/**
 * Created by ducnguyen on 8/10/16.
 * This is the navigation drawer for the business class (dynamic business menu item)
 */
public class BusNavDrawer extends RecyclerView {

	private static final String LOG_TAG = BusNavDrawer.class.getSimpleName();

	private BusNavAdapter mAdapter;
	// To keep track which business page this drawer in (general info or product info or loyalty..)
	private String currentState = null;

	public BusNavDrawer(Context context) {
		super(context);
		mAdapter = new BusNavAdapter(getContext());
	}

	public BusNavDrawer(Context context, AttributeSet attrs) {
		super(context, attrs);
		mAdapter = new BusNavAdapter(getContext());
	}

	public BusNavDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mAdapter = new BusNavAdapter(getContext());
	}

	public void addBusinessNavigation(String busNav) {
		mAdapter.setBusNav(busNav);
	}

	public void createBusinessNavigationView() {
		LayoutManager layoutManager = new LinearLayoutManager(
				getContext(), LinearLayoutManager.VERTICAL, false);
		this.setLayoutManager(layoutManager);
		this.setItemViewCacheSize(10);

		if (!mAdapter.busNavSetted()) {
			Log.e(LOG_TAG, "Business navigation is not set");
			return;
		}

		if (currentState == null) {
			Log.e(LOG_TAG, "Business current state is not set");
			return;
		}

		mAdapter.reconstructData();
		this.setAdapter(mAdapter);
	}

	public void createBusinessNavigationView(String busNav, String state) {
		addBusinessNavigation(busNav);
		setCurrentState(state);
		createBusinessNavigationView();
	}

	/**
	 * Set the current business page that the drawer is in
	 * @param state     the state from AppConstants.SERVER_RESPONSE.DRAWER...
	 */
	public void setCurrentState(String state) {

		if ((!state.equals(AppConstants.SERVER_RESPONSE.DRAWER_BUSINESS_INFO)) &&
			(!state.equals(AppConstants.SERVER_RESPONSE.DRAWER_PRODUCT_INFO)) &&
			(!state.equals(AppConstants.SERVER_RESPONSE.DRAWER_LOYALTY))) {
			throw new IllegalArgumentException(LOG_TAG + ": Unknown state: " + state);
		}
		currentState = state;
	}



	public class BusNavAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private final String LOG_TAG = BusNavAdapter.class.getSimpleName();

		private final int HEADER_HOLDER = 0;
		private final int TEXT_HOLDER = 1;
		private final int SEP_HOLDER = 2;

		private boolean busNavSetted = false;

		private Map<String, String> mHeader = new HashMap<>();
		private ArrayList<Integer> mSettings = new ArrayList<>(
				Arrays.asList(
						R.string.drawer_messages,
						R.string.drawer_settings,
						R.string.drawer_saved));
		private ArrayList<Integer> mMercury = new ArrayList<>(
				Arrays.asList(
						R.string.drawer_about,
						R.string.drawer_privacy,
						R.string.drawer_sign_out));
		private ArrayList<Integer> mBusNav;
		private ArrayList<Integer> mData = new ArrayList<>();


		public BusNavAdapter(Context context) {

			SharedPreferences prefs = context.getSharedPreferences(
					AppConstants.PREFERENCES.GLOBAL, Context.MODE_PRIVATE);

			mHeader.put(AppConstants.PREFERENCES.USER_NAME,
					   prefs.getString(AppConstants.PREFERENCES.USER_NAME,
							            getContext().getString(R.string.drawer_anonymous)));
			mHeader.put(AppConstants.PREFERENCES.USER_PIC,
					   prefs.getString(AppConstants.PREFERENCES.USER_PIC, null));

		}

		public void setBusNav(String busNav) {

			mBusNav = new ArrayList<>();
			String[] busNavs = busNav.split("(?!^)");

			for (String string : busNavs) {
				Log.v(LOG_TAG, "Split 012: " + string);
			}

			for (String string : busNavs) {

				switch (string) {
					case AppConstants.SERVER_RESPONSE.DRAWER_BUSINESS_INFO:
						mBusNav.add(R.string.drawer_bus_general);
						break;
					case AppConstants.SERVER_RESPONSE.DRAWER_PRODUCT_INFO:
						mBusNav.add(R.string.drawer_bus_product);
						break;
					case AppConstants.SERVER_RESPONSE.DRAWER_LOYALTY:
						mBusNav.add(R.string.drawer_bus_loyalty);
						break;
					default:
						throw new IllegalArgumentException(LOG_TAG + ": Cannot recognize " +
								"this business drawer code: " + string);
				}
			}

			busNavSetted = true;
		}

		public void reconstructData() {

			for (int item : mBusNav) {
				mData.add(item);
			}
			// -1 means that it is separator
			mData.add(-1);
			for (int item : mSettings) {
				mData.add(item);
			}
			mData.add(-1);
			for (int item : mMercury) {
				mData.add(item);
			}

		}

		public boolean busNavSetted() {
			return busNavSetted;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			switch (viewType) {

				case HEADER_HOLDER:
					return new HeaderHolder(LayoutInflater.from(getContext())
								.inflate(R.layout.item_bus_nav_header, parent, false));

				case TEXT_HOLDER:
					return new TextHolder(LayoutInflater.from(getContext())
								.inflate(R.layout.item_bus_nav_text, parent, false));

				case SEP_HOLDER:
					return new SeparatorHolder(LayoutInflater.from(getContext())
								.inflate(R.layout.item_bus_nav_sep, parent, false));

				default:
					throw new UnsupportedOperationException(LOG_TAG + ": Unknown holder" +
							"type: " + viewType);
			}

		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {

			switch (getItemViewType(position)) {

				case HEADER_HOLDER:
					HeaderHolder castedHolder = (HeaderHolder) holder;
					castedHolder.getName().setText(mHeader.get(AppConstants.PREFERENCES.USER_NAME));

					// TODO: need to implement file system. But for now use URL
					String pic = mHeader.get(AppConstants.PREFERENCES.USER_PIC);
					if (pic == null) {
						Picasso.with(getContext())
								.load(R.drawable.test_unknown_user)
								.fit()
								.centerCrop()
								.into(castedHolder.getAvatar());
					} else {
						Picasso.with(getContext())
								.load(pic)
								.fit()
								.centerCrop()
								.into(castedHolder.getAvatar());
					}
					break;

				case TEXT_HOLDER:
					TextView textView = ((TextHolder) holder).getText();
					final int textId = mData.get(position - 1);
					final String state = currentState;
					textView.setText(getContext().getText(textId));

					textView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {

							switch (textId) {

								case R.string.drawer_bus_general:
									if (state.equals(
											AppConstants.SERVER_RESPONSE.DRAWER_BUSINESS_INFO)) {
										return;
									}
									Toast.makeText(getContext(), "Bus general", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_bus_product:
									if (state.equals(
											AppConstants.SERVER_RESPONSE.DRAWER_PRODUCT_INFO)) {
										return;
									}
									Toast.makeText(getContext(), "Bus prod", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_bus_loyalty:
									if (state.equals(
											AppConstants.SERVER_RESPONSE.DRAWER_LOYALTY)) {
										return;
									}
									Toast.makeText(getContext(), "Loyalty", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_messages:
									Toast.makeText(getContext(), "Messages", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_settings:
									Toast.makeText(getContext(), "Settings", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_saved:
									Toast.makeText(getContext(), "Places", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_about:
									Toast.makeText(getContext(), "About", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_privacy:
									Toast.makeText(getContext(), "Privacy", Toast.LENGTH_SHORT)
											.show();
									break;

								case R.string.drawer_sign_out:
									Toast.makeText(getContext(), "Out", Toast.LENGTH_SHORT)
											.show();
									break;

								default:
									throw new UnsupportedOperationException(LOG_TAG + ": Unknown " +
											"text holder type: " + textId);

							}

						}
					});
					break;

				case SEP_HOLDER:
					break;

				default:
					throw new UnsupportedOperationException(LOG_TAG + ": Unknown holder" +
							"type: " + getItemViewType(position));

			}

		}

		/**
		 * Return the total number amount of items. We have:
		 *      - 1 header
		 *      - .size() of bus navigations
		 *      - then 1 separator
		 *      - .size() of settings
		 *      - then 1 separator
		 *      - .size() of Mercury
		 *  So in total we have: 3 + .size bus + .size() settings + .size() Mercury
		 * @return  the total number amount of items
		 */
		@Override
		public int getItemCount() {
			return mData.size() + 1;
		}

		/**
		 * A typical navigation has the following basic structure:
		 *
		 * 0        Header
		 * 1        ______
		 * 2        BusItem1
		 * 3        BusItem2
		 * 4        BusItem3
		 * 5        ______
		 * 6        SetItem1
		 * 7        SetItem2
		 * 8        ______
		 * 9        Mercury1
		 * 10       Mercury2
		 *
		 * @param position  the position in recycler view
		 * @return          the item code
		 */
		@Override
		public int getItemViewType(int position) {

//			if (position == 0) {
//				return HEADER_HOLDER;
//			} else if ((position == 1) || (position == 1 + mBusNav.size() + 1)
//					   || (position == 1 + mBusNav.size() + 1 + mSettings.size() + 1)) {
//				return SEP_HOLDER;
//			} else {
//				return TEXT_HOLDER;
//			}

			if (position == 0) {
				return HEADER_HOLDER;
			} else if (mData.get(position - 1) == -1) {
				return SEP_HOLDER;
			} else {
				return TEXT_HOLDER;
			}

		}

		// -------------------------------------- View holders
		public class HeaderHolder extends RecyclerView.ViewHolder {

			ImageView imageView;
			TextView textView;

			public HeaderHolder(View view) {
				super(view);
				imageView = (ImageView) view.findViewById(R.id.image_bus_nav_ava);
				textView = (TextView) view.findViewById(R.id.text_bus_nav_name);
			}

			public ImageView getAvatar() {
				return imageView;
			}

			public TextView getName() {
				return textView;
			}
		}

		public class TextHolder extends RecyclerView.ViewHolder {

			public TextHolder(View view) {
				super(view);
			}

			public TextView getText() {
				return (TextView) itemView;
			}

		}

		public class SeparatorHolder extends RecyclerView.ViewHolder {

			public SeparatorHolder(View view) {
				super(view);
			}
		}
	}

	// TODO: BusNavObject might be unnecessary
	public static class BusNavObject {

		private static final String LOG_TAG = BusNavObject.class.getSimpleName();

		public static final int HEADER_TYPE = 0;
		public static final int SEP_TYPE = 1;
		public static final int BUS_TYPE = 2;
		public static final int SET_TYPE = 3;
		public static final int MER_TYPE = 4;

		private int mType;
		private String mName;
		private String mExtraData;

		public BusNavObject() {}

		public BusNavObject(int type) {
			mType = type;
		}

		public BusNavObject(int type, String name) {
			mType = type;
			mName = name;
		}

		public BusNavObject(int type, String name, String extraData) {
			mType = type;
			mName = name;
			mExtraData = extraData;
		}

		public void setType(int type) {
			mType = type;
		}

		public void setName(String name) {
			mName = name;
		}

		public void setExtraData(String extraData) {
			mExtraData = extraData;
		}

		public String getExtraData() {
			return mExtraData;
		}

		public String getName() {
			return mName;
		}

		public int getType() {
			return mType;
		}

	}
}
