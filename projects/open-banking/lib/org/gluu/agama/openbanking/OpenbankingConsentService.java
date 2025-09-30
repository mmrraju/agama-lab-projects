package lib.org.gluu.agama.openbanking;

import java.util.Map;

import lib.org.gluu.agama.openbanking.consent.OpenbankingConsentServiceImpl;

public abstract class OpenbankingConsentService {

    public abstract Map<String, Object> verifyExternalAppResult(Map<String, Object> resultFromApp);

    public abstract String prepareRfacRespPayload();

    public abstract Map<String, Object> validateConsent(Map<String, Object> reqObject);

    public abstract boolean sendRFACResponseToApp();


    public static OpenbankingConsentService getInstance(){
        
        return OpenbankingConsentServiceImpl.getInstance();
    }
}
