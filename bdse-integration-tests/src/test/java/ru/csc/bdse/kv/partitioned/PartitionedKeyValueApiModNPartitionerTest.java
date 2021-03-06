package ru.csc.bdse.kv.partitioned;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import ru.csc.bdse.kv.InMemoryKeyValueApi;
import ru.csc.bdse.kv.KeyValueApi;
import ru.csc.bdse.partitioning.ModNPartitioner;
import ru.csc.bdse.partitioning.Partitioner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PartitionedKeyValueApiModNPartitionerTest extends AbstractPartitionedKeyValueApiHttpClientTest {
    private static Map<String, KeyValueApi> shardsForCluster1;
    private static Map<String, KeyValueApi> shardsForCluster2;
    private static Partitioner partitionerForCluster1;
    private static Partitioner partitionerForCluster2;
    private static Set<String> keys = Stream.generate(() -> RandomStringUtils.randomAlphanumeric(10)).limit(1000)
            .collect(Collectors.toSet());

    @BeforeClass
    public static void setUp() {
        KeyValueApi[] innerNodes = new KeyValueApi[5];
        for (int i = 0; i < 5; i++) {
            innerNodes[i] = new InMemoryKeyValueApi("node" + i);
        }
        shardsForCluster1 = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            shardsForCluster1.put("node" + i, innerNodes[i]);
        }
        shardsForCluster2 = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            shardsForCluster2.put("node" + i, innerNodes[i]);
        }
        partitionerForCluster1 = new ModNPartitioner(shardsForCluster1.keySet());
        partitionerForCluster2 = new ModNPartitioner(shardsForCluster2.keySet());
    }

    @Override
    protected KeyValueApi newCluster1() {
        return new ShardsCoordinator(new ShardsConfiguration(
                shardsForCluster1,
                3000,
                partitionerForCluster1
        ));
    }

    @Override
    protected KeyValueApi newCluster2() {
        return new ShardsCoordinator(new ShardsConfiguration(
                shardsForCluster2,
                3000,
                partitionerForCluster2
        ));
    }

    @Override
    protected Set<String> keys() {
        return keys;
    }

    @Override
    protected float estimatedKeyLossProbability() {
        return 0.8f;
    }

    @Override
    protected float expectedKeysLossProportion() {
        int cntChangedPartitionKeys = 0;
        for (String key : keys) {
            if (!partitionerForCluster1.getPartition(key).equals(partitionerForCluster2.getPartition(key))) {
                cntChangedPartitionKeys++;
            }
        }
        return (float) cntChangedPartitionKeys / keys.size();
    }

    @Override
    protected float expectedUndeletedKeysProportion() {
        return expectedKeysLossProportion();
    }
}
