package com.microsoftopentechnologies.acs.federation;

public class LogonFailureException extends Exception {
	private static final long serialVersionUID = 1L;

	public LogonFailureException(String message) {
        super(message);
    }
}
