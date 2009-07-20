package com.threeaspen.android;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class MainActivity extends Activity {
	private static final Logger logger = Logger.getLogger("com.threeaspen.android.MainActivity");

    public static final int ID_BASE = 1000;
	public static final int EULA_ID = Menu.FIRST + ID_BASE + 1;
	public static final int REQUIRED_EULA_DIALOG = ID_BASE + 1;
	public static final int EULA_DIALOG = ID_BASE + 2;
	
	private Handler handler;
	private int versionCode;
	private String versionName;
	
	private int eulaTitle;
	private int eula;
	private int eulaYes;
	private int eulaNo;
	private int aboutTitle;
	
	public MainActivity() {
		super();
		this.handler = new Handler();
	}
	
	public void setEula(int eulaTitle, int eula, int eulaYes, int eulaNo) {
		this.eulaTitle = eulaTitle;
		this.eula = eula;
		this.eulaYes = eulaYes;
		this.eulaNo = eulaNo;
	}

	public void setAboutTitle(int aboutTitle) {
		this.aboutTitle = aboutTitle;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        storeVersion(); 
        super.onCreate(savedInstanceState);
	}

	public int getVersionCode() {
		return versionCode;
	}

	public String getVersionName() {
		return versionName;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == EULA_DIALOG || id == REQUIRED_EULA_DIALOG) {
			WebView wv = new WebView(this);
			wv.loadData(readEULA(), "text/html", "utf-8");
			AlertDialog.Builder eula = new AlertDialog.Builder(this)
			.setView(wv)
			.setTitle(eulaTitle)
			.setIcon(android.R.drawable.ic_dialog_info);
			if (id == EULA_DIALOG) {
				eula.setCancelable(true);
			} else if (id == REQUIRED_EULA_DIALOG) {
				eula.setPositiveButton(eulaYes, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
						SharedPreferences.Editor eulaPref = prefs.edit();
						eulaPref.putInt("last_eula", versionCode);
						eulaPref.commit();
					}
				});
				eula.setNegativeButton(eulaNo, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
				eula.setCancelable(false);
			}
			return eula.create();
		}
		return super.onCreateDialog(id);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu m) {
    	if (eula > 0 && aboutTitle > 0) {
	     	MenuItem eula = m.add(Menu.NONE, EULA_ID, Menu.NONE, aboutTitle);
	    	eula.setIcon(android.R.drawable.ic_menu_info_details);
    	}
    	return super.onCreateOptionsMenu(m);
    }
    
	protected void storeVersion() {
		PackageInfo pkgInfo = null;
		try {
			pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			logger.log(Level.FINE, "Cannnot get package info", e);
		}
		if (pkgInfo == null) {
			logger.warning("No package information found, closing");
			finish();
		}
		versionCode = pkgInfo.versionCode;
		versionName = pkgInfo.versionName;
	}

	public void checkEula() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (eula > 0 && getVersionCode() > prefs.getInt("last_eula", -1)) {
			showDialog(REQUIRED_EULA_DIALOG);
		}
	}

	public void checkUpgrade(final String versionUrl, final int upgradeTitle, final int upgrade, final int upgradeYes) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.getBoolean("check_updates", true)) return;
		
    	new Thread() {
        	@Override
        	public void run() {
        		try {
        			HttpClient client = new DefaultHttpClient();
        			HttpGet get = new HttpGet(MessageFormat.format(versionUrl, versionName));
        			String resp = client.execute(get, new BasicResponseHandler());
        			resp = resp.trim();
        			String[] line = resp.split("\\s+");
        			final int newVersionCode = Integer.parseInt(line[0]);
        			final String updateUrl = line[1];
        			final String newVersionName = line[2];
        			if (newVersionCode > versionCode) {
        				handler.post(new Runnable() {
        					public void run() {
        						new AlertDialog.Builder(MainActivity.this)
        		        		.setMessage(getString(upgrade, newVersionName, versionName))
        		        		.setTitle(upgradeTitle)
        		        		.setIcon(android.R.drawable.ic_dialog_info)
        		        		.setCancelable(true)
        		        		.setPositiveButton(upgradeYes, new DialogInterface.OnClickListener() {
        							public void onClick(DialogInterface dialog, int which) {
        								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)));
        							}
        		        		})
        		        		.show();				
        					}
        				});
        			}
        		} catch (Exception e) {
        			logger.log(Level.SEVERE, "Error while checking for upgrade", e);
        		}
        	}
        }.start();
	}

	protected String readEULA() {
		if (eula <= 0) return "";
		Reader r = null;
		StringBuffer str = new StringBuffer();
		try {
			try {
				r = new InputStreamReader(getResources().openRawResource(eula));
				char[] buf = new char[8092];
				int l;
				while ((l = r.read(buf)) > 0) {
					str.append(buf, 0, l);
				}
			} finally {
				if (r != null) r.close();
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "While reading EULA", e);
			finish();
		}
		return str.toString();
		
	}

}