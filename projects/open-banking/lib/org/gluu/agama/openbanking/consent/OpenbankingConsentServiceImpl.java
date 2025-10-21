package org.gluu.agama.openbanking.consent;

import io.jans.as.common.util.CommonUtils;
import io.jans.as.common.model.registration.Client;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.SessionIdService;
import jakarta.servlet.http.HttpServletRequest;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.agama.engine.script.LogUtils;
import io.jans.util.StringHelper;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.CryptoProviderFactory;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;

import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;

import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.server.service.ClientService;

import io.jans.util.security.StringEncrypter.EncryptionException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.Date;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.io.*;
import java.util.Base64;


import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.gluu.agama.openbanking.OpenbankingConsentService;

public class OpenbankingConsentServiceImpl extends OpenbankingConsentService {

    // Static variables to set externally before calling the method
    // ----------------------
    public static String OPENBANKING_INTENT_ID;
    public static String CLIENT_ID;
    public static String ACR_VALUE = "acr";
    public static String CALLBACK_URL= "https://<your-server-name>/jans-auth/fl/callback";

    // Signing related
    public static String SIGNING_KEY_ID;          // e.g., set while verifyJwt
    public static SignatureAlgorithm SIGN_ALG;    // e.g., set while verifyJwt    

    private static final String CLIENT_ID_CLAIM = "client_id";
    private static final String KEY_ID_CLAIM = "kid";    
    private String AUTH_METHOD;
    private String CONSENT_ID;
    private static final String CONSENT_ENGINE_API_ENDPOINT = "http://mmrraju-trusting-locust.gluu.info/account-access-consents/";
    private static OpenbankingConsentServiceImpl INSTANCE = null;
    private final HttpClient httpClient = HttpClient.newHttpClient();


    public OpenbankingConsentServiceImpl(){
    }

    public static synchronized OpenbankingConsentServiceImpl getInstance()
    {
        
        if (INSTANCE == null)
            INSTANCE = new OpenbankingConsentServiceImpl();
        return INSTANCE;
    }

    @Override
    public Map<String, Object> validateConsent() {
        try {
            Map<String, Object> validationResult = new HashMap<>();
            LogUtils.log("Retrieve request object from session...");
            Map<String, String> sessionAttrs = getSessionId().getSessionAttributes();
            LogUtils.log(sessionAttrs);

            String rawjwt = (String)sessionAttrs.get("request");
            if (verifyJwt(rawjwt)) {
                // Retrieve openbanking_intent_id from Jwt payload.
                String intentId = extractOpenBankingIntentId(rawjwt);
                if (intentId != null) {
                    this.OPENBANKING_INTENT_ID = intentId;
                    LogUtils.log("Extracted openbanking_intent_id: %", intentId);

                    boolean isValid = validateConsentStatus(intentId);
                    if (isValid) {
                        LogUtils.log("Consent validation successful for intentId: %", intentId);
                        validationResult.put("valid", true);
                        validationResult.put("message", "Consent validation successful for intentId");  
                        return validationResult;                         
                    } else {
                        LogUtils.log("Consent validation failed for intentId: %", intentId);
                        validationResult.put("valid", false);
                        validationResult.put("message", "Consent validation failed for intentId");  
                        return validationResult;                          
                    }
                } else {
                    LogUtils.log("openbanking_intent_id not found in JWT");
                    validationResult.put("valid", false);
                    validationResult.put("message", "openbanking_intent_id or consent_id not found in JWT");  
                    return validationResult;                    
                }               

            }else{
                validationResult.put("valid", false);
                validationResult.put("message", "Jwt verification failed.");  
                return validationResult;              
            }
            

        } catch (Exception e) {
            LogUtils.log("Error: %", e);
        }
    }

