package ng.duc.mercury.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;

import ng.duc.mercury.AppConstants.SERVER_RESPONSE;

/**
 * Created by ducprogram on 6/19/16.
 * This class defines the contract between the application
 * and backend data resources, so that every component in
 * the application will have a consistent interface to interact
 * with the backend data.
 * More specifically, this DataContract class defines the field
 * and other relevant information for each of the table used
 * in the application.
 */
public class DataContract {

	// Define the package name and base Uri, on which other Uris
	// will be built upon
	public static final String PACKAGE_NAME = "ng.duc.mercury";
	public static final Uri BASE_URI = Uri.parse("content://" + PACKAGE_NAME);

	// Define table names
	public static final String TAG_BUS = "tag";
	public static final String RECOMMENDATION_BUS = "recom";
	public static final String EVENT = "events";
	public static final String SEARCH = "search";

	public static final class tagEntry implements BaseColumns {

		// Unique Uri for this table
		public static final Uri CONTENT_URI =
				BASE_URI.buildUpon().appendPath(TAG_BUS).build();


		// Unique MIME type for data from this table
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
				+ PACKAGE_NAME + "/" + TAG_BUS;
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
				+ PACKAGE_NAME + "/" + TAG_BUS;

		// TODO: image needs not be a square. For now settle with crop center

		// Constants for each columns
		public static final String COL_TAG = SERVER_RESPONSE.TAG;
		public static final String COL_BUSID = SERVER_RESPONSE.BUS_ID;
		public static final String COL_NAME = SERVER_RESPONSE.BUS_NAME;
		public static final String COL_CAT = SERVER_RESPONSE.BUS_CAT;
		public static final String COL_LOC = SERVER_RESPONSE.BUS_LOC;
		public static final String COL_COST = SERVER_RESPONSE.BUS_COST;
		public static final String COL_POP = SERVER_RESPONSE.BUS_POP;
		public static final String COL_POSIT = SERVER_RESPONSE.BUS_POSITIVE;
		public static final String COL_SERVS = SERVER_RESPONSE.BUS_SER;
		public static final String COL_CIMG = SERVER_RESPONSE.BUS_COVER_IMG;
		public static final String COL_LAT = SERVER_RESPONSE.LAT;
		public static final String COL_LONG = SERVER_RESPONSE.LONG;

		// All of the above, except COL_TAGS and COL_TIMEADDED
		public static final String[] PROJECTION = new String[] {
				_ID, COL_BUSID, COL_NAME, COL_CAT, COL_COST,
				COL_POP, COL_POSIT, COL_LOC, COL_SERVS, COL_CIMG, COL_LAT,
				COL_LONG, COL_TAG
		};

		public static final int ID_IDX = 0;
		public static final int BUSID_IDX = 1;
		public static final int NAME_IDX = 2;
		public static final int CAT_IDX = 3;
		public static final int COST_IDX = 4;
		public static final int POP_IDX = 5;
		public static final int POSIT_IDX = 6;
		public static final int LOC_IDX = 7;
		public static final int SERVS_IDX = 8;
		public static final int CIMG_IDX = 9;
		public static final int LAT_IDX = 10;
		public static final int LONG_IDX = 11;
		public static final int TAG_IDX = 12;



		public static final String selectTags =
				"WHERE " + COL_TAG + " = ?";

		public static final String selectTagAndId =
				COL_TAG + " = ? AND " + COL_BUSID + " = ?";

		public static Uri buildGeneralTag() {

			// URI to call for list of all tag lists
			// content://ng.duc.mercury/tag
			return CONTENT_URI;
		}

		public static Uri buildSpecificTags(String[] tagName) {

			// URI to call for a specific tag list
			// content://ng.duc.mercury/tag/<tagName>

			String allTags;
			if (tagName.length != 0) {
				allTags = TextUtils.join("$._.", tagName);
			} else {
				// since "all" is an invalid tag name, we use "all" as
				// a special tag to notify SQL to return nothing. Yes,
				// it is intuitive, but it is more efficient and actually
				// makes the whole system less complicated.
				// So content://ng.duc.mercury/tag/all will return empty cursor
				allTags = "all";
			}

			return CONTENT_URI.buildUpon().appendPath(allTags).build();
		}

		public static Uri buildSpecificTags(HashSet<String> tagName) {

			// URI to call for a specific tag list
			// content://ng.duc.mercury/tag/<tagName>

			String[] tagNames = tagName.toArray(new String[tagName.size()]);
			return buildSpecificTags(tagNames);
		}

		public static Uri buildSingleTag(String tagName, String busID) {

			// URI to insert an entry to the Tag table
			// content://ng.duc.mercury/tag/<tagName>/<busID>

			return CONTENT_URI.buildUpon().appendPath(tagName)
					.appendPath(busID).build();
		}

