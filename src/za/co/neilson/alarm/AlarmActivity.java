package za.co.neilson.alarm;

import za.co.neilson.alarm.database.Database;
import za.co.neilson.alarm.preferences.AlarmPreferencesActivity;
import za.co.neilson.alarm.service.AlarmServiceBroadcastReciever;
import za.co.neilson.alarm.R;
import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class AlarmActivity extends ListActivity implements
		android.view.View.OnClickListener {

	ImageButton newButton;
	ListView mathAlarmListView;
	AlarmListAdapter alarmListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alarm_activity);

		newButton = (ImageButton) findViewById(za.co.neilson.alarm.R.id.button_new);
		newButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					newButton.setBackgroundColor(getResources().getColor(
							R.color.holo_blue_light));
					break;
				case MotionEvent.ACTION_UP:
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					Intent newAlarmIntent = new Intent(AlarmActivity.this,
							AlarmPreferencesActivity.class);
					startActivity(newAlarmIntent);
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_CANCEL:
					newButton.setBackgroundColor(getResources().getColor(
							android.R.color.transparent));
					break;
				}
				return true;
			}
		});
		
		mathAlarmListView = (ListView) findViewById(android.R.id.list);

		mathAlarmListView.setLongClickable(true);
		mathAlarmListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> adapterView,
							View view, int position, long id) {
						view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
						final Alarm alarm = (Alarm) alarmListAdapter
								.getItem(position);
						Builder dialog = new AlertDialog.Builder(
								AlarmActivity.this);
						dialog.setTitle("Delete");
						dialog.setMessage("Delete this alarm?");
						dialog.setPositiveButton("Ok", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								alarmListAdapter.getMathAlarms().remove(
										alarm);
								alarmListAdapter.notifyDataSetChanged();

								Database.init(AlarmActivity.this);
								Database.deleteEntry(alarm);

								AlarmActivity.this
										.callMathAlarmScheduleService();
							}
						});
						dialog.show();
						alarmListAdapter.getItem(position);
						return true;
					}
				});

		callMathAlarmScheduleService();
	}

	private void callMathAlarmScheduleService() {
		Intent mathAlarmServiceIntent = new Intent(AlarmActivity.this,
				AlarmServiceBroadcastReciever.class);
		sendBroadcast(mathAlarmServiceIntent, null);
	}

	@Override
	protected void onPause() {
		// setListAdapter(null);
		Database.deactivate();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		@SuppressWarnings("deprecation")
		final Object data = getLastNonConfigurationInstance();
		if (data == null) {
			alarmListAdapter = new AlarmListAdapter(this);
		} else {
			alarmListAdapter = (AlarmListAdapter) data;
		}

		this.setListAdapter(alarmListAdapter);

	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return alarmListAdapter;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		Alarm alarm = (Alarm) alarmListAdapter
				.getItem(position);
		Intent intent = new Intent(AlarmActivity.this,
				AlarmPreferencesActivity.class);
		intent.putExtra("alarm", alarm);
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.checkBox_alarm_active) {
			CheckBox checkBox = (CheckBox) v;
			Alarm alarm = (Alarm) alarmListAdapter
					.getItem((Integer) checkBox.getTag());
			alarm.setAlarmActive(checkBox.isChecked());
			Database.update(alarm);
			AlarmActivity.this.callMathAlarmScheduleService();
			Toast.makeText(AlarmActivity.this,
					alarm.getTimeUntilNextAlarmMessage(), Toast.LENGTH_LONG)
					.show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_rate:
			Uri uri = Uri.parse("market://details?id="
					+ getPackageName());
			Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
			try {
				startActivity(goToMarket);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, "Couldn't launch the market",
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.menu_item_website:
			String url = "http://www.neilson.co.za";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
			break;
		case R.id.menu_item_report:
			Intent send = new Intent(Intent.ACTION_SENDTO);
			String uriText;

			String emailAddress = "bugs@neilson.co.za";
			String subject = R.string.app_name + " Bug Report";
			String body = "Debug:";
			body += "\n OS Version: " + System.getProperty("os.version") + "("
					+ android.os.Build.VERSION.INCREMENTAL + ")";
			body += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
			body += "\n Device: " + android.os.Build.DEVICE;
			body += "\n Model (and Product): " + android.os.Build.MODEL + " ("
					+ android.os.Build.PRODUCT + ")";
			body += "\n Screen Width: "
					+ getWindow().getWindowManager().getDefaultDisplay()
							.getWidth();
			body += "\n Screen Height: "
					+ getWindow().getWindowManager().getDefaultDisplay()
							.getHeight();
			body += "\n Hardware Keyboard Present: "
					+ (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS);

			uriText = "mailto:" + emailAddress + "?subject=" + subject
					+ "&body=" + body;

			uriText = uriText.replace(" ", "%20");
			Uri emalUri = Uri.parse(uriText);

			send.setData(emalUri);
			startActivity(Intent.createChooser(send, "Send mail..."));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
