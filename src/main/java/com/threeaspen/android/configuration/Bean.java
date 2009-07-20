package com.threeaspen.android.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class Bean {
	private static final Logger logger = Logger.getLogger("com.threeaspen.android.configuration.Bean");

    private String className;
	private Map<String, Object> params = new HashMap<String, Object>();
	private boolean cached;
	
	private Object cache;
	
	protected Bean(String id) {
		setParam("id", id);
		this.cached = true;
	}
	
	String getId() {
		return (String)params.get("id");
	}

	void setClassName(String className) {
		this.className = className;
	}
	
	void setParam(String name, Object value) {
		params.put(name, value);
	}
	
	void setCached(boolean cached) {
		this.cached = cached;
	}
	
	@Override
	public String toString() {
		return getId() + "[" + className + "]" + (cached ? "(cached)" : "");
	}
	
	Object instantiateRecursive(Object o) {
		if (o instanceof Bean) {
			Bean b = (Bean)o;
			return b.instantiate();
		}
		if (o instanceof Collection) {
			Collection<Object> c = new ArrayList<Object>();
			for (Object v : (Collection<?>)o) {
				c.add(instantiateRecursive(v));
			}
			return c;
		}
		return o;
	}
	
	Object instantiate() {
		try {
			if (cache != null) return cache;

			Class<?> c = Class.forName(className);
			Map<String, Object> instantiatedParams = new HashMap<String, Object>();
			for (Map.Entry<String, Object> e : params.entrySet()) {
				instantiatedParams.put(e.getKey(), instantiateRecursive(e.getValue()));
			}
            Object o = c.getConstructor(Map.class).newInstance(instantiatedParams);
			
			if (cached) cache = o;
			return o;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot instantiate bean", e);
			throw new ConfigurationException(e);
		}
	}
	
}