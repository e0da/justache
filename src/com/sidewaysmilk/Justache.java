package com.sidewaysmilk;

import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Put and get objects in a cache.
 * 
 * Runs a thread which prunes expired entries periodically to prevent memory
 * bloat.
 * 
 * Objects are added to the cache with a key and a value. Objects put into the
 * cache can be retrieved until they expire.
 * 
 * @author Justin Force - justin.force@gmail.com
 * 
 * @param <K>
 *            The Java class of the key
 * @param <V>
 *            The Java class of the values
 */
public class Justache<K, V> {

	/**
	 * The "time to live" of the cached object. After ttl milliseconds have
	 * passed, the object is to be expired.
	 */
	private final long ttl;

	/**
	 * The Java Hashtable which we will use to store our objects internally.
	 */
	private Hashtable<K, JustacheValue<V>> table;

	/**
	 * The thread which prunes expired entries
	 */
	private Thread thread;

	/**
	 * The maximum number of items allowed in the cache. Default is 0 for
	 * unlimited.
	 */
	private long maxSize;

	/**
	 * A chronological list of all of the keys for use with automatically
	 * deleting the oldest elements when maxSize is exceeded
	 */
	private Vector<K> keys;

	/**
	 * Constructs a new, empty cache with "time to live" set to ttl
	 * milliseconds. Objects put into the cache will be expired after ttl has
	 * elapsed.
	 * 
	 * @param ttl
	 *            "time to live" in milliseconds
	 */
	public Justache(final long ttl) {

		this.ttl = ttl;

		this.maxSize = 0;

		/*
		 * Set up new Hashtable to hold all of our cache key/value pairs
		 */
		table = new Hashtable<K, JustacheValue<V>>();

		/*
		 * Set up the thread and just run prune then sleep ttl milliseconds
		 * forever.
		 */
		thread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					prune();
					try {
						Thread.sleep(ttl);
					} catch (InterruptedException e) {
						// We really don't mind being interrupted. :)
					}
				}
			}
		});
		thread.start();
	}

	public Justache(final long ttl, final long maxSize) {
		this(ttl);
		this.maxSize = maxSize;
		this.keys = new Vector<K>();
	}

	/**
	 * Put value into cache with key as an index.
	 * 
	 * @param key
	 *            the key for the value
	 * @param value
	 *            the value
	 */
	public void put(K key, V value) {

		/*
		 * Wrap value in a new JustacheValue, setting the ttl to now + ttl
		 * milliseconds, then add it to the hashtable.
		 */
		JustacheValue<V> v = new JustacheValue<V>(
				new GregorianCalendar().getTimeInMillis() + ttl, value);
		table.put(key, v);

		/*
		 * Handle the maxSize maintenance if it's defined
		 */
		if (maxSize > 0) {

			keys.add(key);

			/*
			 * If the new length is over the max, remove the oldest item and set
			 * the next-to-oldest item as the oldest item.
			 */
			if (table.size() > maxSize) {
				remove(keys.firstElement());
				keys.remove(0);
			}
		}
	}

	/**
	 * Get value corresponding to key from cache
	 * 
	 * @param key
	 *            the key corresponding to value
	 * @return the value
	 * @throws JustacheKeyNotFoundException
	 *             if the value corresponding to key is not present, which can
	 *             happen if it has expired or never existed.
	 */
	public V get(K key) throws JustacheKeyNotFoundException {
		JustacheValue<V> o = table.get(key);
		if (o == null || o.expired()) {
			throw new JustacheKeyNotFoundException();
		} else {
			return o.get();
		}
	}

	/**
	 * Remove value corresponding to key from cache.
	 * 
	 * @param key
	 *            the key corresponding to the value
	 * @return the value that is removed
	 */
	public V remove(K key) {
		return table.remove(key).get();
	}

	/**
	 * Return the thread which is managing the pruning operations
	 * 
	 * @return the thread
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * Remove expired entries from cache. Meant to be done periodically, as in a
	 * thread, but can also be called manually if needed.
	 */
	public void prune() {
		Enumeration<K> keys = table.keys();
		while (keys.hasMoreElements()) {
			K key = keys.nextElement();
			if (table.get(key).expired()) {
				remove(key);
			}
		}
	}

	/**
	 * Wrapper for the cached value with its ttl
	 * 
	 * @author Justin Force - justin.force@gmail.com
	 * 
	 * @param <C>
	 *            the Java class of the value to be stored
	 */
	private class JustacheValue<C> {

		/**
		 * The value stored
		 */
		private C value;

		/**
		 * The expiration date in milliseconds
		 */
		private long expiry;

		/**
		 * Construct new JustacheValue with value and expiration date
		 * 
		 * @param expiry
		 *            the expiration date
		 * @param value
		 *            the value
		 */
		private JustacheValue(long expiry, C value) {
			this.expiry = expiry;
			this.value = value;
		}

		/**
		 * Check whether this value is expired
		 * 
		 * @return true if this value is expired, false if it is not
		 */
		private boolean expired() {
			return (new GregorianCalendar().getTimeInMillis() > expiry);
		}

		/**
		 * Get the value
		 * 
		 * @return the value
		 */
		private C get() {
			return value;
		}
	}
}
