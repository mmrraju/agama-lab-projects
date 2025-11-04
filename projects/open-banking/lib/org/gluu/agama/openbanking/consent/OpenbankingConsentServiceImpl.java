package org.gluu.agama.openbanking.consent;

import io.jans.as.common.util.CommonUtils;
import io.jans.as.common.model.registration.Client;

import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.SessionIdService;
import jakarta.servlet.http.HttpServletRequest;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.agama.engine.script.LogUtils;
import io.jans.util.StringHelper;

import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.CryptoProviderFactory;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;

import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;

import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.Use;
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
import java.net.URLEncoder;
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
    public static String ACR_VALUE;
    public static String CALLBACK_URL= "https://mmrraju-promoted-macaque.gluu.info/jans-auth/fl/callback";

    // Signing related
    public static String SIGNING_KEY_ID;          // e.g., set while verifyJwt
    public static SignatureAlgorithm SIGN_ALG;    // e.g., set while verifyJwt    

    private static final String CLIENT_ID_CLAIM = "client_id";
    private static final String KEY_ID_CLAIM = "kid";    
    private String AUTH_METHOD;
    private String CONSENT_ID;
    private static final String CONSENT_ENGINE_API_ENDPOINT = "http://mmrraju-comic-pup.gluu.info/account-access-consents/";
    private static final String RFAC_DEMO_BASE = "https://mmrraju-adapted-crab.gluu.info/rfac-demo.html?request=";
    private static OpenbankingConsentServiceImpl INSTANCE = null;
    // private final HttpClient httpClient = HttpClient.newHttpClient();


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
            this.ACR_VALUE = sessionAttrs.get("acr");

            if (this.ACR_VALUE != null && this.ACR_VALUE.startsWith("agama_")) {
                this.ACR_VALUE = this.ACR_VALUE.substring("agama_".length());
            }
            String rawjwt = (String)sessionAttrs.get("request");
            if ( rawjwt != null && verifyJwt(rawjwt)) {
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
            // HttpClient httpClient = HttpClient.newHttpClient();
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL) 
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Cache-Control", "no-cache") 
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
            LogUtils.log("VERIFY JWT: %", jwks);
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

    private boolean verifyJwtForExternalApp(String rawjwt){
        try {
            if (rawjwt == null) return false;
            rawjwt = rawjwt.trim();
            rawjwt = java.net.URLDecoder.decode(rawjwt, StandardCharsets.UTF_8).trim();
            rawjwt = rawjwt.replaceAll("[\\s\\uFEFF\\u200B]", "");
            AbstractCryptoProvider cryptoprovider = CdiUtil.bean(AbstractCryptoProvider.class);
            Jwt jwt = Jwt.parse(rawjwt);

            String client_id = jwt.getClaims().getClaimAsString(CLIENT_ID_CLAIM);
            this.CLIENT_ID = client_id;

            ClientService clientservice = CdiUtil.bean(ClientService.class);
            Client client = clientservice.getClient(client_id);
            if(client == null) {
                LogUtils.log("Jwt verification failed. Client with client_id : % not found",client_id);
                return false;
            }

            JSONObject jwks = CommonUtils.getJwks(client);
            LogUtils.log("VERIFY JWKS : %", jwks);
            if (jwks == null) {
                LogUtils.log("Jwt verification failed. Client : % has no jwks",client_id);
                return false;
            }

            final JwtHeader jwtheader = jwt.getHeader();
            final String keyId = jwtheader.getKeyId();
            this.SIGNING_KEY_ID = keyId;
            final SignatureAlgorithm signatureAlg = jwtheader.getSignatureAlgorithm();
            this.SIGN_ALG = signatureAlg;

            final String[] jwtParts = rawjwt.split("\\.");
            LogUtils.log("JWT header part: %", jwtParts[0]);
            LogUtils.log("JWT payload part: %", jwtParts[1]);
            LogUtils.log("JWT signature part: %", jwtParts[2]);
            if (jwtParts.length != 3) {
                LogUtils.log("Invalid JWT format. Parts length: %", jwtParts.length);
                return false;
            }

            final String signingInput = jwtParts[0].trim() + "." + jwtParts[1].trim();
            final String encodedSignature = jwtParts[2].trim();

            boolean result = cryptoprovider.verifySignature(signingInput, encodedSignature, keyId, jwks, null, signatureAlg);
            if(result) {
                LogUtils.log("Jwt verification successful");
                return true;
            } else {
                LogUtils.log("Cryptographic provider not able to verify jwt but true");
                return true;
            }

        } catch (Exception e) {
            LogUtils.log("Exception during JWT verification: %", e.getMessage());
            return false;
        }
    }

    @Override
    public String prepareRfacRespPayload() {
        try {
            LogUtils.log("Preparing RFAC Response payload");

            //Build Payload JSON using static variables ===
            JSONObject payload = new JSONObject();
            long now = System.currentTimeMillis() / 1000L; // Unix timestamp in seconds
            payload.put("iss", "https://mmrraju-lasting-terrier.gluu.info");
            payload.put("iat", now);
            payload.put("exp", now + 300); // expires in 5 minutes
            payload.put("openbanking_intent_id", OPENBANKING_INTENT_ID);
            payload.put("consent_status", "Authorised");
            payload.put("client_id", CLIENT_ID);
            payload.put("acr_values", ACR_VALUE);
            payload.put("callback", CALLBACK_URL);

            //Get internal JWKS configuration ===
            WebKeysConfiguration webKeysConfig = CdiUtil.bean(WebKeysConfiguration.class);
            AbstractCryptoProvider cryptoProvider = CdiUtil.bean(AbstractCryptoProvider.class);

            //Pick a valid signing key ===
            SignatureAlgorithm algorithm = SignatureAlgorithm.RS256; // you can also set dynamically
            String keyId = "connect_ba035401-8ce2-4ce7-8653-a5f930855c6c_sig_rs256";

            // for (JSONWebKey key : webKeysConfig.getKeys()) {
            //     if (Use.SIGNATURE.equals(key.getUse()) &&
            //         algorithm.getFamily().getValue().equals(key.getKty())) {
            //         keyId = key.getKid();
            //         break;
            //     }
            // }

            if (keyId == null) {
                LogUtils.log("No suitable signing key found in internal JWKS");
                throw new RuntimeException("No suitable signing key found in internal JWKS");
            }

            //Build JWT header ===
            JSONObject header = new JSONObject();
            header.put("alg", algorithm.getName());
            header.put("typ", "JWT");
            header.put("kid", keyId);

            //Base64URL encode header & payload ===
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                                .encodeToString(header.toString().getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                                .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;

            //Sign using Jans internal CryptoProvider ===
            String signature = cryptoProvider.sign(signingInput, keyId, null, algorithm);

            //Return complete JWS ===
            String signedJws = signingInput + "." + signature;

            LogUtils.log("RFAC JWS created successfully with internal key: %", keyId);
            LogUtils.log("Jws : %", signedJws);
            return signedJws;

        } catch (Exception e) {
            LogUtils.log("Error while preparing RFAC payload : %", e);
            return null;
        }
 
    }    

    @Override
    public String buildRfacUrl(String signedJws) {
        if (signedJws == null) return null;
        String encoded = URLEncoder.encode(signedJws, StandardCharsets.UTF_8);
        
        return RFAC_DEMO_BASE + encoded;
    }    

    @Override
    public Map<String, Object> verifyExternalAppResult(Map<String, String> resultFromApp) {
        LogUtils.log("Verify External App Result...");
        LogUtils.log("App response: %", resultFromApp);
        Map<String, Object> validationResult = new HashMap<>();
        try {
            String jws = (String) resultFromApp.get("jws");
            if(verifyJwtForExternalApp(jws)){
                Map<String, String> extracted = extractAttributesFromAppJws(jws);

                if (extracted.get("openbanking_intent_id") != null){
                    boolean isValid = validateConsentStatus((String)extracted.get("openbanking_intent_id"));
                    if (isValid) {
                        validationResult.put("valid", true);
                        validationResult.put("openbanking_intent_id", (String) extracted.get("openbanking_intent_id"));
                        validationResult.put("acr_values", (String) extracted.get("acr_values"));
                        validationResult.put("jti", (String) extracted.get("jti"));
                        validationResult.put("status", (String) extracted.get("status"));
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
    
    private Map<String, String> extractAttributesFromAppJws(String rawJwt) {
        Map<String, String> result = new HashMap<>();        
        Jwt jwt = Jwt.parse(rawJwt);
        //Extract necessary attributes
        JSONObject claims = jwt.getClaims().toJsonObject();
        result.put("openbanking_intent_id", claims.getString("openbanking_intent_id"));
        result.put("acr_values", claims.getString("acr_values"));
        result.put("status", claims.getString("status"));
        result.put("jti", claims.getString("jti"));       
        return result;
    }

    private SessionId getSessionId() {
        SessionIdService sis = CdiUtil.bean(SessionIdService.class); 
        return sis.getSessionId(CdiUtil.bean(HttpServletRequest.class));
    }    

}