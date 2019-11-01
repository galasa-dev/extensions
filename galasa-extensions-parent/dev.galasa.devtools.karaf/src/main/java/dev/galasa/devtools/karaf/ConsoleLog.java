/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.devtools.karaf;

import org.apache.commons.logging.Log;

public class ConsoleLog implements Log {

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isFatalEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}
	
	public void writeMessage(Object message, Throwable t) {
		System.out.println(message);
		if (t != null) {
			t.printStackTrace(System.out);
		}
	}

	@Override
	public void trace(Object message) {
		if (isTraceEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void trace(Object message, Throwable t) {
		if (isTraceEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void debug(Object message) {
		if (isDebugEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void debug(Object message, Throwable t) {
		if (isDebugEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void info(Object message) {
		if (isInfoEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void info(Object message, Throwable t) {
		if (isInfoEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void warn(Object message) {
		if (isWarnEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void warn(Object message, Throwable t) {
		if (isWarnEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void error(Object message) {
		if (isErrorEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void error(Object message, Throwable t) {
		if (isErrorEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void fatal(Object message) {
		if (isFatalEnabled()) {
			writeMessage(message, null);
		}
	}

	@Override
	public void fatal(Object message, Throwable t) {
		if (isFatalEnabled()) {
			writeMessage(message, null);
		}
	}

}
