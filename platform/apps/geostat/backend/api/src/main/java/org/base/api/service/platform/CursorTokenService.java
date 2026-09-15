package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/** Signed, contract/page-bound cursor. It prevents cursor reuse across contracts or pages. */
@Service
public class CursorTokenService {
    private static final long MAX_CURSOR_AGE_SECONDS = 86_400L;
    private final byte[] secret;
    public CursorTokenService(@Value("${platform.cursor-signing-secret:change-me-in-production}") String secret){
        String profile=System.getProperty("spring.profiles.active",System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE",""));
        if(profile.toLowerCase(Locale.ROOT).contains("prod") &&
                (secret==null || secret.isBlank() || secret.equals("change-me-in-production") || secret.length()<32))
            throw new IllegalStateException("PLATFORM_CURSOR_SIGNING_SECRET must be a non-placeholder value of at least 32 characters in production");
        this.secret=Objects.requireNonNull(secret,"platform.cursor-signing-secret").getBytes(StandardCharsets.UTF_8);
    }
    public String issue(String contract,int pageId,int nextPage){String payload=contract+"|"+pageId+"|"+nextPage+"|"+Instant.now().getEpochSecond();return enc(payload)+"."+enc(sign(payload));}
    public int verify(String token,String contract,int pageId){try{String[] p=token.split("\\.",-1);if(p.length!=2)throw new IllegalArgumentException("Invalid pagination cursor");String payload=new String(Base64.getUrlDecoder().decode(p[0]),StandardCharsets.UTF_8);if(!constant(p[1],enc(sign(payload))))throw new IllegalArgumentException("Invalid pagination cursor");String[] v=payload.split("\\|",-1);if(v.length!=4||!v[0].equals(contract)||Integer.parseInt(v[1])!=pageId)throw new IllegalArgumentException("Cursor scope mismatch");long issued=Long.parseLong(v[3]);long age=Instant.now().getEpochSecond()-issued;if(issued<=0||age<0||age>MAX_CURSOR_AGE_SECONDS)throw new IllegalArgumentException("Pagination cursor expired");return Math.max(1,Integer.parseInt(v[2]));}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Invalid pagination cursor");}}
    public String issueKeyset(String contract,int pageId,String snapshot,String fingerprint,List<String> keys,List<Object> values,boolean descending){String payload="KS|"+enc(contract)+"|"+pageId+"|"+enc(snapshot==null?"":snapshot)+"|"+enc(fingerprint==null?"":fingerprint)+"|"+descending+"|"+enc(String.join("\u001f",keys))+"|"+enc(values.stream().map(v->v==null?"":String.valueOf(v)).collect(java.util.stream.Collectors.joining("\u001f")));return enc(payload)+"."+enc(sign(payload));}
    public KeysetState verifyKeyset(String token,String contract,int pageId,String fingerprint){try{String[] p=token.split("\\.",-1);if(p.length!=2)throw new IllegalArgumentException("Invalid keyset cursor");String payload=new String(Base64.getUrlDecoder().decode(p[0]),StandardCharsets.UTF_8);if(!constant(p[1],enc(sign(payload))))throw new IllegalArgumentException("Invalid keyset cursor");String[] v=payload.split("\\|",-1);if(v.length!=8||!"KS".equals(v[0])||!contract.equals(new String(Base64.getUrlDecoder().decode(v[1]),StandardCharsets.UTF_8))||Integer.parseInt(v[2])!=pageId||!fingerprint.equals(new String(Base64.getUrlDecoder().decode(v[4]),StandardCharsets.UTF_8)))throw new IllegalArgumentException("Keyset cursor scope mismatch");List<String> keys=split(v[6]);List<Object> vals=new ArrayList<>(split(v[7]));if(keys.size()!=vals.size())throw new IllegalArgumentException("Invalid keyset tuple");return new KeysetState(new String(Base64.getUrlDecoder().decode(v[3]),StandardCharsets.UTF_8),keys,vals,Boolean.parseBoolean(v[5]));}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Invalid keyset cursor");}}
    private static List<String> split(String x){String s=new String(Base64.getUrlDecoder().decode(x),StandardCharsets.UTF_8);return s.isEmpty()?List.of():List.of(s.split("\\u001f",-1));}
    public record KeysetState(String snapshot,List<String> keys,List<Object> values,boolean descending){}
    private byte[] sign(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException("Cursor signer unavailable",e);}}
    private static String enc(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static String enc(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    private static boolean constant(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
