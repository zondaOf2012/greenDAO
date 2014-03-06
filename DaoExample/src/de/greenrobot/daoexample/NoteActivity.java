/*
 * Copyright (C) 2011 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.greenrobot.daoexample;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import de.greenrobot.daoexample.DaoMaster.DevOpenHelper;

public class NoteActivity extends ListActivity {

	private SQLiteDatabase db;

	private EditText editText;

	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private NoteDao noteDao;

	private Cursor cursor;

	SimpleCursorAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "notes-db",
				null);

		db = helper.getWritableDatabase();

		daoMaster = new DaoMaster(db);

		daoSession = daoMaster.newSession();

		noteDao = daoSession.getNoteDao();

		String textColumn = NoteDao.Properties.Text.columnName;

		String orderBy = textColumn + " COLLATE LOCALIZED ASC";

		cursor = db.query(noteDao.getTablename(), noteDao.getAllColumns(),
				null, null, null, null, orderBy);

		String[] from = { textColumn, NoteDao.Properties.Comment.columnName };

		int[] to = { android.R.id.text1, android.R.id.text2 };

		// noteDao.queryBuilder().limit(5).build();

		// DELETE FROM table where _id NOT IN (SELECT _id from table ORDER BY
		// insertion_date DESC LIMIT 50)

		adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2, cursor, from, to);

		setListAdapter(adapter);

		editText = (EditText) findViewById(R.id.editTextNote);

		addUiListeners();
	}

	public void onMyLimitClick(View view) {

		test();
	}

	void test() {

		// int flag = db.delete(noteDao.getTablename(),
		// NoteDao.Properties.Id.columnName
		// + " NOT IN (SELECT _id from "
		// + noteDao.getTablename()
		// + " ORDER BY "
		// + NoteDao.Properties.Date.columnName
		// + " DESC LIMIT ?); ", new String[]{"5"});
		//
		//
		// Log.i("TAG", "flag : " + flag);

		// adapter.notifyDataSetChanged();

		// Log.i("TAG", " noteDao.loadAll(): " + noteDao.loadAll().toString());

		// sqlite 默认不区分大小写，可利用pragma语句修改，如下：为1则区分，为0则不区分
		// db.execSQL("PRAGMA case_sensitive_like = 1");

		QueryBuilder<Note> queryBuilder = null;

		List<Note> queryResult = null;

		// text以“A”开头的记录，并以时间降序排列
		queryBuilder = noteDao.queryBuilder();

		queryResult = queryBuilder.where(NoteDao.Properties.Text.like("A%"))
				.orderDesc(NoteDao.Properties.Date).list();

		Log.i("TAG", " one queryResult: " + queryResult.toString());

		queryBuilder = noteDao.queryBuilder();

		// 以text首字母降序排列
		queryResult = queryBuilder.orderRaw(
				NoteDao.Properties.Text.columnName + " COLLATE LOCALIZED DESC")
				.list();

		Log.i("TAG", " tow queryResult: " + queryResult.toString());

		// 仅记录最新5条数据, 列查询
		String queryCondition = NoteDao.Properties.Id.columnName
				+ " NOT IN (SELECT _id from " + noteDao.getTablename()
				+ " ORDER BY " + NoteDao.Properties.Date.columnName
				+ " DESC LIMIT 5)";

		noteDao.queryBuilder()
				.where(new WhereCondition.StringCondition(queryCondition))
				.buildDelete().executeDeleteWithoutDetachingEntities();

		Log.i("TAG", " three queryResult: " + noteDao.loadAll().toString());

		cursor.requery();
	}

	protected void addUiListeners() {
		editText.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					addNote();
					return true;
				}
				return false;
			}
		});

		final View button = findViewById(R.id.buttonAdd);
		button.setEnabled(false);
		editText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				boolean enable = s.length() != 0;
				button.setEnabled(enable);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	public void onMyButtonClick(View view) {
		addNote();
	}

	private void addNote() {
		String noteText = editText.getText().toString();
		editText.setText("");

		final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
				DateFormat.MEDIUM);
		String comment = "Added on " + df.format(new Date());
		Note note = new Note(null, noteText, comment, new Date());
		noteDao.insert(note);
		Log.d("DaoExample", "Inserted new note, ID: " + note.getId());

		cursor.requery();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		noteDao.deleteByKey(id);
		Log.d("DaoExample", "Deleted note, ID: " + id);
		cursor.requery();
	}

}