package matrix.flow.sdk;

import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import lombok.extern.log4j.Log4j2;
import matrix.flow.sdk.model.BatchMintVoucherResult;
import matrix.flow.sdk.model.VoucherClientConfig;
import matrix.flow.sdk.model.VoucherMetadataModel;

@Log4j2
public class VoucherMinterClientPool {
    private final GenericObjectPool<VoucherClient> objectPool;


    public VoucherMinterClientPool(final int keyStartIndex, final int keyCapacity,
            final VoucherClientConfig minterClientBaseConfig) {

        final VoucherClientPoolFactory voucherClientPoolFactory =
                new VoucherClientPoolFactory(minterClientBaseConfig, keyStartIndex, keyCapacity);
        final GenericObjectPoolConfig<VoucherClient> objectPoolConfig =
                new GenericObjectPoolConfig<>();
        log.info("Init VoucherMinterClientPool with global key index", keyStartIndex, "keyCapacity",
                keyCapacity);
        objectPoolConfig.setMaxTotal(keyCapacity);
        objectPoolConfig.setMaxIdle(keyCapacity);
        objectPoolConfig.setMaxWaitMillis(120000); // FIXME: how to proper configure this from
                                                   // external
        objectPoolConfig.setBlockWhenExhausted(true);
        objectPoolConfig.setTestOnBorrow(true);
        objectPoolConfig.setTestOnCreate(true);
        // Build pool
        this.objectPool = new GenericObjectPool<>(voucherClientPoolFactory, objectPoolConfig);
    }

    /**
     * Very user composite signatures
     *
     * @param message raw message in hex
     * @param accountAddress account address
     * @param keyIds keys ids corresponding to signatures
     * @param signatures signed using above keys
     *
     * @return true means verified successfully
     */
    public boolean verifyUserSignatureCadence(final String message, final String accountAddress,
            final List<Integer> keyIds, final List<String> signatures) {
        VoucherClient client = null;
        try {
            client = objectPool.borrowObject();
            log.info(String.format(
                    "[VoucherMinterClientPool.batchMint] use key index %d to send mint transaction",
                    client.getAccountKeyIndex()));
            return client.verifyUserSignatureCadence(message, accountAddress, keyIds, signatures);
        } catch (final Exception e) {
            log.error("[VoucherMinterClientPool.batchMintVoucher] failed with", e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                objectPool.returnObject(client);
            }
        }
    }

    public BatchMintVoucherResult batchMintAndResolveVoucher(final List<String> recipientList,
            final List<String> landInfoHashStringList) {

        VoucherClient client = null;
        String transactionId = "";
        List<VoucherMetadataModel> tokens = null;
        try {
            client = objectPool.borrowObject();
            log.info(String.format(
                    "[VoucherMinterClientPool.batchMint] use key index %d to send mint transaction",
                    client.getAccountKeyIndex()));
            transactionId = client.batchMintVoucher(recipientList, landInfoHashStringList);
            tokens = client.resolveBatchMintVoucherTransaction(transactionId);
            return BatchMintVoucherResult.builder().transactionId(transactionId).tokens(tokens)
                    .build();
        } catch (final Exception e) {
            log.error("[VoucherMinterClientPool.batchMintVoucher] failed with", e);
            if (transactionId.equals("")) {
                throw new RuntimeException(e);
            }
            return BatchMintVoucherResult.builder().transactionId(transactionId).tokens(tokens)
                    .build();
        } finally {
            if (client != null) {
                objectPool.returnObject(client);
            }
        }
    }

    public String batchMintVoucher(final List<String> recipientList,
            final List<String> landInfoHashStringList) {

        VoucherClient client = null;
        try {
            client = objectPool.borrowObject();
            log.info(String.format(
                    "[VoucherMinterClientPool.batchMint] use key index %d to send mint transaction",
                    client.getAccountKeyIndex()));
            return client.batchMintVoucher(recipientList, landInfoHashStringList);
        } catch (final Exception e) {
            log.error("[VoucherMinterClientPool.batchMintVoucher] failed with", e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                objectPool.returnObject(client);
            }
        }
    }

    public List<VoucherMetadataModel> resolveBatchMintVoucher(final String transactionId) {

        VoucherClient client = null;
        try {
            client = objectPool.borrowObject();
            log.info(String.format(
                    "[VoucherMinterClientPool.resolveBatchMintVoucher] use key index %d to resolve transactionId %s",
                    client.getAccountKeyIndex(), transactionId));
            return client.resolveBatchMintVoucherTransaction(transactionId);
        } catch (final Exception e) {
            log.error("[VoucherMinterClientPool.resolveBatchMintVoucher] failed", e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                objectPool.returnObject(client);
            }
        }
    }

    public VoucherMetadataModel mintVoucher(final String recipient,
            final String landInfoHashString) {

        VoucherClient client = null;
        try {
            client = objectPool.borrowObject();
            log.info(String.format("[VoucherMinterClientPool.mintVoucher] use key index %d to mint",
                    client.getAccountKeyIndex()));
            return client.mintVoucher(recipient, landInfoHashString);
        } catch (final Exception e) {
            log.error("[VoucherMinterClientPool.mintVoucher] failed with", e);
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                objectPool.returnObject(client);
            }
        }
    }

    public void close() {
        this.objectPool.close();
    }

}