    private boolean validateConsentStatus(String intentId) {
        try {
            LogUtils.log("Validating consent for intentId: %", intentId);

            String apiUrl = this.CONSENT_ENGINE_API_ENDPOINT + intentId;
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LogUtils.log("ERROR: Consent API returned status code: %", response.statusCode());
                return false;
            }

            String jsonResponse = response.body();
            LogUtils.log("Consent Engine Response: %", jsonResponse);

            // Parse JSON using your existing ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> consentMap = mapper.readValue(jsonResponse, Map.class);
            Map<String, Object> data = (Map<String, Object>) consentMap.get("Data");

            if (data == null || !data.containsKey("Status")) {
                LogUtils.log("ERROR: Missing 'Data.Status' field in consent response");
                return false;
            }

            String status = (String) data.get("Status");
            LogUtils.log("Consent status: %", status);

            return "Authorised".equalsIgnoreCase(status);

        } catch (Exception e) {
            LogUtils.log("Exception while validating consent status: %", e);
            return false;
        }
    }

    private String extractOpenBankingIntentId(String rawjwt) {
    try {
        Jwt jwt = Jwt.parse(rawjwt);
        // Navigate through nested claims structure
        JSONObject claims = jwt.getClaims().toJsonObject();
        // The attribute is nested like: claims -> userinfo -> openbanking_intent_id -> value
        JSONObject userInfo = claims.getJSONObject("claims")
                                   .getJSONObject("userinfo");

        JSONObject intentObject = userInfo.getJSONObject("openbanking_intent_id");
        String intentId = intentObject.getString("value");
        return intentId;

    } catch (Exception e) {
        LogUtils.log("Error extracting openbanking_intent_id: %", e);
        return null;
    }
    }

    private boolean verifyJwt(String rawjwt) {
        try {
            //AppConfiguration appconfig = CdiUtil.bean(AppConfiguration.class);
            AbstractCryptoProvider cryptoprovider = CdiUtil.bean(AbstractCryptoProvider.class);
            Jwt jwt = Jwt.parse(rawjwt);
            String client_id = jwt.getClaims().getClaimAsString(CLIENT_ID_CLAIM);
            this.CLIENT_ID = client_id;
            ClientService clientservice  = CdiUtil.bean(ClientService.class);
            Client client = clientservice.getClient(client_id);
            if(client == null) {
                LogUtils.log("Jwt verification failed. Client with client_id : % not found",client_id);
                return false;
            }
            String clientsecret = clientservice.decryptSecret(client.getClientSecret());
            JSONObject jwks = CommonUtils.getJwks(client);
            if (jwks == null) {
                LogUtils.log("Jwt verification failed. Client : % has no jwks",client_id);
                return false;
            }
            final JwtHeader jwtheader = jwt.getHeader();
            final String keyId = jwtheader.getKeyId();
            this.SIGNING_KEY_ID = keyId;
            final SignatureAlgorithm signatureAlg = jwtheader.getSignatureAlgorithm();
            this.SIGN_ALG = signatureAlg;
            final String [] jwtParts = rawjwt.split("\\.");
            final String signingInput = jwtParts[0] + "." + jwtParts[1];
            final String encodedSignature = jwtParts[2];
            final boolean result = cryptoprovider.verifySignature(signingInput,encodedSignature,keyId,jwks,clientsecret,signatureAlg);
            if(result) {
                LogUtils.log("Jwt verification successfull");
                return true;
            }else {
                LogUtils.log("Jwt verification failed. Cryptographic provider failed to validate the jwt");
                return false;
            }            
        } catch (Exception e) {
            LogUtils.log("Exception : %", e);
        }
    }

    @Override
    public String prepareRfacRespPayload() {
        try {
            LogUtils.log("Preparing RFAC Response payload");

            //Build payload JSON using static variables
            JSONObject payload = new JSONObject();
            long now = System.currentTimeMillis() / 1000L; // Unix timestamp in seconds
            payload.put("iss", "https://mmrraju-lasting-terrier.gluu.info");
            payload.put("iat", now);
            payload.put("exp", now + 300); // expires in 5 min
            payload.put("openbanking_intent_id", OPENBANKING_INTENT_ID);
            payload.put("consent_status", "Authorised");
            payload.put("client_id", CLIENT_ID);
            payload.put("acr_values", ACR_VALUE);
            payload.put("callback", CALLBACK_URL);

            // Build JWT header using static signing variables
            JSONObject header = new JSONObject();
            header.put("alg", SIGN_ALG.getName());
            header.put("typ", "JWT");
            header.put("kid", SIGNING_KEY_ID);

            // Encode header and payload (Base64 URL)
            // String encodedHeader = Base64.getUrlEncoder().withoutPadding()
            //                     .encodeToString(header.toString().getBytes(StandardCharsets.UTF_8));
            // String encodedPayload = Base64.getUrlEncoder().withoutPadding()
            //                     .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
            String encodedHeader = Base64Util.base64urlencode(header.toString().getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64Util.base64urlencode(payload.toString().getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedPayload;

            // Sign using AbstractCryptoProvider
            AbstractCryptoProvider cryptoProvider = CdiUtil.bean(AbstractCryptoProvider.class);
            String signature = cryptoProvider.sign(signingInput, SIGNING_KEY_ID, null, SIGN_ALG);

            //  Return complete JWS
            return signingInput + "." + signature;             
        } catch (Exception e) {
            LogUtils.log("Getting error while praparing RFAC payload %",e);
        }
 
    }    

    @Override
    public Map<String, Object> verifyExternalAppResult(Map<String, String> resultFromApp) {
        LogUtils.log("Verify External App Result...");
        Map<String, Object> validationResult = new HashMap<>();
        try {
            String jws = (String) resultFromApp.get("requst");
            if(verifyJwt(jws)){
                Map<String, Object> extracted = extractAttributesFromAppJws(jws);

                if (extracted.get("consentId") != null){
                    boolean isValid = validateConsentStatus((String)extracted.get("consentId"));
                    if (isValid) {
                        validationResult.put("valid", true);
                        validationResult.put("consentId", (String) extracted.get("consentId"));
                        validationResult.put("authMethod", (String) extracted.get("authMethod"));
                        validationResult.put("transactionId", (String) extracted.get("transactionId"));
                        validationResult.put("message", "External app result verify succssful");
                        return validationResult;
                        
                    }else{
                        validationResult.put("valid", false);
                        validationResult.put("message", "ConsentId is not valid");
                        return validationResult;                         
                    }
                }else{
                    validationResult.put("valid", false);
                    validationResult.put("message", "ConsentId not found");
                    return validationResult;                    
                }

            }else{
                validationResult.put("valid", false);
                validationResult.put("message", "Invalid JWS signature from App ");
                return validationResult;
            }

        } catch (Exception e) {
            validationResult.put("valid", false);
            validationResult.put("message", "Exception parsing JWS: " + e.getMessage());
            return validationResult;
        }

        return validationResult;
    }
    
    private Map<String, Object> extractAttributesFromAppJws(String rawJwt) {
        Map<String, Object> result = new HashMap<>();        
        Jwt jwt = Jwt.parse(rawJwt);
        //Extract necessary attributes
        JSONObject claims = new JSONObject(jwt.getClaims().toJson());
        result.put("consentId", claims.getString("consentId"));
        result.put("authMethod", claims.getString("authMethod"));
        if (claims.has("transactionId")) {
            result.put("transactionId", claims.getString("transactionId"));
        }        
        return result;
    }

    private SessionId getSessionId() {
        SessionIdService sis = CdiUtil.bean(SessionIdService.class); 
        return sis.getSessionId(CdiUtil.bean(HttpServletRequest.class));
    }    

}
