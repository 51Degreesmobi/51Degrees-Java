package fiftyone.device.proto.example;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fiftyone.mobile.detection.Dataset;
import fiftyone.mobile.detection.Match;
import fiftyone.mobile.detection.Provider;
import fiftyone.mobile.detection.cache.ICache;
import fiftyone.mobile.detection.cache.IPutCache;
import fiftyone.mobile.detection.cache.ILoadingCache;
import fiftyone.mobile.detection.cache.IValueLoader;
import fiftyone.mobile.detection.factories.StreamFactory;

import java.io.IOException;
import java.util.Date;

import static fiftyone.properties.CacheConstants.CacheType.NodesCache;
import static fiftyone.properties.CacheConstants.CacheType.ProfilesCache;
import static fiftyone.properties.CacheConstants.NODES_CACHE_SIZE;
import static fiftyone.properties.CacheConstants.PROFILES_CACHE_SIZE;

/**
 * @author jo
 */
class GuavaExample {

    public static class CacheAdaptor <K,V>  implements ICache<K,V> {
        protected final Cache<K,V> cache;

        public CacheAdaptor(Cache<K,V> cache) {
            this.cache = cache;
        }

        @Override
        public V get(K key) {
            return cache.getIfPresent(key);
        }

        @Override
        public long getCacheSize() {
            return cache.size();
        }

        @Override
        public long getCacheMisses() {
            return cache.stats().missCount();
        }

        @Override
        public long getCacheRequests() {
            return cache.stats().requestCount();
        }

        @Override
        public double getPercentageMisses() {
            return getCacheMisses()/getCacheRequests();
        }

        @Override
        public void resetCache() {
            cache.invalidateAll();
        }
    }

    public static class PutCacheAdaptor<K,V> extends CacheAdaptor<K,V> implements IPutCache<K,V>{

        public PutCacheAdaptor(Cache<K, V> cache) {
            super(cache);
        }

        @Override
        public void put(K key, V value) {
            cache.put(key, value);
        }
    }

    public static class UaCacheAdaptor <K,V> extends CacheAdaptor<K,V> implements ILoadingCache<K,V> {

        public UaCacheAdaptor(com.google.common.cache.Cache<K,V> cache) {
            super(cache);
        }

        @Override
        public V get(K key, IValueLoader<K, V> loader) throws IOException {
            V result = get(key);
            if (result == null) {
                result = loader.load(key);
                if (result != null) {
                    cache.put(key, result);
                }
            }
            return result;
        }

        @Override
        public V get(K key) {
            return cache.getIfPresent(key);
        }
    }


    public static void main (String[] args) throws IOException {
        com.google.common.cache.Cache uaCache = CacheBuilder.newBuilder()
                .initialCapacity(100000)
                .maximumSize(100000)
                .concurrencyLevel(5) // set to number of threads that can access cache at same time
                .build();

        com.google.common.cache.Cache nodeCache = CacheBuilder.newBuilder()
                .initialCapacity(NODES_CACHE_SIZE)
                .maximumSize(NODES_CACHE_SIZE)
                .concurrencyLevel(5)
                .build();

        com.google.common.cache.Cache profileCache = CacheBuilder.newBuilder()
                .initialCapacity(PROFILES_CACHE_SIZE)
                .maximumSize(PROFILES_CACHE_SIZE)
                .concurrencyLevel(5)
                .build();

        Dataset dataset = new StreamFactory.Builder()
                .addCache(NodesCache, new PutCacheAdaptor(nodeCache))
                .addCache(ProfilesCache, new PutCacheAdaptor(profileCache))
                .lastModified(new Date())
                .build("data/51Degrees-LiteV3.2.dat");

        Provider provider = new Provider(dataset, new UaCacheAdaptor(uaCache));

        Match match = provider.match("Hello World");
        System.out.printf("%s", match.getSignature());
    }
}
