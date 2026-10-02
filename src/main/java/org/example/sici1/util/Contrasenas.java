package org.example.sici1.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de contraseñas con PBKDF2 (incluido en Java, sin dependencias extra).
 * Formato guardado: pbkdf2$iteraciones$salBase64$hashBase64
 */
public final class Contrasenas {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Contrasenas() {}

    public static String hash(String contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        RANDOM.nextBytes(sal);
        byte[] hash = derivar(contrasena.toCharArray(), sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    public static boolean verificar(String contrasena, String guardado) {
        if (contrasena == null || guardado == null) return false;
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(contrasena.toCharArray(), sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(contrasena, sal, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            spec.clearPassword();
        }
    }
}
