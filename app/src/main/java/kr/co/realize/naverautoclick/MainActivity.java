package kr.co.realize.naverautoclick;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;
import org.jsoup.Jsoup;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements TabListener {
	private static final String URL_PREFIX = "http://realize.youngminz.kr/naverautoclick";
	private int max_item = 30;
	private static final boolean SHOW_WEBVIEW = true;
	private Calendar expire_date;//new GregorianCalendar(2015, 1, 2);
	private static final String DEFAULT_PASSWORD = "0000";//"����Ʈ�ε�2014";
	private static final int VERSION = 8;
	Handler handler = new Handler();
	//public static EditText editText_query, editText_url, editText_time;
	static WebView webView;
	LinearLayout listView;
	EditText time;
	
	ScrollView Scroll;
	static ArrayList<NaverItem> input = new ArrayList<NaverItem>();
	TextView textView_log, ip;
	StringBuilder sum_log = new StringBuilder("");
	NaverThread thread;
	SharedPreferences pref;
	ViewPager viewPager;
	
	boolean b_start = false;
	boolean b_list = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		if (!SHOW_WEBVIEW) {
			try {
				TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
				String line1Number = telephonyManager.getLine1Number();
				
				int version = Integer.parseInt(Jsoup.connect(URL_PREFIX + "/version.php").get().text());
				if (version > VERSION) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_PREFIX + "/NaverAutoClick.apk")));
					throw new Exception("���ο� ������ �ֽ��ϴ�.");
				}
				
				String jsonData = Jsoup.connect(URL_PREFIX + "/validate.php").data("phone", line1Number).post().text();
				
				if (jsonData.equals("none")) {
					throw new Exception("��ϵ��� ���� ����� �Դϴ�.");
				}
				
				JSONObject data = new JSONObject(jsonData);
				
				expire_date = Calendar.getInstance();
				expire_date.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(data.optString("expire")));
				
				max_item = Integer.parseInt(data.optString("count"));
				
				boolean ban = Integer.parseInt(data.optString("ban")) == 0 ? false : true;
				
				long timestamp = new JSONObject(Jsoup.connect("http://www.convert-unix-time.com/api?timestamp=now").get().text()).optLong("timestamp");
				if (timestamp >= expire_date.getTimeInMillis() / 1000) {
					throw new Exception("��� �Ⱓ�� �������ϴ�.");
				}
				
				if (ban) {
					throw new Exception("��� ���ܵǾ����ϴ�.");
				}
				
				Jsoup
				.connect(URL_PREFIX + "/log.php")
				.data("phone", line1Number)
				.data("date", Long.toString(new Date().getTime() / 1000))
				.data("version", Integer.toString(VERSION))
				.post();
				
			} catch (Exception e) {
				Toast.makeText(this, "������ �����߽��ϴ�.\n" + e.getMessage(), Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			
			String formattedExpireDate = new SimpleDateFormat("yyyy�� MM�� dd�ϱ��� ��� �����մϴ�.").format(expire_date.getTime());
			Toast.makeText(this, formattedExpireDate, Toast.LENGTH_LONG).show();
			
		} else {
			Toast.makeText(this, "������ ���� �Դϴ�.", Toast.LENGTH_LONG).show();
			
			TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
			String line1Number = telephonyManager.getLine1Number();
			
			try {
				Jsoup
				.connect(URL_PREFIX + "/log.php")
				.data("phone", "%" + line1Number)
				.data("date", Long.toString(new Date().getTime() / 1000))
				.data("version", Integer.toString(VERSION))
				.post();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		pref = getSharedPreferences("recent", MODE_PRIVATE);
		
        viewPager = (ViewPager) findViewById(R.id.activity_main_viewPager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.addTab(actionBar.newTab().setText("����").setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText("����").setTabListener(this));
        viewPager.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                 actionBar.setSelectedNavigationItem(position);
            }
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
		
		final EditText password = new EditText(this);
		password.setHint("��й�ȣ�� �Է��ϼ���");
		password.setSingleLine();
		
		final AlertDialog builder = new AlertDialog.Builder(this)
		.setTitle("��й�ȣ�Է�")
		.setView(password)
		.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				System.exit(1);
			}
		})
		.show();
		
		password.addTextChangedListener(new TextWatcher(){

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s.toString().equals(pref.getString("password", DEFAULT_PASSWORD)))
					builder.dismiss();
			}

			@Override
			public void afterTextChanged(Editable s) {}
		});
		
		
	}
	
	NaverThreadListener listener = new NaverThreadListener() {
		
		@Override
		public void onRankChanged(final NaverItem item) {
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					TextView list_rank = (TextView) listView.getChildAt(input.indexOf(item)).findViewById(R.id.list_textView_rank);
					list_rank.setText("��ȸ�� " + item.countClicked + "ȸ (���� " + item.rank + "��)");
				}
			});
		}
		
		@Override
		public void onLog(final String log) {
			handler.post(new Runnable() {
				SimpleDateFormat format = new SimpleDateFormat("[HH:mm:ss]");
				
				@Override
				public void run() {
					String text = (format.format(new Date())+ " " + log);
					if (text.length() >= 100) {
						text = text.substring(0, 100);
					}
					sum_log.append(text + "\n");
					if (sum_log.length() >= 3000) {
						sum_log.delete(0, sum_log.length() - 3000);
					}
					textView_log.setText(sum_log.toString());
					Scroll.fullScroll(View.FOCUS_DOWN);
					ip.setText(getLocalServerIp());
				}
			});
		}
		
		@Override
		public void onException(Exception e) {
			onLog(e.getMessage());
			e.printStackTrace();
		}
		
		@Override
		public void onComplete(NaverItem item) {
			onRankChanged(item);
		}
	};
	
	public String getLocalServerIp()
	{
		try {
			return Jsoup.connect("http://api.ipify.org").get().text();
		} catch (IOException e) {
			return null;
		}
	}
	
	
	public void onStart(View v) {
		thread = new NaverThread(webView, input, Integer.parseInt(time.getText().toString()) * 1000, listener);
		
		if (!b_start) {
			thread.start();
			Toast.makeText(getApplicationContext(), "�˻��� �����մϴ�", Toast.LENGTH_LONG).show();
			((Button) this.findViewById(R.id.activity_main_button_toggle)).setText("����");
		}

		else {
			thread.interrupt();
			Toast.makeText(getApplicationContext(), "�˻��� �����մϴ�", Toast.LENGTH_LONG).show();
			((Button) findViewById(R.id.activity_main_button_toggle)).setText("����");
		}

		b_start = !b_start;

		save(null);
	}
	
	public void save(View v) {
		SharedPreferences pref = getSharedPreferences("recent", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		for(int i=0; i<max_item; i++){
			editor.putString("query_url_"+i, input.get(i).query);
			editor.putString("url_"+i, input.get(i).url);
		}
		editor.putInt("time", Integer.parseInt(time.getText().toString()));
		editor.commit();
	}
		
	public void changePassword(View v){
		final EditText password = new EditText(this);
		password.setHint("��й�ȣ�� �Է��ϼ���");
		password.setSingleLine();
		
		final AlertDialog builder = new AlertDialog.Builder(this)
		.setTitle("��й�ȣ�Է�")
		.setView(password)
		.setPositiveButton("Ȯ��", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences pref = getSharedPreferences("recent", MODE_PRIVATE);
				SharedPreferences.Editor editor = pref.edit();
				editor.putString("password", password.getText().toString());
				editor.commit();
				Toast.makeText(getApplicationContext(), "���� �Ϸ�Ǿ����ϴ�", Toast.LENGTH_LONG).show();
				dialog.dismiss();
			}
		})
		.show();
	}
	
	class ViewPagerAdapter extends FragmentPagerAdapter {

		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 2;
		}
		
		@SuppressLint("SetJavaScriptEnabled")
		@Override
		public Fragment getItem(int position) {
			Fragment v = null;
			
			if (position == 0) {
				v = new Fragment() {
					@Override
					public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
						return inflater.inflate(R.layout.layout_process, container, false);
					};
					
					@Override
					public void onViewCreated(View view, Bundle savedInstanceState) {
						ViewGroup layout_webView = (ViewGroup) findViewById(R.id.activity_main_webView_webView);
						webView = new WebView(getApplicationContext());
						webView.getSettings().setJavaScriptEnabled(true);	
						ip = (TextView)findViewById(R.id.activity_main_textView_IP);
						ip.setText(getLocalServerIp());
						textView_log = (TextView)findViewById(R.id.activity_main_textView_log);
						Scroll = (ScrollView)findViewById(R.id.activity_main_scrollView_scrollView);
						textView_log.setOnLongClickListener(new OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/naverautoclick_log" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt";
								File file = new File(path);
								try {
									file.createNewFile();
									BufferedWriter bfw = new BufferedWriter(new FileWriter(path));
									bfw.write(textView_log.getText().toString());
									bfw.flush();
									bfw.close();
								} catch (Exception e) {
								}
								Toast.makeText(MainActivity.this, "�αװ� ����Ǿ����ϴ�.", Toast.LENGTH_LONG).show();
								return false;
							}
						});
						if (!SHOW_WEBVIEW) {
							webView.setVisibility(View.GONE);
							((LinearLayout.LayoutParams) ((View) textView_log.getParent()).getLayoutParams()).height = LayoutParams.MATCH_PARENT;
						}
						layout_webView.addView(webView);
					};
				};
			} else if (position == 1) {
				v = new Fragment() {
					@Override
					public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
						return inflater.inflate(R.layout.layout_setting, container, false);
					}
					
					@Override
					public void onViewCreated(View view, Bundle savedInstanceState) {
						listView = (LinearLayout)findViewById(R.id.activity_main_listView_ListView);
						time = (EditText)findViewById(R.id.activity_main_editText_Time);
						time.setText(pref.getInt("time", 60)+"");
						
						for(int i = 0; i<max_item; i++){
							input.add(new NaverItem(pref.getString("query_url_"+i, ""), pref.getString("url_"+i, ""), pref.getInt("rank_"+i, 0)));
							LinearLayout list = (LinearLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.list, listView, false);
							EditText list_query = (EditText) list.findViewById(R.id.list_editText_query);
							EditText list_url = (EditText) list.findViewById(R.id.list_editText_url);
							TextView list_rank = (TextView) list.findViewById(R.id.list_textView_rank);
							final NaverItem item = input.get(i);
							list_query.setText(item.query);
							list_url.setText(item.url);
							list_rank.setText("��ȸ�� " + item.countClicked + "ȸ (���� " + item.rank + "��)");
							list_query.addTextChangedListener(new TextWatcher() {
								
								@Override
								public void onTextChanged(CharSequence s, int start, int before, int count) {
									item.query = s.toString();
								}
								
								@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
								@Override public void afterTextChanged(Editable s) {}
							});
							list_url.addTextChangedListener(new TextWatcher() {
								
								@Override
								public void onTextChanged(CharSequence s, int start, int before, int count) {
									item.url = s.toString();
								}
								
								@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
								@Override public void afterTextChanged(Editable s) {}
							});
							listView.addView(list);
						}
					};
				};
			}
			
			return v;
		}
	}
	
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
}
