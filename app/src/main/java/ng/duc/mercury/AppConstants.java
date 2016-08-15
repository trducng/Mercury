package ng.duc.mercury;

/**
 * Created by ducnguyen on 6/15/16.
 * This class contains the constants needed to operate many functions
 * in the app. These constants tend to serve as identifiers (which are
 * different from strings in strings.xml)
 */
public class AppConstants {

	public static String APP_NAME = "mercury";


	/**
	 * This class holds the constants that will be used in Main Activity
	 */
	public static class MAIN_ACTIVITY {

		public static final int HOME_FRAGMENT_KEY = 0;

		// Intent filter that listens for INTENT_HOME_UPDATE will listen to a broadcast intent
		// that has INTENT_HOME_UPDATE key. When this intent filter receives the broadcast, it
		// will help LocalBroadcastManager triggers the BroadcastReceiver that is registered and
		// associated with that intent filter to handle the call.
		public static final String INTENT_HOME_UPDATE = "ng.duc.mercury.MainActivity.Home.BROADCAST";
		public static final String INTENT_PERSONAL_QUERY =
				"ng.duc.mercury.MainActivity.Personal.QUERY";
		public static final String INTENT_AROUND_QUERY =
				"ng.duc.mercury.MainActivity.Around.QUERY";

		// This is a key in Intent.putExtra.
		public static final String EXTRA = "ng.duc.mercury.MainActivity.EXTRA";
		public static final String UPDATE_HOME_ARRAYLIST = "ng.duc.mercury.MainActivity.EXTRA_DATA";
		public static final String URL_HOME = "ng.duc.mercury.MainActivity.LOCATION";

		public static final String URL_UPDATE = "ng.duc.mercury.MainActivity.URL";
	}

	public static class BUSINESS_ACTIVITY {

		public static final String INTENT_FILTER_DRAWER = "ng.duc.mercury.BusActivity.DRAWER";

		public static final String INTENT_URL_UPDATE = "ng.duc.mercury.BusActivity.URL";
		public static final String INTENT_EXTRA = "ng.duc.mercury.BusActivity.EXTRA";

		public static final String BUNDLE_BUS_ID = "busId";
		public static final String BUNDLE_EVENT_ID = "eventId";

	}

	/**
	 * This class holds the constants that serve as the keys in JSON key-value pair from server
	 */
	public static class SERVER_RESPONSE {

		public static final String RESULT = "result";

		// items general ====================================================
		public static final String BUS_ID = "busID";
		public static final String BUS_NAME = "busName";
		public static final String BUS_LOC = "loc";
		public static final String BUS_SER = "ser";
		public static final String BUS_COVER_IMG = "covImg";
		public static final String BUS_DISTANCE = "dis";
		public static final String BUS_CAT = "cat";
		public static final String BUS_COST = "cost";
		public static final String BUS_POP = "popbar";
		public static final String BUS_POSITIVE = "plus";

		// items tag information
		public static final String LAT = "lat";
		public static final String LONG = "long";
		public static final String TAG = "tag";
		public static final String TAG_COLOR = "tagColor";
		public static final String TIMEADDED = "timeAdded";


		// ==================================================================

		// business pages ===================================================

		// drawer business navigation
		public static final String DRAWER = "drawer";   // key
		public static final String DRAWER_BUSINESS_INFO = "0"; // one value
		public static final String DRAWER_PRODUCT_INFO = "1";   // one value
		public static final String DRAWER_LOYALTY = "2";        // one value

		// pages in general business info
		public static final String BUS_INFO_NUM_EVENTS = "busInfoNumEvents";

		// business general information
		public static final String BUS_NUM_IMGS = "numImg";
		public static final String BUS_CONTACT = "contact";
		public static final String BUS_HOURS = "hours";

		// business general recommendations and tips
		public static final String REC_USER = "user";
		public static final String REC_IMG = "img";
		public static final String REC_CONTENT = "content";

		// business other information
		public static final String BUS_INFO_SPECIAL = "special";
		public static final String BUS_INFO_FAV = "fav";
		public static final String BUS_INFO_TIPS = "tips";

		// event information
		public static final String EVENT_IMG = "img";
		public static final String EVENT_NAME = "name";
		public static final String EVENT_CAT = "cat";
		public static final String EVENT_TIME = "time";
		public static final String EVENT_GOING = "going";
		public static final String EVENT_DESCRIPTION = "desc";
		public static final String EVENT_SCHEDULE = "sche";
		public static final String EVENT_TRANSPORTATION = "trans";
		public static final String EVENT_OTHER_INFO = "other";

		public static final int EVENT_GOING_GO = 1;
		public static final int EVENT_GOING_MAYBE = 0;

		// ==================================================================

		// products =========================================================

		public static final String ITEM_IMAGE = "image";
		public static final String ITEM_NAME = "name";
		public static final String ITEM_PRICE = "price";

		public static final String ITEM_ID = "id";
		public static final String ITEM_CLICKABLE = "click";
		public static final int ITEM_CLICKABLE_POSITIVE = 1;
		public static final int ITEM_CLICKABLE_NEGATIVE = 0;

		// ==================================================================

		// around tag information
		public static final String EVENT_INFO = "ev";
		public static final String EVENT_ID = "evID";
		public static final String TYPE = "type";
		public static final String AROUND_DEAL = "deal";
		public static final String AROUND_EVENT = "event";
		public static final String HEADER = "header";

		// status code
		public static final String CODE = "code";
		public static final int CODE_SUCCESS = 1;
		public static final int CODE_ERROR = -1;
		public static final int CODE_NULL = 0;


		// general extra
		public static final String EXTRA = "extra";

		// service values
		public static final int BUS_SER_DELIVERY = 0;
		public static final int BUS_SER_RESERVE = 1;


	}


	/**
	 * This class holds the components needed to build URL
	 */
	public static class URL_CONSTANTS {

		public static String SERVER_NAME = "https://www.mercury.com";

		public static String TAG = "tag";
		public static String AROUND = "around";
		public static String BUS = "bus";
		public static String BUS_DRAWER = "drawer";
		public static String BUS_NUM_EVENTS = "numEvent";
		public static String BUS_INFO = "busInfo";


		public static String USER_ID = "userId";
		public static String EXTRA = "extra";
		public static String TYPE = "type";
		public static String LAT = "lat";
		public static String LON = "lon";

		public static String REFRESH = "refresh";

		public static String BUS_ID = "busId";
		public static String EVENT_ID = "eventId";

	}

	/**
	 * This class holds constants for SharedPreferences
	 */
	public static class PREFERENCES {

		public static final String GLOBAL = "ng.duc.mercury";

		public static final String USER_ID = "uID";
		public static final String USER_NAME = "uName";
		public static final String USER_PIC = "uPic";

		public static final String PERSONAL_SYNC = "personalSync";
		public static final String PERSONAL_BUTTON_GROUP = "personalButtons";

	}

}
