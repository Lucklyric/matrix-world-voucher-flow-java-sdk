package matrix.flow.sdk;

import java.util.concurrent.ConcurrentLinkedQueue;
import com.nftco.flow.sdk.impl.FlowAccessApiImpl;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import lombok.extern.log4j.Log4j2;
import matrix.flow.sdk.model.VoucherClientConfig;

@Log4j2
public final class VoucherClientPoolFactory extends BasePooledObjectFactory<VoucherClient> {
    private final ConcurrentLinkedQueue<Integer> keyIndexQueue =
            new ConcurrentLinkedQueue<Integer>();

    private final VoucherClientConfig clientConfig;

    public VoucherClientPoolFactory(final VoucherClientConfig clientConfig, final int keyStartIndex,
            final int keyCapacity) {
        this.clientConfig = clientConfig;

        // Fulfill keyIndex
        final int keyEndIndex = keyStartIndex + keyCapacity;
        log.info(String.format("Create pool factory from global keyIndex %d to %d", keyStartIndex,
                keyEndIndex));
        for (int keyIndex = keyStartIndex; keyIndex < keyEndIndex; keyIndex++) {
            this.keyIndexQueue.add(keyIndex);
        }
    }

    public VoucherClient create() throws Exception {
        final Integer key = this.keyIndexQueue.poll();
        if (key == null){
            throw new RuntimeException("Key pool exhausted");
        }
        log.info(String.format("Create object with key %d", key));
        final VoucherClientConfig localConfig =
                this.clientConfig.toBuilder().keyIndex(key).build();
        return new VoucherClient(localConfig);
    }

    public PooledObject<VoucherClient> wrap(final VoucherClient client) {
        return new DefaultPooledObject<>(client);
    }

    @Override
    public void destroyObject(final PooledObject<VoucherClient> client) throws Exception {
        log.info(String.format("Destroy object with key %d", client.getObject().getAccountKeyIndex()));
        ((FlowAccessApiImpl) client.getObject().accessAPI).close();
        client.getObject().accessAPI.wait();
        final int keyIndex = client.getObject().getAccountKeyIndex();
        super.destroyObject(client);
        this.keyIndexQueue.add(keyIndex);
    }

    @Override
    public boolean validateObject(PooledObject<VoucherClient> client) {
        try {
            log.info(String.format("Validate client with key %d", client.getObject().getAccountKeyIndex()));
            client.getObject().accessAPI.ping();
            return true;
        } catch (Exception e) {
            log.warn("Validate failed");
            return false;
        }
    }

}
