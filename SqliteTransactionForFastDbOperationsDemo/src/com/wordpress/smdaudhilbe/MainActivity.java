package com.wordpress.smdaudhilbe;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.wordpress.smdaudhilbe.sqlitetransactiondemo.R;

//	tutorial : https://www.codeofaninja.com/2013/12/android-sqlite-transaction-tutorial.html

public class MainActivity extends Activity {

	private EditText eText;
	private TextView txtView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initViews();
	}

	private void initViews() {
		eText = (EditText) findViewById(R.id.editText1);
		txtView = (TextView) findViewById(R.id.textView1);
	}

	@fromXML
	public void fastLoad(View view) {
		new MyAsyncTask().execute(getString(R.string.fastButton));
	}

	@fromXML
	public void normalLoad(View view) {
		new MyAsyncTask().execute(getString(R.string.slowButton));
	}

	// asyncTask
	class MyAsyncTask extends AsyncTask<String, Void, Void> {

		private long startTime;
		private DBHelper dbHelper;

		public MyAsyncTask() {
			dbHelper = new DBHelper(getApplicationContext());
		}

		@Override
		protected Void doInBackground(String... params) {

			try {
				// get number of records to be inserted
				int insertCount = Integer.parseInt(eText.getText().toString());

				dbHelper.deleteRecords();

				startTime = System.nanoTime();

				if (params[0].equals(getString(R.string.fastButton))) {
					dbHelper.insertFast(insertCount);
				} else if (params[0].equals(getString(R.string.slowButton))) {
					dbHelper.insertNormal(insertCount);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			long timeElapsed = System.nanoTime() - startTime;
			txtView.setText("It took " + timeElapsed / 1000000 + " ms to insert " + dbHelper.countRecords() + " entries!");
		}
	}

	// db helper
	class DBHelper extends SQLiteOpenHelper {

		// database version
		private static final int DATABASE_VERSION = 1;
		String tableName = "myTABLE";
		String id = "myId";
		String name = "myName";
		String description = "myDescription";
		String index = "myIndex";

		// database name
		protected static final String DATABASE_NAME = "myDB";

		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// creating table
			db.execSQL("create table " + tableName + " (" + id
					+ " integer primary key autoincrement," + name + " text,"
					+ description + " text);");

			// creating index
			db.execSQL("create unique index " + index + " on " + tableName
					+ " (" + name + "," + description + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("drop table if exists " + tableName);
			onCreate(db);
		}

		// deletes all records
		public void deleteRecords() {
			SQLiteDatabase db = this.getWritableDatabase();
			db.execSQL("delete from " + tableName);
			db.close();
		}

		// fastInserting
		public void insertFast(int count) {
			String sqlQuery = "insert or replace into " + tableName + "("
					+ name + "," + description + ") values (?,?)";

			SQLiteDatabase db = this.getWritableDatabase();

			try {

				/*
				 * According to the docs
				 * http://developer.android.com/reference/android
				 * /database/sqlite/SQLiteDatabase.html Writers should use
				 * beginTransactionNonExclusive() or
				 * beginTransactionWithListenerNonExclusive
				 * (SQLiteTransactionListener) to start a transaction.
				 * Non-exclusive mode allows database file to be in readable by
				 * other threads executing queries.
				 */
				db.beginTransactionNonExclusive();

				SQLiteStatement stateMent = db.compileStatement(sqlQuery);

				for (int x = 1; x <= count; x++) {
					stateMent.bindString(1, "Name # " + x);
					stateMent.bindString(2, "Description # " + x);

					stateMent.execute();
					stateMent.clearBindings();
				}
				db.setTransactionSuccessful();
			} catch (Exception e) {
				// catching
				e.printStackTrace();
			} finally {
				db.endTransaction();
				db.close();
			}
		}

		// slowInserting
		public void insertNormal(int count) {
			SQLiteDatabase db = this.getWritableDatabase();
			try {
				for (int x = 1; x <= count; x++) {
					ContentValues values = new ContentValues();
					values.put(name, "Name # " + x);
					values.put(description, "Description # " + x);

					db.insert(tableName, null, values);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.close();
			}
		}

		// count records
		public int countRecords() {

			SQLiteDatabase db = this.getWritableDatabase();

			Cursor cursor = db.rawQuery("SELECT count(*) from " + tableName,null);
			cursor.moveToFirst();

			int recCount = cursor.getInt(0);

			cursor.close();
			db.close();

			return recCount;
		}
	}
}