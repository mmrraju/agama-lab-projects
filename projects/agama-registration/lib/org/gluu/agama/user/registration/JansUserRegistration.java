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


import org.gluu.agama.smtp.jans.user.UserRegistration;

public class JansUserRegistration extends UserRegistration {
    
    private static final Logger logger = LoggerFactory.getLogger(JansUserRegistration.class);

    private static final String INUM_ATTR = "inum";
    private static final String EXT_ATTR = "jansExtUid";
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

                if (name == null) {
                    name = email.substring(0, email.indexOf("@"));
                }
            }
            //I need a modifiable map
            return new HashMap<>(Map.of(UID, uid, INUM_ATTR, inum, "name", name, "email", email));
        }
        return new HashMap<>();

    }

    public Map<String, String> getUserEntityByUserName(String userName) {

        User user = getUser(UID, userName);
        boolean local = user != null;
        logger.debug("There is {} local account for {}", local ? "a" : "no", email);

        if (local) {
            String email = getSingleValuedAttr(user, MAIL);
            String inum = getSingleValuedAttr(user, INUM_ATTR);
            String name = getSingleValuedAttr(user, GIVEN_NAME);

            if (name == null) {
                name = getSingleValuedAttr(user, DISPLAY_NAME);

                if (name == null) {
                    name = email.substring(0, email.indexOf("@"));
                }
            }
            //I need a modifiable map
            return new HashMap<>(Map.of(UID, uid, INUM_ATTR, inum, "name", name, "email", email));
        }
        return new HashMap<>();

    }   

    public User addNewUser(Map<String, String> profile, Set<String> attributes)
    throws Exception {

        User user = new User();

        attributes.forEach(attr -> {
            String val = profile.get(attr);
            if (StringHelper.isNotEmpty(val)) {
                user.setAttribute(attr, val);
            }
        });
        UserService userService = CdiUtil.bean(UserService.class);

        user = userService.addUser(user, true);
        if (user == null) throw new EntryNotFoundException("Added user not found");

        // return getSingleValuedAttr(user, INUM_ATTR);
        return user;

    }   

    private String getSingleValuedAttr(User user, String attribute) {

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
