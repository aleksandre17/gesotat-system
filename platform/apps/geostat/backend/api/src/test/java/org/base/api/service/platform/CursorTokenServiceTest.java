package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CursorTokenServiceTest {
    @Test void cursorIsBoundToContractAndPage(){CursorTokenService s=new CursorTokenService("test-secret");String token=s.issue("C",11,2);assertEquals(2,s.verify(token,"C",11));assertThrows(IllegalArgumentException.class,()->s.verify(token,"C",12));assertThrows(IllegalArgumentException.class,()->s.verify(token,"D",11));}
    @Test void tamperingIsRejected(){CursorTokenService s=new CursorTokenService("test-secret");String token=s.issue("C",11,2);String tampered=token.substring(0,token.length()-1)+(token.endsWith("A")?"B":"A");assertThrows(IllegalArgumentException.class,()->s.verify(tampered,"C",11));}
    @Test void productionRejectsPlaceholderSigningSecret(){
        String old=System.getProperty("spring.profiles.active");
        try { System.setProperty("spring.profiles.active","prod"); assertThrows(IllegalStateException.class,()->new CursorTokenService("change-me-in-production")); }
        finally { if(old==null) System.clearProperty("spring.profiles.active"); else System.setProperty("spring.profiles.active",old); }
    }
    @Test void expiredCursorIsRejected(){
        CursorTokenService s=new CursorTokenService("test-secret");
        String payload="C|11|2|"+(java.time.Instant.now().minusSeconds(86_401).getEpochSecond());
        String token=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signForTest(payload));
        assertThrows(IllegalArgumentException.class,()->s.verify(token,"C",11));
    }
    private static byte[] signForTest(String value){try{var m=javax.crypto.Mac.getInstance("HmacSHA256");m.init(new javax.crypto.spec.SecretKeySpec("test-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));return m.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}}
}
