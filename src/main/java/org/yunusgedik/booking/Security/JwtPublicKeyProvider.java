package org.yunusgedik.booking.Security;

import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtPublicKeyProvider {
    private final PublicKey publicKey;

    public JwtPublicKeyProvider() throws Exception {
        String key = Files.readString(Paths.get("src/main/resources/keys/jwt_public.pem"))
            .replaceAll("-----\\w+ PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.publicKey = kf.generatePublic(spec);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}