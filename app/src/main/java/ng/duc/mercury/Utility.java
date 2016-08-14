package ng.duc.mercury;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ng.duc.mercury.AppConstants.URL_CONSTANTS;

/**
 * Created by ducnguyen on 6/15/16.
 * This class contains some utility methods that will be used to handle miscellaneous
 * functions throughout the app.
 */
public class Utility {

	private static final String LOG_TAG = Utility.class.getSimpleName();

	/**
	 * This function serializes an object to string
	 * @param object    an object that has Serializable implemented
	 * @return          a string representation of that object
	 */
	public static String serializeToString(Object object) {

		ByteArrayOutputStream byteInput;
		ObjectOutputStream output = null;

		try {
			byteInput = new ByteArrayOutputStream();
			output = new ObjectOutputStream(byteInput);
			output.writeObject(object);
			return Base64.encodeToString(byteInput.toByteArray(),
					Base64.DEFAULT);
		} catch (IOException e) {
			Log.e(LOG_TAG+".serializeToString",
					"IOException during serialization: " + e.getMessage());
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					Log.e(LOG_TAG + ".serializeToString",
							"IOException during closing: " + e.getMessage());
				}
			}
		}
		return null;
	}

	/**
	 * This function deserializes an object from string
	 * @param ser       a serialized string that will be decoded
	 * @return          an object that represented by original string
	 */
	public static Object deserializeFromString(String ser) {

		byte[] data = Base64.decode(ser, Base64.DEFAULT);
		ByteArrayInputStream byteInput = new ByteArrayInputStream(data);
		ObjectInputStream input = null;

		try {
			input = new ObjectInputStream(byteInput);
			return input.readObject();
		} catch (IOException e) {
			Log.e(LOG_TAG + ".deserializeFromString",
					"IOException during deserialize: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			Log.e(LOG_TAG + ".deserializeFromString",
					"ClassNotFoundException during deserialization: " +
							e.getMessage());
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					Log.e(LOG_TAG + ".deserializeFromString",
							"IOException during closing stream: " +
									e.getMessage());
				}
			}
		}
		return null;
	}



	// FORMAT REPRESENTATION #######################################################
	/**
	 * This function formats the distance from getDistance(double, double,
	 * double, double) with appropriate notation. More specifically, if
	 * the distance is below < 10km, it will have format a.b km, if the
	 * distance is below < 100km, it will have format ab.c km, if the distance
	 * is larger than 100km, it will have format abc km
	 * @param distance  the distance between two locations (from getDistance)
	 * @return          the human friendly representation of the distance in km
	 */
	public static String formatKM(double distance) {
		if (distance < 10) {
			String phrase = String.valueOf(distance);
			if (phrase.length() > 3) {
				return "~" + phrase.substring(0, 3) + " km";
			} else {
				return "~" + phrase + " km";
			}
		} else if (distance < 100) {
			String phrase = String.valueOf(distance);
			if (phrase.length() > 4) {
				return "~" + phrase.substring(0, 4) + " km";
			} else {
				return "~" + phrase + " km";
			}
		} else {
			String phrase = String.valueOf(distance).split("\\.")[0];
			return "~" + phrase + " km";
		}
	}


	/**
	 * This function calculates the distance between two points, based on their
	 * respective latitudes and longitudes. The latitudes and longitudes used
	 * in this function are radians
	 * @param lat1      the radians latitude of first location
	 * @param long1     the radians longitude of first location
	 * @param lat2      the radians latitude of second location
	 * @param long2     the radians longitude of second location
	 * @return          the distance between two locations in km
	 */
	public static double getDistance(double lat1, double long1,
	                                 double lat2, double long2) {

		double deltaLat = lat2 - lat1;
		double deltaLong = long2 - long1;

		double a = Math.pow((Math.sin(deltaLat/2)), 2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.pow((Math.sin(deltaLong/2)), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return c * 6371;
	}


	/**
	 * This function attach the currency to the cost
	 * @param cost    the cost
	 * @return          the human-friendly cost
	 */
	public static String formatCurrency(String currency, String cost) {
		return currency + cost;
	}

	// VISUAL HELPERS ################################################################
	public static float pxToDpsRaw(int px, Context context) {

		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();

		return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
	}

	public static float dpsToPxRaw(int dps, Context context) {

		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();

		return dps * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
	}


	/**
	 * This method is called in application class. Upon executed, it will change the default
	 * font asset and replace it with a custom font. As a result, rather than adding a customized
	 * font, we replace the source of an established font to the customized font. As a result,
	 * when we call the established font, the customized source will be used rather than the
	 * established source
 	 * @param context   the context of application
	 * @param typeFont  the type-fontDir map (the type key corresponds to sSystemFont keys. As of
	 *                  API23, the following are supported:
	 *                      - monospace
	 *                      - sans-serif
	 *                      - serif
	 */
	public static void setDefaultFont(Context context, Map<String, String> typeFont) {

		Iterator iterator = typeFont.entrySet().iterator();

		if (Build.VERSION.SDK_INT >= 21) {

			Map<String, Typeface> newMap = new HashMap<>();

			while (iterator.hasNext()) {
				Map.Entry pair = (Map.Entry) iterator.next();
				newMap.put((String) pair.getKey(),
							Typeface.createFromAsset(context.getAssets(),
													 (String) pair.getValue()));
				iterator.remove();
			}

			try {
				final Field staticField = Typeface.class.getDeclaredField("sSystemFontMap");
				staticField.setAccessible(true);
				staticField.set(null, newMap);
			} catch (NoSuchFieldException e) {
				Log.e(LOG_TAG, "Cannot retrieve sSystemFontMap");
			} catch (IllegalAccessException e) {
				Log.e(LOG_TAG, "Cannot put font map to sSystemFontMap");
			}

		} else {

			String typeface;

			while (iterator.hasNext()) {

				Map.Entry pair = (Map.Entry) iterator.next();
				Typeface newTypeface = Typeface.createFromAsset(context.getAssets(),
													   (String) pair.getValue());

				switch ((String) pair.getKey()) {

					case "monospace":
						typeface = "MONOSPACE";
						break;
					case "sans-serif":
						typeface = "SANS_SERIF";
						break;
					case "serif":
						typeface = "SERIF";
						break;
					default:
						typeface = "DEFAULT";
						Log.e(LOG_TAG, "Cannot recognize font " + pair.getKey() + ". Use DEFAULT");

				}

				try {
					final Field staticField = Typeface.class.getDeclaredField(typeface);
					staticField.setAccessible(true);
					staticField.set(null, newTypeface);
				} catch (NoSuchFieldException e) {
					Log.e(LOG_TAG, "Cannot retrieve " + pair.getKey()  + ": " + e.getMessage());
				} catch (IllegalAccessException e) {
					Log.e(LOG_TAG, "Cannot manually set customized font to " + pair.getKey()
							+ ": " + e.getMessage());
				}

				iterator.remove();
			}

		}

	}



	// INTERACT WITH THE INTERNET ####################################################

	/**
	 * This function takes an URI and returns a string version of a JSONObject
	 * (the stringed JSONObject will then be put into getDataFromJSON to extract
	 * ContentValues object to store in the database).
	 * @param uri       the uri request to server
	 * @return          a stringed JSONObject that wil be used to fetch into
	 *                  getDataFromJSON
	 */
	public static String sendHTTPRequest(Uri uri) {

		HttpURLConnection urlConnection = null;
		BufferedReader reader = null;

		try {
			// Construct URL from the URI
			URL url = new URL(uri.toString());

			// Create connection to the Internet, send request type and connect
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");

			urlConnection.connect();

			// Read the input stream into a string
			InputStream inputStream = urlConnection.getInputStream();
			StringBuffer buffer = new StringBuffer();

			if (inputStream == null) {return "";}
			reader = new BufferedReader(new InputStreamReader(inputStream));

			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line + "\n");
			}

			if (buffer.length() == 0) {return "";}
			return buffer.toString();


		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Malformed URL: " + uri.toString());
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error with url.openConnection(): " + e.getMessage());
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}

			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					Log.e(LOG_TAG, "Error closing stream", e);
				}
			}
		}
		return "";
	}

	/**
	 * This function takes a string, which is a serialized (JSON format), convert
	 * it to JSON, and then return a list of ContentValues, which will be used to
	 * update into the database.
	 * The JSON data has the form of
	 * {"item": [json1, json2...], "extraKey1": "value1", "extraKey2": "value2"}
	 * @param rawString     raw string that represents json file
	 * @param extraKeys     these keys hold values that all items in "item" contains
	 *                      (rather store "value1", "value2",... in each of the items
	 *                      in "items", store these values separately which has
	 *                      "extraKey1", "extraKey2",... points to, so that it will
	 *                      takes less time to download data and it will result in
	 *                      a lower data transfer)
	 * @return              a vector of ContentValues. Each ContentValues is each
	 *                      row that can be stored in the database
 	 */
	public static ArrayList<ContentValues> getDataFromJSON(String rawString,
	                                                       @Nullable String[] extraKeys)
			throws JSONException {

		try {

			JSONObject originalFile = new JSONObject(rawString);
			JSONArray jsonFile = originalFile.getJSONArray(AppConstants.SERVER_RESPONSE.RESULT);

			ArrayList<ContentValues> cvFiles = new ArrayList<>(jsonFile.length());

			if (jsonFile.length() == 0) {
				return new ArrayList<>();
			}

			for (int i = 0; i < jsonFile.length(); i++) {

				JSONObject eachRow = jsonFile.getJSONObject(i);
				Iterator<?> keys = eachRow.keys();
				ContentValues eachResult = new ContentValues();

				// Get the value from extraKeys (outside of "item")
				if ((extraKeys != null) && (extraKeys.length > 0)) {
					for (String extraKey: extraKeys) {
						eachResult.put(extraKey, originalFile.getString(extraKey));
					}
				}

				while (keys.hasNext()) {
					String key = (String) keys.next();
					eachResult.put(key, eachRow.getString(key));
				}
				cvFiles.add(eachResult);
			}
			return cvFiles;

		} catch (JSONException e) {
			Log.e("getDataFromJSON", "JSONException: " + e.getMessage());
		}

		return new ArrayList<>();
	}


	/**
	 * This function takes a json file, which is a serialized (JSON format), convert
	 * it to JSON, and then return a list of ContentValues, which will be used to
	 * update into the database.
	 * The JSON data has the form of
	 * {"item": [json1, json2...], "extraKey1": "value1", "extraKey2": "value2"}
	 * @param jsonObject    the json that holds data
	 * @param extraKeys     these keys hold values that all items in "item" contains
	 *                      (rather store "value1", "value2",... in each of the items
	 *                      in "items", store these values separately which has
	 *                      "extraKey1", "extraKey2",... points to, so that it will
	 *                      takes less time to download data and it will result in
	 *                      a lower data transfer)
	 * @return              a vector of ContentValues. Each ContentValues is each
	 *                      row that can be stored in the database
	 */
	public static ArrayList<ContentValues> getDataFromJSON(JSONObject jsonObject,
	                                                       @Nullable String[] extraKeys)
			throws JSONException {

		try {

			JSONArray jsonFile = jsonObject.getJSONArray(AppConstants.SERVER_RESPONSE.RESULT);

			ArrayList<ContentValues> cvFiles = new ArrayList<>(jsonFile.length());

			if (jsonFile.length() == 0) {
				return new ArrayList<>();
			}

			for (int i = 0; i < jsonFile.length(); i++) {

				JSONObject eachRow = jsonFile.getJSONObject(i);
				Iterator<?> keys = eachRow.keys();
				ContentValues eachResult = new ContentValues();

				// Get the value from extraKeys (outside of "item")
				if ((extraKeys != null) && (extraKeys.length > 0)) {
					for (String extraKey: extraKeys) {
						eachResult.put(extraKey, jsonObject.getString(extraKey));
					}
				}

				while (keys.hasNext()) {
					String key = (String) keys.next();
					eachResult.put(key, eachRow.getString(key));
				}
				cvFiles.add(eachResult);
			}
			return cvFiles;

		} catch (JSONException e) {
			Log.e("getDataFromJSON", "JSONException: " + e.getMessage());
		}

		return new ArrayList<>();
	}

	public interface DataUpdatedListener {
		void onDataUpdated();
	}


	/**
	 * This class handles building the URL that will send to server.
	 */
	public static class BuildURL {


		/**
		 * Create url to sync user tagged business on server database. URL has the form:
		 * https://mercury.com/tag?userID=<userID>&extra=<extra>
		 * @param userId    the user ID to query
		 * @param extra     extra is a number to help server fast check whether this is
		 *                  the most up-to-date database (if the extra field contains
		 *                  the number equal to the number on the server, then it is
		 *                  up-to-date)
		 * @return          return uri that can be send from phone to server
		 */
		public static Uri tagSync(String userId, int extra) {

			return Uri.parse(URL_CONSTANTS.SERVER_NAME)
					.buildUpon()
					.appendPath(URL_CONSTANTS.TAG)
					.appendQueryParameter(URL_CONSTANTS.USER_ID, userId)
					.appendQueryParameter(URL_CONSTANTS.EXTRA, String.valueOf(extra))
					.build();
		}

		/**
		 * Create url to update the surrounding events and deals. URL has the form:
		 * https://mercury.com/around?userID=<userID>&type=<type>&lat=<lat>&long=<long>
		 *     &refresh=<refresh>
		 * @param userId    the id of user that will query server
		 * @param type      the type of around update (deal or event)
		 * @param lat       the current latitude of user
		 * @param lon       the current longitude of user
		 * @param refresh   whether user wants to refresh data, 1=refresh, 0=no_refresh
		 * @param page      the page used to mark where the data is in infinite scroll
		 * @return          the correct URL to query to server
		 */
		public static Uri aroundSync(@Nullable String userId, String type,
		                             double lat, double lon, int refresh, int page) {

			if ((refresh != 0) && (refresh != 1)) {
				throw new IllegalArgumentException("refresh argument can only be either 1 or 0, " +
						"currently " + refresh);
			}
			Uri result;

			if (userId == null) {
				result = Uri.parse(URL_CONSTANTS.SERVER_NAME)
						.buildUpon()
						.appendPath(URL_CONSTANTS.AROUND)
						.appendQueryParameter(URL_CONSTANTS.TYPE, type)
						.appendQueryParameter(URL_CONSTANTS.LAT, String.valueOf(lat))
						.appendQueryParameter(URL_CONSTANTS.LON, String.valueOf(lon))
						.appendQueryParameter(URL_CONSTANTS.REFRESH, String.valueOf(refresh))
						.appendQueryParameter(URL_CONSTANTS.EXTRA, String.valueOf(page))
						.build();
			} else {
				result = Uri.parse(URL_CONSTANTS.SERVER_NAME)
						.buildUpon()
						.appendPath(URL_CONSTANTS.AROUND)
						.appendQueryParameter(URL_CONSTANTS.USER_ID, userId)
						.appendQueryParameter(URL_CONSTANTS.TYPE, type)
						.appendQueryParameter(URL_CONSTANTS.LAT, String.valueOf(lat))
						.appendQueryParameter(URL_CONSTANTS.LON, String.valueOf(lon))
						.appendQueryParameter(URL_CONSTANTS.REFRESH, String.valueOf(refresh))
						.appendQueryParameter(URL_CONSTANTS.EXTRA, String.valueOf(page))
						.build();
			}

			return result;
		}

		/**
		 * Create url to update the surrounding events and deals. URL has the form:
		 * https://mercury.com/around?userID=<userID>&type=<type>&refresh=<refresh>&extra=<page>
		 * @param userId    the id of user that will query server
		 * @param type      the type of around update (deal or event)
		 * @param refresh   whether user wants to refresh data, 1=refresh, 0=no_refresh
		 * @param page      the page used in infinite scroll
		 * @return          the correct URL to query to server
		 */
		public static Uri aroundSync(@Nullable String userId, String type,
		                             int refresh, int page) {

			if (!((refresh == 0) || (refresh == 1))) {
				throw new IllegalArgumentException("refresh argument can only be either 1 or 0, " +
						"currently " + refresh);
			}
			Uri result;

			if (userId == null) {
				result = Uri.parse(URL_CONSTANTS.SERVER_NAME)
						.buildUpon()
						.appendPath(URL_CONSTANTS.AROUND)
						.appendQueryParameter(URL_CONSTANTS.TYPE, type)
						.appendQueryParameter(URL_CONSTANTS.REFRESH, String.valueOf(refresh))
						.appendQueryParameter(URL_CONSTANTS.EXTRA, String.valueOf(page))
						.build();
			} else {
				result = Uri.parse(URL_CONSTANTS.SERVER_NAME)
						.buildUpon()
						.appendPath(URL_CONSTANTS.AROUND)
						.appendQueryParameter(URL_CONSTANTS.USER_ID, userId)
						.appendQueryParameter(URL_CONSTANTS.TYPE, type)
						.appendQueryParameter(URL_CONSTANTS.REFRESH, String.valueOf(refresh))
						.appendQueryParameter(URL_CONSTANTS.EXTRA, String.valueOf(page))
						.build();
			}

			return result;
		}

		/**
		 * To fetch business drawer ids to construct appropriate drawer view
		 * @param busId     the id of interested business
		 * @return          the uri needed to query drawer information
		 */
		public static Uri busDrawerSync(String busId) {

			return Uri.parse(URL_CONSTANTS.SERVER_NAME)
					.buildUpon()
					.appendPath(URL_CONSTANTS.BUS_DRAWER)
					.appendQueryParameter(URL_CONSTANTS.BUS_ID, busId)
					.build();
		}

		/**
		 * To fetch the number of events to construct appropriate info activity. Example link:
		 * https://mercury.com/numEvent?busID=<busID>
		 * @param busId     the id of interested business
		 * @return          the uri needed to query number of events
		 */
		public static Uri busNumEventSync(String busId) {
			return Uri.parse(URL_CONSTANTS.SERVER_NAME)
					.buildUpon()
					.appendPath(URL_CONSTANTS.BUS_NUM_EVENTS)
					.appendQueryParameter(URL_CONSTANTS.BUS_ID, busId)
					.build();
		}

		/**
		 * To fetch the general business information. Example link:
		 * https://mercury.com/busInfo?busID=<busID>
		 * @param busId     the id of business that we want to query general information
		 * @return          the link to query from server
		 */
		public static Uri busInfoSync(String busId) {
			return Uri.parse(URL_CONSTANTS.SERVER_NAME)
					.buildUpon()
					.appendPath(URL_CONSTANTS.BUS_INFO)
					.appendQueryParameter(URL_CONSTANTS.BUS_ID, busId)
					.build();
		}

		/**
		 * To fetch other business information to show in business general page (ex: favourites,
		 * customer tips, today specials...). This url is just slightly different from
		 * build info  sync url (in that this one has extra=1 parameter)
		 * https://mercury.com/busInfo?busID=<busID>&extra=1
		 * @param busId     the id of business we want to query other information
		 * @return
		 */
		public static Uri busInfoOtherSync(String busId) {
			return Uri.parse(URL_CONSTANTS.SERVER_NAME)
					.buildUpon()
					.appendPath(URL_CONSTANTS.BUS_INFO)
					.appendQueryParameter(URL_CONSTANTS.BUS_ID, busId)
					.appendQueryParameter(URL_CONSTANTS.EXTRA, String.valueOf(1))
					.build();
		}

	}
}
