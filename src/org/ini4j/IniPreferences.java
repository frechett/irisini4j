/*
 * Copyright 2005,2009 Ivan SZKIBA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ini4j;

import static org.ini4j.IniPreferencesFactory.KEY_SYSTEM;
import static org.ini4j.IniPreferencesFactory.KEY_USER;
import static org.ini4j.IniPreferencesFactory.PROPERTIES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class IniPreferences extends AbstractPreferences {
	protected class SectionPreferences extends AbstractPreferences {
		/** underlaying <code>Section</code> implementation */
		private final Ini.Section _section;

		/**
		 * Constructs a new SectionPreferences instance on top of Ini.Section
		 * instance.
		 *
		 * @param parent
		 *            parent preferences node
		 * @param section underlaying Ini.Section instance
		 * @param isNew
		 *            indicate is this a new node or already existing one
		 */
		SectionPreferences(AbstractPreferences parent, Ini.Section section,
				boolean isNew) {
			super(parent, section.getSimpleName());
			_section = section;
			newNode = isNew;
		}

		@Override
		protected String[] childrenNamesSpi() throws BackingStoreException {
			return _section.childrenNames();
		}

		@Override
		protected SectionPreferences childSpi(String name)
				throws UnsupportedOperationException {
			Ini.Section child = _section.getChild(name);
			boolean isNew = child == null;

			if (isNew) {
				child = _section.addChild(name);
				if (!ignoreChange) {
					change = true;
					store();
				}
			}

			return new SectionPreferences(this, child, isNew);
		}

		/**
		 * Implements the <CODE>flush</CODE> method as per the specification in
		 * {@link java.util.prefs.Preferences#flush()}.
		 * <p>
		 * This implementation just call parent's <code>flush()</code> method.
		 *
		 * @throws BackingStoreException
		 *             if this operation cannot be completed due to a failure in
		 *             the backing store, or inability to communicate with it.
		 */
		@Override
		public void flush() throws BackingStoreException {
			parent().flush();
		}

		/**
		 * Implements the <CODE>flushSpi</CODE> method as per the specification
		 * in {@link java.util.prefs.AbstractPreferences#flushSpi()}.
		 *
		 * This implementation does nothing.
		 *
		 * @throws BackingStoreException
		 *             if this operation cannot be completed due to a failure in
		 *             the backing store, or inability to communicate with it.
		 */
		@Override
		protected void flushSpi() throws BackingStoreException {
			assert true;
		}

		/**
		 * Implements the <CODE>getSpi</CODE> method as per the specification in
		 * {@link java.util.prefs.AbstractPreferences#getSpi(String)}.
		 * 
		 * @return if the value associated with the specified key at this
		 *         preference node, or null if there is no association for this
		 *         key, or the association cannot be determined at this time.
		 * @param key
		 *            key to get value for
		 */
		@Override
		protected String getSpi(String key) {
			return _section.fetch(key);
		}

		/**
		 * Implements the <CODE>keysSpi</CODE> method as per the specification
		 * in {@link java.util.prefs.AbstractPreferences#keysSpi()}.
		 *
		 * @return an array of the keys that have an associated value in this
		 *         preference node.
		 * @throws BackingStoreException
		 *             if this operation cannot be completed due to a failure in
		 *             the backing store, or inability to communicate with it.
		 */
		@Override
		protected String[] keysSpi() throws BackingStoreException {
			return _section.keySet().toArray(EMPTY);
		}

		@Override
		public Preferences node(String path) {
			ignoreChange = true;
			Preferences node = super.node(path);
			ignoreChange = false;
			return node;
		}

		/**
		 * Implements the <CODE>putSpi</CODE> method as per the specification in
		 * {@link java.util.prefs.AbstractPreferences#putSpi(String,String)}.
		 *
		 * @param key
		 *            key to set value for
		 * @param value
		 *            new value of key
		 */
		@Override
		protected void putSpi(String key, String value) {
			String old = _section.get(key);
			if (old == value) {
				return;
			}
			if (old != null && old.equals(value)) {
				return;
			}
			_section.put(key, value);
			if (!ignoreChange) {
				change = true;
				store();
			}
		}

		/**
		 * Implements the <CODE>removeNodeSpi</CODE> method as per the
		 * specification in
		 * {@link java.util.prefs.AbstractPreferences#removeNodeSpi()}.
		 *
		 * @throws BackingStoreException
		 *             if this operation cannot be completed due to a failure in
		 *             the backing store, or inability to communicate with it.
		 */
		@Override
		protected void removeNodeSpi() throws BackingStoreException {
			_ini.remove(_section);
			if (!ignoreChange) {
				change = true;
				store();
			}
		}

		/**
		 * Implements the <CODE>removeSpi</CODE> method as per the specification
		 * in {@link java.util.prefs.AbstractPreferences#removeSpi(String)}.
		 * 
		 * @param key
		 *            key to remove
		 */
		@Override
		protected void removeSpi(String key) {
			if (_section.remove(key) != null) {
				if (!ignoreChange) {
					change = true;
					store();
				}
			}
		}

		/**
		 * Implements the <CODE>sync</CODE> method as per the specification in
		 * {@link java.util.prefs.Preferences#sync()}.
		 *
		 * This implementation just call parent's <code>sync()</code> method.
		 *
		 * @throws BackingStoreException
		 *             if this operation cannot be completed due to a failure in
		 *             the backing store, or inability to communicate with it.
		 */
		@Override
		public void sync() throws BackingStoreException {
			parent().sync();
		}

		/**
		 * Implements the <CODE>syncSpi</CODE> method as per the specification
		 * in {@link java.util.prefs.AbstractPreferences#syncSpi()}.
		 *
		 * This implementation does nothing.
		 *
		 * @throws BackingStoreException
		 *             if this operation cannot be completed due to a failure in
		 *             the backing store, or inability to communicate with it.
		 */
		@Override
		protected void syncSpi() throws BackingStoreException {
			assert true;
		}
	}

	private static volatile Preferences _system;
	private static volatile Preferences _user;
	/** frequently used empty String array */
	private static final String[] EMPTY = {};

	/**
	 * Get INI location.
	 * 
	 * @param key
	 *            the property key.
	 * @return the INI location or null if none.
	 */
	private static String getIniLocation(String key) {
		String location = Config.getSystemProperty(key);
		if (location == null) {
			try {
				Properties props = new Properties();
				props.load(Thread.currentThread().getContextClassLoader()
						.getResourceAsStream(PROPERTIES));
				location = props.getProperty(key);
			} catch (Exception x) {
				assert true;
			}
		}
		return location;
	}

	private static Logger getLogger() {
		return Logger.getLogger("java.util.prefs");
	}

	/**
	 * Get the resource.
	 * 
	 * @param location
	 *            the location.
	 * @return the resource.
	 * @throws IllegalArgumentException
	 *             if error.
	 */
	private static URL getResource(String location)
			throws IllegalArgumentException {
		try {
			URI uri = new URI(location);
			URL url;

			if (uri.getScheme() == null) {
				url = Thread.currentThread().getContextClassLoader()
						.getResource(location);
			} else {
				url = uri.toURL();
			}

			return url;
		} catch (Exception x) {
			throw (IllegalArgumentException) new IllegalArgumentException()
					.initCause(x);
		}
	}

	/**
	 * Get the system root.
	 * 
	 * @return the system root.
	 */
	public static Preferences getSystemRoot() {
		Preferences system = _system;
		if (system == null) {
			system = newIniPreferences(false);
			_system = system;
		}
		return system;
	}

	/**
	 * Get the system root file.
	 * 
	 * @return the system root file.
	 */
	public static File getSystemRootFile() {
		String root = System.getProperty("java.util.prefs.systemRoot");
		if (root == null || root.startsWith("~/")) {
			final String userHome = System.getProperty("user.home");
			if (root == null) {
				root = userHome;
			} else {
				root = userHome + root.substring(1);
			}
		}
		File rootDir = new File(root);
		// Attempt to create root directory if it does not yet exist.
		if (!rootDir.exists()) {
			if (!rootDir.mkdirs()) {
				getLogger().warning(
						"Couldn't create system preferences directory.");
			}
		}
		return new File(rootDir, "system.ini");
	}

	/**
	 * Get the user root.
	 * 
	 * @return the user root.
	 */
	public static Preferences getUserRoot() {
		Preferences user = _user;
		if (user == null) {
			user = newIniPreferences(true);
			_user = user;
		}
		return user;
	}

	/**
	 * Get the user root file.
	 * 
	 * @return the user root file.
	 */
	public static File getUserRootFile() {
		String root = System.getProperty("java.util.prefs.userRoot");
		if (root == null || root.startsWith("~/")) {
			final String userHome = System.getProperty("user.home");
			if (root == null) {
				root = userHome;
			} else {
				root = userHome + root.substring(1);
			}
		}
		File rootDir = new File(root);
		// Attempt to create root directory if it does not yet exist.
		if (!rootDir.exists()) {
			if (!rootDir.mkdirs()) {
				getLogger()
						.warning("Couldn't create user preferences directory.");
			}
		}
		return new File(rootDir, "user.ini");
	}

	/**
	 * Create the new INI preferences.
	 * 
	 * @param user
	 *            true if user preferences, false if system preferences.
	 * @return the new INI preferences.
	 */
	private static Preferences newIniPreferences(boolean user) {
		final Ini ini = new Ini();
		try {
			String location;
			if (user) {
				location = getIniLocation(KEY_USER);
			} else {
				location = getIniLocation(KEY_SYSTEM);
			}
			if (location != null) {
				try {
					ini.load(getResource(location).openStream());
				} catch (Exception x) {
					throw (IllegalArgumentException) new IllegalArgumentException()
							.initCause(x);
				}
			} else {
				File file;
				if (user) {
					file = getUserRootFile();
				} else {
					file = getSystemRootFile();
				}
				ini.setFile(file);
				if (file.canRead()) {
					ini.load();
				}
			}
		} catch (Exception ex) {
			throw (IllegalArgumentException) new IllegalArgumentException()
					.initCause(ex);
		}
		return new IniPreferences(ini);
	}

	/** underlaying <code>Ini</code> implementation */
	private final Ini _ini;
	private volatile boolean change;
	private volatile boolean ignoreChange;

	/**
	 * Constructs a new preferences node on top of <code>Ini</code> instance.
	 *
	 * @param ini
	 *            underlaying <code>Ini</code> instance
	 */
	public IniPreferences(Ini ini) {
		super(null, "");
		_ini = ini;
	}

	/**
	 * Constructs a new preferences node based on newly loaded <code>Ini</code>
	 * instance.
	 * <p>
	 * This is just a helper constructor, to make simpler constructing
	 * <code>IniPreferences</code> directly from <code>InputStream</code>.
	 *
	 * @param input
	 *            the <code>InputStream</code> containing <code>Ini</code> data
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws InvalidFileFormatException
	 *             if <code>Ini</code> parsing error occurred
	 */
	public IniPreferences(InputStream input)
			throws IOException, InvalidFileFormatException {
		super(null, "");
		_ini = new Ini(input);
	}

	/**
	 * Constructs a new preferences node based on newly loaded <code>Ini</code>
	 * instance.
	 * <p>
	 * This is just a helper constructor, to make simpler constructing
	 * <code>IniPreferences</code> directly from <code>Reader</code>.
	 *
	 * @param input
	 *            the <code>Reader</code> containing <code>Ini</code> data
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws InvalidFileFormatException
	 *             if <code>Ini</code> parsing error occurred
	 */
	public IniPreferences(Reader input)
			throws IOException, InvalidFileFormatException {
		super(null, "");
		_ini = new Ini(input);
	}

	/**
	 * Constructs a new preferences node based on newly loaded <code>Ini</code>
	 * instance.
	 * <p>
	 * This is just a helper constructor, to make simpler constructing
	 * <code>IniPreferences</code> directly from <code>URL</code>.
	 *
	 * @param input
	 *            the <code>URL</code> containing <code>Ini</code> data
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws InvalidFileFormatException
	 *             if <code>Ini</code> parsing error occurred
	 */
	public IniPreferences(URL input)
			throws IOException, InvalidFileFormatException {
		super(null, "");
		_ini = new Ini(input);
	}

	/**
	 * Implements the <CODE>childrenNamesSpi</CODE> method as per the
	 * specification in
	 * {@link java.util.prefs.AbstractPreferences#childrenNamesSpi()}.
	 * 
	 * @return an array containing the names of the children of this preference
	 *         node.
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 */
	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		List<String> names = new ArrayList<String>();

		for (String name : _ini.keySet()) {
			if (name.indexOf(_ini.getPathSeparator()) < 0) {
				names.add(name);
			}
		}

		return names.toArray(EMPTY);
	}

	/**
	 * Implements the <CODE>childSpi</CODE> method as per the specification in
	 * {@link java.util.prefs.AbstractPreferences#childSpi(String)}.
	 * 
	 * @param name
	 *            child name
	 * @return child node
	 */
	@Override
	protected SectionPreferences childSpi(String name) {
		Ini.Section sec = _ini.get(name);
		boolean isNew = sec == null;

		if (isNew) {
			sec = _ini.add(name);
			if (!ignoreChange) {
				change = true;
				store();
			}
		}

		return new SectionPreferences(this, sec, isNew);
	}

	@Override
	protected void flushSpi() {
		store();
	}

	/**
	 * Provide access to underlaying {@link org.ini4j.Ini} implementation.
	 *
	 * @return <code>Ini</code> implementation
	 */
	protected Ini getIni() {
		return _ini;
	}

	/**
	 * Implements the <CODE>getSpi</CODE> method as per the specification in
	 * {@link java.util.prefs.AbstractPreferences#getSpi(String)}.
	 * <p>
	 * This implementation doesn't support this operation, so always throws
	 * UnsupportedOperationException.
	 *
	 * @return if the value associated with the specified key at this preference
	 *         node, or null if there is no association for this key, or the
	 *         association cannot be determined at this time.
	 * @param key
	 *            key to get value for
	 * @throws UnsupportedOperationException
	 *             this implementation always throws this exception
	 */
	@Override
	protected String getSpi(String key) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Implements the <CODE>keysSpi</CODE> method as per the specification in
	 * {@link java.util.prefs.AbstractPreferences#keysSpi()}.
	 * <p>
	 * This implementation always return an empty array.
	 *
	 * @return an empty array.
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 */
	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return EMPTY;
	}

	@Override
	public Preferences node(String path) {
		ignoreChange = true;
		Preferences node = super.node(path);
		ignoreChange = false;
		return node;
	}

	/**
	 * Implements the <CODE>putSpi</CODE> method as per the specification in
	 * {@link java.util.prefs.AbstractPreferences#putSpi(String,String)}.
	 * <p>
	 * This implementation doesn't support this operation, so always throws
	 * UnsupportedOperationException.
	 *
	 * @param key
	 *            key to set value for
	 * @param value
	 *            new value for key
	 * @throws UnsupportedOperationException
	 *             this implementation always throws this exception
	 */
	@Override
	protected void putSpi(String key, String value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Implements the <CODE>removeNodeSpi</CODE> method as per the specification
	 * in {@link java.util.prefs.AbstractPreferences#removeNodeSpi()}.
	 * <p>
	 * This implementation doesn't support this operation, so always throws
	 * UnsupportedOperationException.
	 * 
	 * @throws UnsupportedOperationException
	 *             this implementation always throws this exception
	 * @throws BackingStoreException
	 *             this implementation never throws this exception
	 */
	@Override
	protected void removeNodeSpi()
			throws BackingStoreException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Implements the <CODE>removeSpi</CODE> method as per the specification in
	 * {@link java.util.prefs.AbstractPreferences#removeSpi(String)}.
	 * 
	 * @param key
	 *            key to remove
	 * @throws UnsupportedOperationException
	 *             this implementation always throws this exception
	 */
	@Override
	protected void removeSpi(String key) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	private void store() {
		if (!change) {
			return;
		}
		change = false;
		try {
			final File file = _ini.getFile();
			if (file != null && (!file.exists() || file.canWrite())) {
				try (FileOutputStream fos = new FileOutputStream(file)) {
					_ini.store(fos);
				}
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			getLogger().warning(e.toString());
		}
	}

	/**
	 * Implements the <CODE>syncSpi</CODE> method as per the specification in
	 * {@link java.util.prefs.AbstractPreferences#syncSpi()}.
	 * <p>
	 * This implementation does nothing.
	 *
	 * @throws BackingStoreException
	 *             if this operation cannot be completed due to a failure in the
	 *             backing store, or inability to communicate with it.
	 */
	@Override
	protected void syncSpi() throws BackingStoreException {
		assert true;
	}
}
