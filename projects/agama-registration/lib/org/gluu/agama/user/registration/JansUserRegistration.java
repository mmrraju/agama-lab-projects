package org.gluu.agama.user.registration;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.UserService;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import io.jans.as.common.service.common.ConfigurationService;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import org.gluu.agama.user.UserRegistration;

public class JansUserRegistration extends UserRegistration {
    
    private static final Logger logger = LoggerFactory.getLogger(JansUserRegistration.class);

    private static final String MAIL = "mail";
    private static final String UID = "uid";
    private static final String DISPLAY_NAME = "displayName";
    private static final String GIVEN_NAME = "givenName";
    private static final String PASSWORD = "userPassword";
    private static final String INUM_ATTR = "inum";
    private static final String EXT_ATTR = "jansExtUid";
    private static final String USER_STATUS = "jansStatus";
    private static final String EXT_UID_PREFIX = "github:";
    private static final SecureRandom RAND = new SecureRandom();

    private static JansUserRegistration INSTANCE = null;

    private JansUserRegistration() {}

    public static synchronized JansUserRegistration getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new JansUserRegistration();

        return INSTANCE;
    }

    public Map<String, String> getUserEntityByMail(String email) {
        User user = getUser(MAIL, email);
        boolean local = user != null;
        logger.debug("There is {} local account for {}", local ? "a" : "no", email);
    
        if (local) {
            String uid = getSingleValuedAttr(user, UID);
            String inum = getSingleValuedAttr(user, INUM_ATTR);
            String name = getSingleValuedAttr(user, GIVEN_NAME);
    
            if (name == null) {
                name = getSingleValuedAttr(user, DISPLAY_NAME);
                if (name == null && email != null && email.contains("@")) {
                    name = email.substring(0, email.indexOf("@"));
                }
            }
    
            // Creating a truly modifiable map
            Map<String, String> userMap = new HashMap<>();
            userMap.put(UID, uid);
            userMap.put(INUM_ATTR, inum);
            userMap.put("name", name);
            userMap.put("email", email);
    
            return userMap;
        }
    
        return new HashMap<>();
    }
    

    public Map<String, String> getUserEntityByUserName(String userName) {
        User user = getUser(UID, userName);
        boolean local = user != null;
    
        if (local) {
            String email = getSingleValuedAttr(user, MAIL);
            String inum = getSingleValuedAttr(user, INUM_ATTR);
            String name = getSingleValuedAttr(user, GIVEN_NAME);
            String uid = getSingleValuedAttr(user, UID); // Define uid properly
    
            if (name == null) {
                name = getSingleValuedAttr(user, DISPLAY_NAME);
                if (name == null && email != null && email.contains("@")) {
                    name = email.substring(0, email.indexOf("@"));
                }
            }
    
            logger.debug("There is {} local account for {}", local ? "a" : "no", email);
    
            // Creating a modifiable HashMap directly
            Map<String, String> userMap = new HashMap<>();
            userMap.put(UID, uid);
            userMap.put(INUM_ATTR, inum);
            userMap.put("name", name);
            userMap.put("email", email);
    
            return userMap;
        }
    
        return new HashMap<>();
    }
    

    public String addNewUser(Map<String, String> profile) throws Exception {
        Set<String> attributes = Set.of("uid", "mail", "displayName","givenName", "sn", "userPassword");
        User user = new User();
    
        attributes.forEach(attr -> {
            String val = profile.get(attr);
            System.out.println("Fetching attribute: " + attr + ", Value: " + val);
            if (StringHelper.isNotEmpty(val)) {
                user.setAttribute(attr, val);      
            }
        });

        user.setAttribute(USER_STATUS, false);
    
        UserService userService = CdiUtil.bean(UserService.class);
        user = userService.addUser(user, true);
    
        if (user == null) {
            throw new EntryNotFoundException("Added user not found");
        }
    
        return getSingleValuedAttr(user, INUM_ATTR);
    } 

    private String getSingleValuedAttr(User user, String attribute) {

        System.out.println("User object: " + user); // Print the entire user object
        System.out.println("Fetching attribute: " + attribute);

        Object value = null;
        if (attribute.equals(UID)) {
            //user.getAttribute("uid", true, false) always returns null :(
            value = user.getUserId();
        } else {
            value = user.getAttribute(attribute, true, false);
        }
        return value == null ? null : value.toString();

    }

    private static User getUser(String attributeName, String value) {
        UserService userService = CdiUtil.bean(UserService.class);
        return userService.getUserByAttribute(attributeName, value, true);
    }    
}



