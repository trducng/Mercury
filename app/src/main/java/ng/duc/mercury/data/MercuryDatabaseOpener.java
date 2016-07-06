package ng.duc.mercury.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ng.duc.mercury.data.DataContract.tagEntry;

/**
 * Created by ducprogram on 6/19/16.
 * This class helps organize database when database first
 * opens, specifically:
 * 1. Create new database when none exists
 * 2. Update database when new version of database exists
 *    (this is important for seamless update between app
 *    versions)
 */
public class MercuryDatabaseOpener extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	static final String DATABASE_NAME = "mercury.db";

	public MercuryDatabaseOpener(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		// TODO: <debug> delete this
		db.execSQL("DROP TABLE IF EXISTS " + DataContract.TAG_BUS);
		// ===============

		// Create each table with the appropriate columns
		// defined in Data contract

		final String SQL_CREATE_TAG_TABLE = "CREATE TABLE "
				+ DataContract.TAG_BUS + " ("
				+ tagEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ tagEntry.COL_TAG + " TEXT NOT NULL, "
				+ tagEntry.COL_BUSID + " TEXT NOT NULL, "
				+ tagEntry.COL_NAME + " TEXT NOT NULL, "
				+ tagEntry.COL_CAT + " TEXT NOT NULL, "
				+ tagEntry.COL_COST + " INT NOT NULL, "
				+ tagEntry.COL_POP +  " REAL NOT NULL, "
				+ tagEntry.COL_POSIT + " INT NOT NULL, "
				+ tagEntry.COL_LOC + " TEXT NOT NULL, "
				+ tagEntry.COL_SERVS + " INT NOT NULL, "
				+ tagEntry.COL_CIMG + " TEXT NOT NULL, "
				+ tagEntry.COL_LAT + " REAL NOT NULL, "
				+ tagEntry.COL_LONG + " REAL NOT NULL);";

		db.execSQL(SQL_CREATE_TAG_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		db.execSQL("DROP TABLE IF EXISTS " + DataContract.TAG_BUS);

		onCreate(db);
	}
}