package org.gluu.agama.openbanking.consent;

// import io.jsonwebtoken.*;
// import io.jsonwebtoken.security.Keys;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.agama.engine.script.LogUtils;
import io.jans.util.StringHelper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.PublicKey;
// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jws;
import java.util.UUID;
import java.util.Date;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.io.*;


import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.gluu.agama.openbanking.OpenbankingConsentService;

public class OpenbankingConsentServiceImpl extends OpenbankingConsentService {
    private String transactionalId;
    private static final String AUTH_METHOD = "urn:openbanking:psd2:sca";
    private String CONSENT_ID;
    private static final String CONSENT_ENGINE_API_ENDPOINT = "https://consent-engine.example.com/api/consents/";
    private static OpenbankingConsentServiceImpl INSTANCE = null;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
    private ObjectMapper mapper = new ObjectMapper(); 

    public OpenbankingConsentServiceImpl(){
    }

    public static synchronized OpenbankingConsentServiceImpl getInstance()
    {
        
        if (INSTANCE == null)
            INSTANCE = new OpenbankingConsentServiceImpl();
        return INSTANCE;
    }

    @Override
    public Map<String, Object> validateConsent(Map<String, Object> reqObject) {
        try {
            LogUtils.log("Validate consent status....");
            Map<String, Object> validationResult = new HashMap<>();
            // Extract claims
            Map<String, Object> claims = (Map<String, Object>) reqObject.get("claims");
            Map<String, Object> idTokenClaims = (Map<String, Object>) claims.get("id_token");
            Map<String, Object> intent = (Map<String, Object>) idTokenClaims.get("openbanking_intent_id");

            String intentId = (String) intent.get("value");
            this.CONSENT_ID = intentId;
            LogUtils.log("Consent id is : %", this.CONSENT_ID);
            // Call Consent Engine REST API
            String apiUrl = this.CONSENT_ENGINE_API_ENDPOINT + intentId;

            HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");

            int status = con.getResponseCode();
            if (status != 200) {
                validationResult.put("valid", false);
                validationResult.put("message", "Initial Consent status is not valid");
                return validationResult;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            String resp = response.toString();
            LogUtils.log("Consent Engine Response: %", resp);
            if(resp.contains("\"status\":\"AwaitingAuthorise\"") || resp.contains("\"status\":\"Authorised\"")){
                validationResult.put("valid", true);
                validationResult.put("message", "Initial Consent status validate successfully");
                return validationResult;
            }
            

        } catch (Exception e) {
            LogUtils.log("Error: %", e);
        }
    }

    @Override
    public String prepareRfacRespPayload() {
        try {
            LogUtils.log("Preparing RFAC Response payload");
            String consentId = this.CONSENT_ID;

            // Build signed JWS with ConsentID + Auth Type
            String jws = buildRFACJWS(consentId, this.AUTH_METHOD);
            Map<String, Object> payload = new HashMap<>();
            payload.put("jws", jws);
            payload.put("status", "PENDING_VERIFICATION");
            // Convert Map to JSON string
            // ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(payload);             
        } catch (Exception e) {
            LogUtils.log("Getting error while praparing RFAC payload %",e);
        }
 
    }    

    @Override
    public Map<String, Object> verifyExternalAppResult(Map<String, Object> resultFromApp) {
        LogUtils.log("Verify External App Result...");
        Map<String, Object> validationResult = new HashMap<>();
        try {
            String jws = (String) resultFromApp.get("jws");

            // Parse JWS using public key
            // Jws<Claims> parsed = Jwts.parserBuilder()
            //         .setSigningKey(keyPair.getPublic())
            //         .build()
            //         .parseClaimsJws(jws);

            // Claims claims = parsed.getBody();

            Map<String, Object> claims = (Map<String, Object>) resultFromApp.get("claims");

            String consentId = claims.get("consentId", String.class);
            String userId = claims.get("userId", String.class);
            String authMethod  = claims.get("authMethod", String.class);
            String transactionId = claims.get("transactionId", String.class);
            Long iat = claims.get("issuedAt", Long.class);
            Long exp = claims.get("expiresAt", Long.class);

            // Validate timestamps
            long now = System.currentTimeMillis() / 1000; // seconds
            if (iat == null || iat > now) {
                validationResult.put("valid", false);
                validationResult.put("message", "JWS issued-at invalid");
                return validationResult;
            }
            if (exp != null && exp < now) {
                validationResult.put("valid", false);
                validationResult.put("message", "JWS expired");
                return validationResult;
            }
            // Validate transactionId
            if (!this.transactionalId.equals(transactionId)) {
                
                validationResult.put("valid", false);
                validationResult.put("message", "Transaction ID mismatch");
                return validationResult;
            }

            // Validate consentId
            if (!this.CONSENT_ID.equals(consentId)) {
                validationResult.put("valid", false);
                validationResult.put("message", "Consent ID mismatch");
                return validationResult;
            }else{
                Map<String, Object> result = verifyFinalConsentStatus(consentId, userId);
                if(!Boolean.TRUE.equals(result.get("valid"))){
                    validationResult.put("valid", false);
                    validationResult.put("message", result.get("message")); 
                }
                // All checks passed
                validationResult.put("valid", true);
                validationResult.put("consentId", consentId);
                validationResult.put("authMethod", authMethod);
                validationResult.put("transactionId", transactionId);
                validationResult.put("userId", result.get("userId"));                 
            }


        } catch (Exception e) {
            validationResult.put("valid", false);
            validationResult.put("message", "Exception parsing JWS: " + e.getMessage());
        }

        return validationResult;
    }
    
    private Map<String, Object> verifyFinalConsentStatus(String consentId, String userId){
        Map<String, Object> result = new HashMap<>();
        try {
            // REST call to Consent Engine
            // Replace with your actual Consent Engine endpoint
            String consentEngineUrl = this.CONSENT_ENGINE_API_ENDPOINT + consentId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(consentEngineUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LogUtils.log("Checking final status code...")
            if (response.statusCode() != 200) {
                result.put("valid", false);
                result.put("message", "Consent Engine error: " + response.statusCode());
                return result;
            }
            Map<String, Object> consentData = mapper.readValue(response.body(), Map.class);

            // Example expected fields from Consent Engine
            String status = (String) consentData.get("status");
            String authMethod = (String) consentData.get("authMethod");
            String consentUser = (String) consentData.get("userId");

            // Validate status
            if (!"Authorised".equalsIgnoreCase(status)) {
                result.put("valid", false);
                result.put("message", "Consent not authorised");
                return result;
            }

            // Validate user
            if (consentUser != null && !consentUser.equals(userId)) {
                result.put("valid", false);
                result.put("message", "User mismatch in consent");
                return result;
            }

            // Success
            result.put("valid", true);
            result.put("consentId", consentId);
            result.put("status", status);
            result.put("authMethod", authMethod);
            result.put("userId", consentUser);

        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "Exception while validating consent: " + e.getMessage());
        }
        return result;        
    }

    private String buildRFACJWS(String consentId, String authMethod) throws Exception {
        // Build claims for app
        this.transactionalId = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("consentId", consentId);
        claims.put("authMethod", authMethod);
        claims.put("transactionalId", this.transactionalId);
        claims.put("issuedAt", Instant.now().toString());
        claims.put("expiresAt", Instant.now().plus(5, ChronoUnit.MINUTES).toString()); // 5-min expiry

        String jws = Jwts.builder()
                .setClaims(claims)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();        
        return jws;
    }  

    private String extractConsentId(Map<String, Object> reqObject) {
        Map<String, Object> claims = (Map<String, Object>) reqObject.get("claims");
        Map<String, Object> userInfo = (Map<String, Object>) claims.get("userinfo");
        Map<String, Object> intentObj = (Map<String, Object>) userInfo.get("openbanking_intent_id");
        return (String) intentObj.get("value");
    }    

    // Debug helper
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

}
