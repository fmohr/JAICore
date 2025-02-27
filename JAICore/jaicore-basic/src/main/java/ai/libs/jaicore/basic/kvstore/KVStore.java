package ai.libs.jaicore.basic.kvstore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.api4.java.datastructure.kvstore.IKVFilter;
import org.api4.java.datastructure.kvstore.IKVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.libs.jaicore.basic.sets.SetUtil;

/**
 * A KVStore can be used to store arbitrary objects for some string key.
 * The KVStore allows for more convenient data access and some basic operations.
 * Within KVStoreCollections it can be subject to significance tests and it may
 * be transformed into a table representation.
 *
 * @author mwever
 */
public class KVStore extends HashMap<String, Object> implements IKVStore, Serializable {
	/**
	 * Auto-generated standard serial version UID.
	 */
	private static final long serialVersionUID = 6635572555061279948L;

	/**
	 * Logger for controlled command line outputs.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(KVStore.class);

	/**
	 * Default separator to separate elements of a list.
	 */
	private static final String DEFAULT_LIST_SEP = ",";

	private KVStoreCollection collection;

	/**
	 * Standard c'tor creating an empty KV store.
	 */
	public KVStore() {

	}

	/**
	 * C'tor creating a KV store loading the data from the provided string representation.
	 *
	 * @param stringRepresentation
	 *            A string formatted key value store to be restored.
	 */
	public KVStore(final String stringRepresentation) {
		this.readKVStoreFromDescription(stringRepresentation);
	}

	/**
	 * C'tor for creating a shallow copy of another KeyValueStore or to initialize with the provided keyValueMap.
	 *
	 * @param keyValueMap
	 *            Map of keys and values to initialize this KeyValueStore with.
	 */
	public KVStore(final Map<String, Object> keyValueMap) {
		this.putAll(keyValueMap);
	}

	/**
	 * C'tor for making a deep copy of another KVStore.
	 *
	 * @param keyValueStoreToCopy
	 *            The KVStore to make a deep copy from.
	 */
	public KVStore(final IKVStore keyValueStoreToCopy) {
		this(keyValueStoreToCopy.toString());
	}

