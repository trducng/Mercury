package ng.duc.mercury.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ducprogram on 6/21/16.
 * This method tests the robustness of data provider.
 */
@RunWith(AndroidJUnit4.class)
public class DataProviderAndroidTest extends ProviderTestCase2 {

	private static final String LOG_TAG = DataProviderAndroidTest.class.getSimpleName();
	Context mContext;


	public DataProviderAndroidTest() {
		super(MercuryDataProvider.class, DataContract.BASE_URI.toString());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setContext(InstrumentationRegistry.getTargetContext());
	}

	@Override
	public ContentProvider getProvider() {
		return new MercuryDataProvider();
	}

	private int randInt(int min, int max) {
		Random rand = new Random();
		return rand.nextInt((max - min) + 1) + min;
	}

	private ArrayList<ContentValues> createFakeData(int numData,
                                        HashMap<String, String> cols) {
		try {
			setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] testString = new String[] {
				"tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7",
				"tag8", "tag9", "tag0"
		};
		ArrayList<ContentValues> fakeData = new ArrayList<>();
		ContentValues data;
		String type;
		ArrayList<String> tableCols = new ArrayList<>(cols.keySet());
		while (numData > 0) {
			numData--;
			data = new ContentValues();

			for (String col: tableCols) {
				type = cols.get(col);
				switch (type) {
					case "string":
						data.put(col, testString[randInt(0, 9)]);
						break;
					case "int":
						data.put(col, randInt(0, 10000));
						break;
					case "real":
						data.put(col, new Random().nextFloat() * 100);
						break;
					default:
						throw new UnsupportedOperationException("Unknown type: " + type);
				}
			}
			fakeData.add(data);
		}
		return fakeData;
	}

	@Test
	public void testCRUDOperation() {

		// Catch away, the default getContext() in content provider will return null
		// hence cannot create an appropriate database opener. That's why we have to
		// create new test methods in Data provider that will manually receive context
		// from this test class to create database opener.
		// TODO: comment out test methods in MercuryDataProvider after completing test

		mContext = InstrumentationRegistry.getTargetContext();
		new MercuryDataProvider().deleteTest(
				mContext, DataContract.tagEntry.buildGeneralTag(), null, null);

		HashMap<String, String> tagCols = new HashMap<>();
		tagCols.put(DataContract.tagEntry.COL_TAG, "string");
		tagCols.put(DataContract.tagEntry.COL_BUSID, "string");
		tagCols.put(DataContract.tagEntry.COL_NAME, "string");
		tagCols.put(DataContract.tagEntry.COL_CAT, "string");
		tagCols.put(DataContract.tagEntry.COL_COST, "real");
		tagCols.put(DataContract.tagEntry.COL_POP, "string");
		tagCols.put(DataContract.tagEntry.COL_LOC, "string");
		tagCols.put(DataContract.tagEntry.COL_SERVS, "int");
		tagCols.put(DataContract.tagEntry.COL_CIMG, "string");
		tagCols.put(DataContract.tagEntry.COL_LAT, "real");
		tagCols.put(DataContract.tagEntry.COL_LONG, "real");

		int numData = 10;
		ArrayList<ContentValues> fakeData = createFakeData(numData, tagCols);
		ContentValues[] fakeDataArray = new ContentValues[fakeData.size()];
		fakeData.toArray(fakeDataArray);

		int inserted = new MercuryDataProvider()
				.bulkInsertTest(mContext, DataContract.tagEntry.buildGeneralTag(),fakeDataArray);

		Log.v(LOG_TAG, "Insertion completes: " + inserted);
		assertThat("Number of data inserted is different from the number of data created",
				inserted, is(fakeDataArray.length));
		Cursor c = new MercuryDataProvider().queryTest(mContext,
				DataContract.tagEntry.buildGeneralTag(), null, null, null, null);

		assertThat("Number of database is different from the number" +
				"of data inserted", c.getCount(), is(numData));

		new MercuryDataProvider().deleteTest(mContext,
				DataContract.tagEntry.buildGeneralTag(), null, null);
		c = new MercuryDataProvider().queryTest(mContext,
				DataContract.tagEntry.buildGeneralTag(), null, null, null, null);
		assertThat("number of dataset is not empty after complete delete",
				c.getCount(), is(0));
	}
}
