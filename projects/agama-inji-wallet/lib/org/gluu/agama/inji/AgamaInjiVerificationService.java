package org.gluu.agama.inji;

import java.util.HashMap;
import java.util.Map;

import org.gluu.agama.inji.AgamaInjiVerificationServiceImpl;

public abstract class AgamaInjiVerificationService{

    public abstract Map<String, Object> createVpVerificationRequest();

    public abstract String buildInjiWebAuthorizationUrl(String requestId, String transactionId);

    // public abstract Map<String, Object> verifyInjiAppResult(Map<String, String> resultFromapp, String requestId, String transactionId);
    public abstract Map<String, Object> verifyInjiAppResult(String requestId, String transactionId);

    public abstract Map<String, String> onboardUser();

    public static AgamaInjiVerificationService getInstance(HashMap config){
        return AgamaInjiVerificationServiceImpl.getInstance(config);
    }
}
