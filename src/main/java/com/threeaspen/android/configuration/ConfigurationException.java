package com.threeaspen.android.configuration;

public class ConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 9016185324104035202L;

	public ConfigurationException(String detailMessage) {
		super(detailMessage);
	}

	public ConfigurationException(Throwable throwable) {
		super(throwable);
	}

	public ConfigurationException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
