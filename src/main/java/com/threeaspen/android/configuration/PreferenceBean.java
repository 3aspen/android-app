/**
 * 
 */
package com.threeaspen.android.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

class PreferenceBean {
    private static final Logger logger = Logger.getLogger("com.threeaspen.android.configuration.PreferenceBean");
	private String key;
	private Class<?> type;
	private String choice;
	private Map<String, Bean> choices = new LinkedHashMap<String, Bean>();
	
	protected PreferenceBean() {
	}
	
	void setKey(String key) {
		this.key = key;
	}

	String getKey() {
		return key;
	}

	void setType(Class<?> type) {
		this.type = type;
	}

	Class<?> getType() {
		return type;
	}

	void setChoice(String choice) {
		this.choice = choice;
	}

	String getChoice() {
		return choice;
	}

	Object getConfigured() {
		return getChoice(choice);
	}
	
	Object getChoice(String id) {
		Bean b = choices.get(id);
		if (b == null) {
            logger.warning("No choice found for id " + id + " in preference bean " + key);
            return null;
        }
		return b.instantiate();
	}
	
	void addChoice(Bean b) {
		choices.put(b.getId(), b);
	}
	
	Collection<Object> getChoices() {
		Collection<Object> c = new ArrayList<Object>();
		for (Bean b : choices.values()) {
			c.add(b.instantiate());
		}
		return c;
	}
	
}