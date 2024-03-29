/*******************************************************************************
 * Copyright (c) 2010-2013 Federico Pecora <federico.pecora@oru.se>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package org.metacsp.utility.logging;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.metacsp.framework.meta.MetaConstraintSolver;

/**
 * <p>
 * This class provides basic console logging functionalities for the MetaCSP Framework.
 * It employs the {@link java.util.logging.Logger} class, and defines uniform formatter
 * for the framework.
 * </p>
 * 
 * <p>
 * Each class should have its own specific logger, which is provided by the factory method of
 * {@link MetaCSPLogging}, e.g.,
 * </p>
 * 
 * <p>
 * <code>private Logger logger = MetaCSPLogging.getLogger(this.getClass());</code>
 * </p>
 * 
 * <p>
 * The obtained {@code logger} instance can then be used to log messages at different
 * log-levels (see {@link Level}) through the {@link Logger}'s specific methods, e.g.,
 * {@code info(String msg)}, {@code fine(String msg)}, {@code finer(String msg)},
 * {@code warning(String msg)} etc.
 * </p>
 * 
 * <p>
 * To enable logging at a specific level (e.g., from your test class), the static
 * methods {@code setLevel(Class<?> c, Level l)} and {@code setLevel(Level l)} are provided.
 * The former sets the desired level on a specific class (e.g., suppose you want to log at level
 * {@code Level.FINE} only for a {@link MetaConstraintSolver}); the latter activates that log
 * level for all classes which use a {@link Logger} provided by the {@link MetaCSPLogging} factory
 * method.
 * </p>
 * 
 * @author Federico Pecora
 *
 */
public final class MetaCSPLogging implements Serializable{

	private transient static HashMap<Class<?>,Logger> loggers = new HashMap<Class<?>,Logger>();

	private transient static HashMap<Class<?>,Level> tempLevels = new HashMap<Class<?>,Level>();

	private static final long serialVersionUID = 7526472295622776139L;

	private static Level globalLevel = null;
	
	private static String logDir = null;

	static { }

	/**
	 * Instruct loggers to log all output to a directory. Each class will be logged to a file named
	 * <code>className.log</code>.
	 * @param dir The directory to log to.
	 */
	public static void setLogDir(String dir) {
		try { 
			logDir = dir;
			for (Entry<Class<?>, Logger> en : loggers.entrySet()) {
				FileHandler fh = new FileHandler(logDir+File.separator+en.getKey().getSimpleName()+".log");
				fh.setFormatter(getFormatter(en.getValue(),true));
				en.getValue().addHandler(fh);
			}
		}
		catch (SecurityException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }  
	}

	private MetaCSPLogging() {}

	/**
	 * Set a desired log level for all loggers.
	 * @param l The desired log level.
	 */
	public static void setLevel(Level l) {
		for (Logger log : loggers.values()) log.setLevel(l);
		globalLevel = l;
	}

	/**
	 * Set a desired log level for a specific class.
	 * @param c The class to set the log level for.
	 * @param l The desired log level.
	 */
	public static void setLevel(Class<?> c, Level l) /* throws LoggerNotDefined */ {
		if (!loggers.containsKey(c)) tempLevels.put(c, l);
		else loggers.get(c).setLevel(l);
		//System.out.println("Set level " + l + " for logger " + loggers.get(c).getName());
	}

	private static Formatter getFormatter(final Logger logger, final boolean timeStamp) {
		return new Formatter() {
			@Override
			public String format(LogRecord arg0) {
				String prefix = "";
				if (timeStamp) prefix = Calendar.getInstance().getTimeInMillis()+"@";
				String ret = prefix + ("[" + logger.getName() + "] " + arg0.getMessage() + "\n");
				return ret;
			}
		};
	}
	/**
	 * Provides a reference to a {@link Logger} which will format log messages according to
	 * the common MetaCSP Framework formatting.
	 * @param c The class within which the logger should be used.
	 * @return A {@link Logger} that can be used to log messages in the given class.
	 */
	public static Logger getLogger(final Class<?> c) {
		if (loggers.get(c) == null) {
			//System.out.println("Making new logger for " + c.getSimpleName());
			final Logger logger = Logger.getLogger(c.getSimpleName());
			loggers.put(c, logger);
			for(Handler h : logger.getParent().getHandlers()) { logger.getParent().removeHandler(h); }
			ConsoleHandler h = new ConsoleHandler();
			h.setFormatter(getFormatter(logger,false));
			h.setLevel(Level.ALL);
			logger.addHandler(h);
			if (tempLevels.keySet().contains(c)) {
				logger.setLevel(tempLevels.get(c));
				tempLevels.remove(c);
			}
			if (logDir != null) {
				try {
					FileHandler fh = new FileHandler(logDir+File.separator+c.getSimpleName()+".log");
					fh.setFormatter(getFormatter(logger,true));
					logger.addHandler(fh);
				}
				catch (SecurityException e) { e.printStackTrace(); }
				catch (IOException e) { e.printStackTrace(); }
			}
			if (globalLevel != null) logger.setLevel(globalLevel);
			return logger;
		}
		//System.out.println("Returning old logger for " + c.getSimpleName());
		return loggers.get(c);
	}

	public static String printDouble(double d, int precision) {
		String fmt = "#0."; 
		for (int i = 0; i < precision; i++) fmt += "0"; 
		NumberFormat formatter = new DecimalFormat(fmt); 
		return formatter.format(d);	
	}
	
	/**
	 * Open a LogBrowser to inspect the log files in a given directory.
	 * @param dir The directory containing log files.
	 */
	public static void showLogs(String dir) {
		LogBrowser.showLogs(dir);
	}

}
