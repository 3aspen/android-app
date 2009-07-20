package com.threeaspen.android.configuration;

import android.content.res.Resources.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Configuration extends Application implements OnSharedPreferenceChangeListener {
    private static final Logger logger = Logger.getLogger("com.threeaspen.android.configuration.Configuration");

	private Map<String, PreferenceBean> preferences;
	private Map<String, Bean> beans;
	private Set<String> interested = new HashSet<String>();
	
	public Configuration() {
	}
	
	protected abstract XmlResourceParser getXml();
	
	@Override
	public void onCreate() {
		super.onCreate();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	prefs.registerOnSharedPreferenceChangeListener(this);
		doConfigure();
	}

	private boolean isInterested(String key) {
		synchronized (interested) {
			return interested.contains(key);
		}
	}
	
	private void addInterestedPref(String key) {
		synchronized (interested) {
			interested.add(key);
		}
	}
	
	private void clearInterestedPrefs() {
		synchronized (interested) {
			interested.clear();
		}
	}
	
	public void doConfigure() {
		try {
			preferences = new HashMap<String, PreferenceBean>();
			beans = new HashMap<String, Bean>();
			clearInterestedPrefs();
			XmlResourceParser cfgRes = getXml();
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        	int elementType = cfgRes.getEventType();
			PreferenceBean prefBean = null;
			String prefDefault = "";
			Bean bean = null;
			List<Object> list = null;
			String listName = null;
			while (elementType != XmlResourceParser.END_DOCUMENT) {
				if ("preference".equals(cfgRes.getName())) {
					if (elementType == XmlResourceParser.START_TAG) {
						prefBean = new PreferenceBean(); 
						prefBean.setKey(cfgRes.getAttributeValue(null, "prefKey"));
						prefBean.setType(Class.forName(cfgRes.getAttributeValue(null, "type")));
						logger.info("Configuring PreferenceBean " + prefBean.getKey() + " of " + prefBean.getType().getName());
						prefDefault = cfgRes.getAttributeValue(null, "default");
					} else if (elementType == XmlResourceParser.END_TAG) {
						addInterestedPref(prefBean.getKey());
						String cfgChoice = prefs.getString(prefBean.getKey(), prefDefault);
						logger.info("Setting PreferenceBean " + prefBean.getKey() + " to " + cfgChoice);
						prefBean.setChoice(cfgChoice);
			        	preferences.put(prefBean.getKey(), prefBean);
						prefBean = null;
					}
				} else if ("bean".equals(cfgRes.getName())) {
					if (elementType == XmlResourceParser.START_TAG) {
						bean = new Bean(cfgRes.getIdAttribute());
						bean.setClassName(cfgRes.getClassAttribute());
						bean.setCached(cfgRes.getAttributeBooleanValue(null, "cache", true));
					} else if (elementType == XmlResourceParser.END_TAG) {
						if (prefBean == null) {
							logger.fine("Adding Bean: " + bean);
							beans.put(bean.getId(), bean);
						} else {
							logger.fine("Adding Choice: " + bean);
							prefBean.addChoice(bean);
						}
						bean = null;
					}
				} else if ("property".equals(cfgRes.getName()) && bean != null) {
					if (elementType == XmlResourceParser.START_TAG) {
						Object value = resolveValue(cfgRes, prefs);
						String name = cfgRes.getAttributeValue(null, "name");
						logger.fine("Setting parameter " + name + ":" + value);
						bean.setParam(name, value);
					}
				} else if ("list".equals(cfgRes.getName()) && bean != null) {
					if (elementType == XmlResourceParser.START_TAG) {
						list = new ArrayList<Object>();
						listName = cfgRes.getAttributeValue(null, "name");
					} else if (elementType == XmlResourceParser.END_TAG) {
						logger.fine("Setting parameter " + listName + ":" + list.toString());
						bean.setParam(listName, list);
					}
				} else if ("value".equals(cfgRes.getName()) && list != null) {
					if (elementType == XmlResourceParser.START_TAG) {
						Object value = resolveValue(cfgRes, prefs);
						list.add(value);
					}
				}
				elementType = cfgRes.next();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not parse configuration resource", e);
			throw new ConfigurationException("Could not parse configuration resource", e);
		}
	}

	protected Object resolveValue(XmlResourceParser cfgRes, SharedPreferences prefs) {
		Object value = null;
        Object defaultValue = null;
		for (int i=0; i<cfgRes.getAttributeCount(); i++) {
			String n = cfgRes.getAttributeName(i);
            if ("default".equals(n)) {
                defaultValue = resolveResource(cfgRes, i);
            }
			if ("value".equals(n)) {
                value = resolveResource(cfgRes, i);
			} else if ("resource".equals(n)) {
				value = cfgRes.getAttributeResourceValue(i, 0);
			} else if ("class".equals(n)) {
				try {
					value = Class.forName(cfgRes.getAttributeValue(i));
				} catch (ClassNotFoundException e) {
					logger.log(Level.SEVERE, "Cannot instantiate class", e);
					value = null;
				}
			} else if ("context".equals(n)) {
				String k = cfgRes.getAttributeValue(i);
				addInterestedPref(k);
				value = prefs.getAll().get(k);
			} else if ("ref".equals(n)) {
				String k = cfgRes.getAttributeValue(i);
				value = beans.get(k);
				if (value == null) value = preferences.get(k);
			}
		}
        if (value == null) return defaultValue;
		return value;
	}

	protected Object resolveResource(XmlResourceParser cfgRes, int i) throws NotFoundException
    {
        Object value = null;
        int r = cfgRes.getAttributeResourceValue(i, -1);
        if (r != -1) {
            String tn = getResources().getResourceTypeName(r);
            if ("drawable".equals(tn)) {
                value = getResources().getDrawable(r);
            } else if ("color".equals(tn)) {
                value = getResources().getColor(r);
            } else {
                value = getText(r);
            }
        }
        if (value == null) {
            value = cfgRes.getAttributeValue(i);
        }
        return value;
    }

	public void onSharedPreferenceChanged(SharedPreferences prefs, String prefKey) {
		if (!isInterested(prefKey)) return;
		doConfigure();
	}
	
	@Override
	public void onConfigurationChanged(android.content.res.Configuration newConfig) {
		doConfigure();
		super.onConfigurationChanged(newConfig);
	}
	
	protected Object getConfigured(String prefKey) {
		PreferenceBean prefBean = preferences.get(prefKey);
		if (prefBean == null) {
            logger.warning("No preference bean found for key " + prefKey);
            return null;
        }
		return prefBean.getConfigured();
	}
	
	protected Collection<?> getChoices(String prefKey) {
		PreferenceBean prefBean = preferences.get(prefKey);
		if (prefBean == null) return null;
		return prefBean.getChoices();
	}

    protected Object getBean(String beanKey) {
        Bean b = beans.get(beanKey);
        if (b == null) return null;
        return b.instantiate();
    }

	
	
}
