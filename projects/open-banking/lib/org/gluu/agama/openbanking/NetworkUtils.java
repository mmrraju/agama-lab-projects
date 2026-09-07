package org.gluu.agama.openbanking.consent;

public class NetworkUtils {
    public static String urlBeforeContextPath() {
        HttpServletRequest req = CdiUtil.bean(HttpServletRequest.class);
        return req.getScheme() + "://" + req.getServerName();
    }    
}