	@Override
	public String getAsString(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return (String) value;
		} else {
			return value + "";
		}
	}

	@Override
	public Boolean getAsBoolean(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return false;
		} else if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof String) {
			return Boolean.valueOf((String) value);
		} else {
			throw new IllegalStateException("Tried to get non-boolean value as boolean from KVStore.");
		}
	}

	@Override
	public Integer getAsInt(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return null;
		} else if (value instanceof Integer) {
			return (Integer) value;
		} else if (value instanceof Long) {
			return Integer.valueOf(value.toString());
		} else if (value instanceof Double) {
			return ((Double) value).intValue();
		} else if (value instanceof String) {
			try {
				return Integer.valueOf((String) value);
			} catch (NumberFormatException e) {
				try {
					return (int) (double) Double.valueOf((String) value);
				} catch (NumberFormatException e1) {
					throw new IllegalStateException("Tired of casting this value " + value + " to a number and I give up.");
				}
			}
		} else {
			throw new IllegalStateException("Tried to get non-integer value as integer from KVStore. Type of value " + value + " is " + value.getClass().getName());
		}
	}

	@Override
	public Double getAsDouble(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return null;
		} else if (value instanceof Double) {
			return (Double) value;
		} else if (value instanceof String) {
			return Double.valueOf((String) value);
		} else if (value instanceof Integer) {
			return Double.parseDouble(value + "");
		} else if (value instanceof Long) {
			return Double.parseDouble(value + "");
		} else {
			throw new IllegalStateException("Tried to get non-double value as double from KVStore.");
		}
	}

	@Override
	public Long getAsLong(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return null;
		} else if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof String) {
			return Long.valueOf((String) value);
		} else {
			throw new IllegalStateException("Tried to get non-long value as long from KVStore.");
		}
	}

	@Override
	public Short getAsShort(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return null;
		} else if (value instanceof Short) {
			return (Short) value;
		} else if (value instanceof String) {
			return Short.valueOf((String) value);
		} else {
			throw new IllegalStateException("Tried to get non-short value as short from KVStore.");
		}
	}

	@Override
	public Byte getAsByte(final String key) {
		Object value = this.get(key);
		if (value == null) {
			return null;
		} else if (value instanceof Byte) {
			return (Byte) value;
		} else if (value instanceof String) {
			return Byte.valueOf((String) value);
		} else {
			throw new IllegalStateException("Tried to get non-byte value as byte from KVStore.");
		}
	}

	@Override
	public <T> T getAsObject(final String key, final Class<T> objectClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return objectClass.getConstructor().newInstance(this.get(key));
	}

	@Override
	public byte[] getAsBytes(final String columnClassifierObject) {
		return (byte[]) this.get(columnClassifierObject);
	}

	@Override
	public List<Double> getAsDoubleList(final String key) {
		return this.getAsDoubleList(key, DEFAULT_LIST_SEP);
	}

	@Override
	public List<Double> getAsDoubleList(final String key, final String separator) {
		if (this.get(key) == null) {
			return new LinkedList<>();
		}
		return Stream.of(this.getAsString(key).split(separator)).map(Double::valueOf).collect(Collectors.toList());
	}

	@Override
	public List<Integer> getAsIntList(final String key) {
		return this.getAsIntList(key, DEFAULT_LIST_SEP);
	}

	@Override
	public List<Integer> getAsIntList(final String key, final String separator) {
		return Stream.of(this.getAsString(key).split(separator)).map(Integer::valueOf).collect(Collectors.toList());
	}

	@Override
	public List<String> getAsStringList(final String key) {
		return this.getAsStringList(key, DEFAULT_LIST_SEP);
	}

	@Override
	public List<String> getAsStringList(final String key, final String separator) {
		return Stream.of(this.getAsString(key).split(separator)).map(this::trimString).collect(Collectors.toList());
	}

	@Override
	public List<Boolean> getAsBooleanList(final String key) {
		return this.getAsBooleanList(key, DEFAULT_LIST_SEP);
	}

	@Override
	public List<Boolean> getAsBooleanList(final String key, final String separator) {
		return Stream.of(this.getAsString(key).split(separator)).map(Boolean::valueOf).collect(Collectors.toList());
	}

	@Override
	public File getAsFile(final String key) {
		Object value = this.get(key);
		if (value instanceof File) {
			return (File) value;
		} else if (value instanceof String) {
			return new File((String) value);
		} else {
			throw new IllegalStateException("Cannot return value as a file if it is not of that type.");
		}
	}

	/**
	 * Takes a String, trims and returns it.
	 * @param x The string to be trimmed.
	 * @return The trimmed string.
	 */
	private String trimString(final String x) {
		return x.trim();
	}

	/**
	 * Reads a KVStore from a string description.
	 *
	 * @param kvDescription
	 *            The string description of the kv store.
	 */
	public void readKVStoreFromDescription(final String kvDescription) {
		String[] pairSplit = kvDescription.trim().split(";");
		for (String kvPair : pairSplit) {
			String[] kvSplit = kvPair.trim().split("=");
			try {
				if (kvSplit.length == 2) {
					this.put(kvSplit[0], kvSplit[1]);
				} else {
					this.put(kvSplit[0], "");
				}
			} catch (Exception e) {
				LOGGER.error("Could not read kv store from string description", e);
			}
		}
	}

	@Override
	public boolean matches(final Map<String, String> selection) {
		boolean doesNotMatchAllSelectionCriteria = selection.entrySet().stream().anyMatch(x -> {
			boolean isEqual = this.getAsString(x.getKey()).equals(x.getValue());

			if (!x.getValue().contains("*")) {
				return !x.getValue().equals(this.getAsString(x.getKey()));
			}

			String[] exprSplit = x.getValue().split("\\*");
			String currentValue = x.getValue();
			boolean matchesPattern = true;

			for (int i = 0; i < exprSplit.length; i++) {
				if (i == 0 && !x.getValue().startsWith("*") && !currentValue.startsWith(exprSplit[i])) {
					matchesPattern = false;
				}

				if (currentValue.contains(exprSplit[i])) {
					currentValue = currentValue.replaceFirst("(" + exprSplit[i] + ")", "#$#");
					currentValue = currentValue.split("#$#")[1];
				} else {
					matchesPattern = false;
				}

				if (i == (exprSplit.length - 1) && !x.getValue().endsWith("*") && !currentValue.endsWith(exprSplit[i])) {
					matchesPattern = false;
				}

				if (!matchesPattern) {
					break;
				}
			}

			return !isEqual && !matchesPattern;
		});

		return !doesNotMatchAllSelectionCriteria;
	}

	@Override
	public void project(final String[] filterKeys) {
		Set<String> keysToKeep = Arrays.stream(filterKeys).collect(Collectors.toSet());
		Collection<String> keysToRemove = SetUtil.difference(this.keySet(), keysToKeep);
		this.removeAll(keysToRemove.toArray(new String[] {}));
	}

	@Override
	public void removeAll(final String[] removeKeys) {
		Set<String> keysToRemove = Arrays.stream(removeKeys).collect(Collectors.toSet());
		for (String key : keysToRemove) {
			this.remove(key);
		}
	}

	@Override
	public void filter(final Map<String, IKVFilter> filterMap) {
		filterMap.entrySet().stream().forEach(x -> this.filter(x.getKey(), x.getValue()));
	}

	@Override
	public void filter(final String key, final IKVFilter filter) {
		if (!this.containsKey(key)) {
			return;
		}
		this.put(key, filter.filter(this.get(key)));
	}

	/**
	 * Serializes the key value store to a file with the given {@code fileName}.
	 *
	 * @param fileName
	 *            The name of the file, the key value store shall be serialized to.
	 * @throws IOException
	 */
	public void serializeTo(final String fileName) throws IOException {
		this.serializeTo(new File(fileName));
	}

	/**
	 * Serializes the key value store to the {@code file} with the given {@code fileName}.
	 *
	 * @param fileName
	 *            The name of the file, the key value store shall be serialized to.
	 * @throws IOException
	 */
	public void serializeTo(final File file) throws IOException {
		if (file.getParent() != null) {
			file.getParentFile().mkdirs();
		}
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			bw.write(this.toString());
		}
	}

	@Override
	public void merge(final String[] fieldKeys, final String separator, final String newKey) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String fieldKey : fieldKeys) {
			if (first) {
				first = false;
			} else {
				sb.append(separator);
			}
			sb.append(this.getAsString(fieldKey));
			this.remove(fieldKey);
		}
		this.put(newKey, sb.toString());
	}

	@Override
	public void prefixAllKeys(final String prefix) {
		Set<String> keySet = new HashSet<>(this.keySet());
		for (String key : keySet) {
			Object value = this.get(key);
			this.remove(key);
			this.put(prefix + key, value);
		}
	}

	@Override
	public void renameKey(final String key, final String newKeyName) {
		if (this.containsKey(key)) {
			this.put(newKeyName, this.get(key));
			this.remove(key);
		}
	}

	/**
	 * @return Get the collection this KVStore belongs to.
	 */
	public KVStoreCollection getCollection() {
		return this.collection;
	}

	/**
	 * Assigns the KVStore to a KVStoreCollection.
	 *
	 * @param collection
	 *            The collection this KVStore belongs to.
	 */
	public void setCollection(final KVStoreCollection collection) {
		this.collection = collection;
	}

	/**
	 * Allows to get a string representation of this KVStore incorporating only key value pairs for the named
	 *
	 * @param projectionFilter
	 * @return
	 */
	public String getStringRepresentation(final String[] projectionFilter) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String filter : projectionFilter) {
			if (first) {
				first = false;
			} else {
				sb.append(";");
			}
			sb.append(filter + "=" + this.getAsString(filter));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return this.getStringRepresentation(this.keySet().toArray(new String[] {}));
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return super.equals(obj);
	}

	@Override
	public boolean isNull(final String key) {
		return (this.get(key) == null || this.getAsString(key).equals("null"));
	}

}
