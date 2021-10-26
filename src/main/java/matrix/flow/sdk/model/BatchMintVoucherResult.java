package matrix.flow.sdk.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class BatchMintVoucherResult {
    private String transactionId;
    private List<VoucherMetadataModel> tokens;
}
