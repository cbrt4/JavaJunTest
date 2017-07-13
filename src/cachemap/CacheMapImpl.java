package cachemap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CacheMapImpl<KeyType, ValueType>
        extends HashMap<KeyType, ValueType>
        implements CacheMap<KeyType, ValueType> {

    private final long DEFAULT_TIME_TO_LIVE = 1000;

    private long timeToLive = DEFAULT_TIME_TO_LIVE;

    private HashMap<KeyType, TimeMarkedValue> coreMap = new HashMap<>();

    public CacheMapImpl(int initialCapacity, float loadFactor, long timeToLive) {
        super(initialCapacity, loadFactor);
        this.timeToLive = timeToLive;
    }

    public CacheMapImpl(int initialCapacity, long timeToLive) {
        super(initialCapacity);
        this.timeToLive = timeToLive;
    }

    public CacheMapImpl(long timeToLive) {
        super();
        this.timeToLive = timeToLive;
    }

    public CacheMapImpl() {
        super();
    }

    public CacheMapImpl(Map<? extends KeyType, ? extends ValueType> map, long timeToLive) {
        super(map);
        this.timeToLive = timeToLive;
    }

    public CacheMapImpl(Map<? extends KeyType, ? extends ValueType> map) {
        super(map);
    }

    @Override
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    @Override
    public long getTimeToLive() {
        return this.timeToLive;
    }

    private boolean isExpired(Entry<KeyType, TimeMarkedValue> entry) {
        return Clock.getTime() - entry.getValue().getTimeMark() > getTimeToLive();
    }

    @Override
    public void clearExpired() {
        HashMap<KeyType, TimeMarkedValue> aliveMap = new HashMap<>();
        for (Entry<KeyType, TimeMarkedValue> entry : coreMap.entrySet())
            if (!isExpired(entry))
                aliveMap.put(entry.getKey(), entry.getValue());
        coreMap = aliveMap;
    }

    private final class TimeMarkedValue {

        private ValueType value;
        private long timeMark;

        private TimeMarkedValue(ValueType value, long timeMark) {
            this.value = value;
            this.timeMark = timeMark;
        }

        private long getTimeMark() {
            return timeMark;
        }

        public ValueType getValue() {
            return value;
        }
    }

    public HashMap<KeyType, ValueType> getAsHashMap() {
        HashMap<KeyType, ValueType> hashMap = new HashMap<>();
        for (Entry<KeyType, TimeMarkedValue> entry : coreMap.entrySet()) {
            hashMap.put(entry.getKey(), entry.getValue().getValue());
        }
        return hashMap;
    }

    private CacheMapImpl<KeyType, TimeMarkedValue> getAsCacheMap(Map<? extends KeyType, ? extends ValueType> hashMap) {
        CacheMapImpl<KeyType, TimeMarkedValue> map = new CacheMapImpl<>();
        for (Entry<? extends KeyType, ? extends ValueType> entry : hashMap.entrySet()) {
            map.put(entry.getKey(), new TimeMarkedValue(entry.getValue(), Clock.getTime()));
        }
        return map;
    }

    @Override
    public int size() {
        clearExpired();
        return coreMap.size();
    }

    @Override
    public boolean isEmpty() {
        clearExpired();
        return coreMap.isEmpty();
    }

    @Override
    public ValueType get(Object key) {
        clearExpired();
        return (coreMap.get(key) == null ? null : coreMap.get(key).getValue());
    }

    @Override
    public boolean containsKey(Object key) {
        clearExpired();
        return coreMap.containsKey(key);
    }

    @Override
    public ValueType put(KeyType key, ValueType value) {
        clearExpired();
        coreMap.put(key, new TimeMarkedValue(value, Clock.getTime()));
        return value;
    }

    @Override
    public void putAll(Map<? extends KeyType, ? extends ValueType> map) {
        clearExpired();
        coreMap.putAll(getAsCacheMap(map));
    }

    @Override
    public ValueType remove(Object key) {
        clearExpired();
        return (coreMap.get(key) == null ? null : coreMap.remove(key).getValue());
    }

    @Override
    public void clear() {
        coreMap.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        clearExpired();
        return getAsHashMap().containsValue(value);
    }

    @Override
    public Set<KeyType> keySet() {
        clearExpired();
        return coreMap.keySet();
    }

    @Override
    public Collection<ValueType> values() {
        clearExpired();
        Collection<ValueType> values = new LinkedList<>();
        for (Entry<KeyType, TimeMarkedValue> entry : coreMap.entrySet())
            values.add(entry.getValue().getValue());
        return values;
    }

    @Override
    public Set<Entry<KeyType, ValueType>> entrySet() {
        clearExpired();
        return getAsHashMap().entrySet();
    }

    @Override
    public ValueType getOrDefault(Object key, ValueType defaultValue) {
        clearExpired();
        return coreMap.getOrDefault(key, new TimeMarkedValue(defaultValue, Clock.getTime())).getValue();
    }

    @Override
    public ValueType putIfAbsent(KeyType key, ValueType value) {
        clearExpired();
        return coreMap.putIfAbsent(key, new TimeMarkedValue(value, Clock.getTime())).getValue();
    }

    @Override
    public boolean remove(Object key, Object value) {
        clearExpired();
        return coreMap.containsKey(key) && coreMap.containsValue(value) && coreMap.remove(key, value);
    }

    @Override
    public boolean replace(KeyType key, ValueType oldValue, ValueType newValue) {
        clearExpired();
        if (coreMap.containsKey(key) && coreMap.containsValue(oldValue)) {
            coreMap.replace(key, new TimeMarkedValue(newValue, Clock.getTime()));
            return true;
        }
        return false;
    }

    @Override
    public ValueType replace(KeyType key, ValueType value) {
        clearExpired();
        if (coreMap.containsKey(key)) {
            ValueType oldValue = coreMap.get(key).getValue();
            coreMap.replace(key, new TimeMarkedValue(value, Clock.getTime()));
            return oldValue;
        }
        return null;
    }

    @Override
    public ValueType computeIfAbsent(KeyType key, Function<? super KeyType, ? extends ValueType> mappingFunction) {
        clearExpired();
        ValueType computed = getAsHashMap().computeIfAbsent(key, mappingFunction);
        coreMap.computeIfAbsent(key, keyType -> new TimeMarkedValue(computed, Clock.getTime()));
        return computed;
    }

    @Override
    public ValueType computeIfPresent(KeyType key, BiFunction<? super KeyType, ? super ValueType, ? extends ValueType> remappingFunction) {
        clearExpired();
        ValueType computed = getAsHashMap().computeIfPresent(key, remappingFunction);
        coreMap.computeIfPresent(key, (keyType, timeMarkedValue) -> new TimeMarkedValue(computed, Clock.getTime()));
        return computed;
    }

    @Override
    public ValueType compute(KeyType key, BiFunction<? super KeyType, ? super ValueType, ? extends ValueType> remappingFunction) {
        clearExpired();
        ValueType computed = getAsHashMap().compute(key, remappingFunction);
        coreMap.compute(key, (keyType, timeMarkedValue) -> new TimeMarkedValue(computed, Clock.getTime()));
        return computed;
    }

    @Override
    public ValueType merge(KeyType key, ValueType value, BiFunction<? super ValueType, ? super ValueType, ? extends ValueType> remappingFunction) {
        clearExpired();
        ValueType merged = getAsHashMap().merge(key, value, remappingFunction);
        TimeMarkedValue timeMarkedValue = new TimeMarkedValue(value, Clock.getTime());
        coreMap.merge(key, timeMarkedValue, (timeMarkedValue1, timeMarkedValue2) -> new TimeMarkedValue(merged, Clock.getTime()));
        return merged;
    }

    @Override
    public void forEach(BiConsumer<? super KeyType, ? super ValueType> action) {
        clearExpired();
        HashMap<KeyType, ValueType> hashMap = getAsHashMap();
        hashMap.forEach(action);
        coreMap = getAsCacheMap(hashMap);
    }

    @Override
    public void replaceAll(BiFunction<? super KeyType, ? super ValueType, ? extends ValueType> function) {
        clearExpired();
        HashMap<KeyType, ValueType> hashMap = getAsHashMap();
        hashMap.replaceAll(function);
        coreMap = getAsCacheMap(hashMap);
    }

    @Override
    public Object clone() {
        clearExpired();
        return coreMap.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheMapImpl)) return false;
        if (!super.equals(o)) return false;

        CacheMapImpl<?, ?> cacheMap = (CacheMapImpl<?, ?>) o;

        return DEFAULT_TIME_TO_LIVE == cacheMap.DEFAULT_TIME_TO_LIVE &&
                getTimeToLive() == cacheMap.getTimeToLive() &&
                coreMap.equals(cacheMap.coreMap);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (DEFAULT_TIME_TO_LIVE ^ (DEFAULT_TIME_TO_LIVE >>> 32));
        result = 31 * result + (int) (getTimeToLive() ^ (getTimeToLive() >>> 32));
        result = 31 * result + coreMap.hashCode();
        return result;
    }

    @Override
    public String toString() {
        clearExpired();
        return getAsHashMap().toString();
    }
}