		/**
		 * Build the query to retrieve items from SQLitedatabase
		 * Based on the number of tags (allTags.length), construct
		 * the query accordingly. Example: allTags = {"tag1", "tag2"}
		 * then the outcome will be "TAG = ? OR TAG = ? "
		 * @param allTags   all the tags that will be queried
		 * @return
		 */
		public static String buildConditionalQuery(String[] allTags) {

			String baseString = COL_TAG + " LIKE ?";

			if (allTags.length == 1) {
				return baseString;
			} else {
				String finalString = baseString;
				for (int dummy_idx = 0;
				     dummy_idx < allTags.length - 1;
				     dummy_idx++) {

					finalString += " OR ";
					finalString += baseString;
				}
				return finalString;
			}
		}

		/**
		 * This function returns the list given a bookmark URI
		 * content://com.ducnguyen.duo/tag/<allTags>
		 * will return reformatted <allTags>>
		 * @param uri   the Uri that contain the tag names
		 * @return
		 */
		public static String[] getTagNames(Uri uri) {

			String[] segments = uri.getEncodedPath().split("/");
			String stringTags = segments[segments.length - 1];
			try {
				stringTags = URLDecoder.decode(stringTags, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			segments = stringTags.split("\\$._.");

			for (int index=0; index<segments.length; index++) {
				segments[index] = "%" + formatTag(segments[index]) + "%";
			}

			return segments;
		}

		/**
		 * Takes an uri that is created by buildSingleBookmark(tagName, busID),
		 * which will looks like content://com.ducnguyen.duo/tag/<tagName>/<busID>
		 * and returns both tagName and busID.
		 *
		 * @param   uri     the URI created by buildSingleBookmark
		 * @return          {tagName, busID}
		 */
		public static String[] getTagNameAndBusID(Uri uri) {

			String[] segments = uri.getEncodedPath().split("/");
			String rawStringTag = segments[segments.length - 2];
			try {
				rawStringTag = URLDecoder.decode(rawStringTag, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return new String[] {   rawStringTag,
					segments[segments.length-1] };
		}

		/**
		 * Format tag to ||tag||
		 * @param tag   the tag to format
		 * @return      the secured tag
		 */
		public static String formatTag(String tag) {
			return "||" + tag + "||";
		}

		/**
		 * Turn ||tag|| to tag
		 * @param tag   the secured tag
		 * @return      the normal tag
		 */
		public static String deformatTag(String tag) {
			return tag.substring(2, tag.length() - 2);
		}
	}

	public static final class recBusEntry implements BaseColumns {

		public static final Uri CONTENT_URI =
				BASE_URI.buildUpon().appendPath(RECOMMENDATION_BUS).build();

		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
				+ PACKAGE_NAME + "/" + RECOMMENDATION_BUS;
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
				+ PACKAGE_NAME + "/" + RECOMMENDATION_BUS;

		public static final String COL_BUSID = SERVER_RESPONSE.BUS_ID;
		public static final String COL_NAME = SERVER_RESPONSE.BUS_NAME;
		public static final String COL_LOC = SERVER_RESPONSE.BUS_LOC;
		public static final String COL_COST = SERVER_RESPONSE.BUS_COST;
		public static final String COL_POP = SERVER_RESPONSE.BUS_POP;
		public static final String COL_CAT = SERVER_RESPONSE.BUS_CAT;
		public static final String COL_SERVS = SERVER_RESPONSE.BUS_SER;
		public static final String COL_CIMG = SERVER_RESPONSE.BUS_COVER_IMG;
		public static final String COL_DISTANCE = SERVER_RESPONSE.BUS_DISTANCE;

		public static Uri buildURI() {

			// Return a URI to return all recommendation results
			// content://ng.duc.mercury/recom
			return CONTENT_URI;
		}
	}

	public static final class searchEntry implements BaseColumns {

		public static final Uri CONTENT_URI =
				BASE_URI.buildUpon().appendPath(SEARCH).build();

		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
				+ PACKAGE_NAME + "/" + SEARCH;
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
				+ PACKAGE_NAME + "/" + SEARCH;

		public static final String COL_BUSID = SERVER_RESPONSE.BUS_ID;
		public static final String COL_NAME = SERVER_RESPONSE.BUS_NAME;
		public static final String COL_LOC = SERVER_RESPONSE.BUS_LOC;
		public static final String COL_SERVS = SERVER_RESPONSE.BUS_SER;
		public static final String COL_CIMG = SERVER_RESPONSE.BUS_COVER_IMG;
		public static final String COL_DISTANCE = SERVER_RESPONSE.BUS_DISTANCE;

		public static Uri buildURI() {

			// Return a URI to return all recommendation results
			// content://ng.duc.mercury/search
			return CONTENT_URI;
		}

	}
}
