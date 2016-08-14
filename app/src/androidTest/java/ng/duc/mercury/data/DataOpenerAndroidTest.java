package ng.duc.mercury.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ducnguyen on 6/21/16.
 * Test to create and upgrade table
 */
@RunWith(AndroidJUnit4.class)
public class DataOpenerAndroidTest extends InstrumentationTestCase {

	Context mContext;
	private static final String LOG_TAG = DataOpenerAndroidTest.class.getSimpleName();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());
		mContext = InstrumentationRegistry.getTargetContext();
		mContext.deleteDatabase(MercuryDatabaseOpener.DATABASE_NAME);
	}

	@Test
	public void testCreateTable() {

		final HashSet<String> realTableNames = new HashSet<>();
		realTableNames.add(DataContract.TAG_BUS);
		realTableNames.add(DataContract.AROUND);
		realTableNames.add(DataContract.BUS_INFO);

		try {
			setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mContext.deleteDatabase(MercuryDatabaseOpener.DATABASE_NAME);
		MercuryDatabaseOpener mOpener = new MercuryDatabaseOpener(mContext);
		SQLiteDatabase db = mOpener.getWritableDatabase();

		assertThat("Database is not opened", db.isOpen(), is(true));

		Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

		// Begin misc tests
		Cursor d = db.rawQuery("SELECT * FROM sqlite_master", null);
		String[] strings = d.getColumnNames();
		for (String string : strings) {
			Log.v(LOG_TAG, string);
		}
		int i = 0;
		if (d.moveToNext()) {
			i++;
			for (String string: strings) {
				Log.v(LOG_TAG, String.valueOf(i) + " " + string +
				" " + d.getString(d.getColumnIndex(string)));
			}
		}
		// End misc tests

		assertThat("Error: There isn't metadata relates to the tables, which means" +
				"the tables are not created correctly",
				c.moveToFirst(), is(true));

		do {
			realTableNames.remove(c.getString(0));
		} while (c.moveToNext());
		assertThat("Error: Your database was created without entry tables",
				realTableNames.isEmpty(), is(true));

		db.close();
	}
}
