package com.novastack.sms.mpesa;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * Builds the Daraja {@code SecurityCredential} (RSA-encrypted initiator password).
 */
final class MpesaSecurityCredential {

    private MpesaSecurityCredential() {
    }

    static boolean configured(AppProperties.Mpesa mpesa) {
        if (mpesa == null || isBlank(mpesa.getInitiatorName())) {
            return false;
        }
        return !isBlank(mpesa.getSecurityCredential()) || !isBlank(mpesa.getInitiatorPassword());
    }

    static String resolve(AppProperties.Mpesa mpesa) {
        if (!isBlank(mpesa.getSecurityCredential())) {
            return mpesa.getSecurityCredential().trim();
        }
        if (isBlank(mpesa.getInitiatorPassword())) {
            throw new ApiException(
                    "M-Pesa Transaction Status is not configured. Set MPESA_INITIATOR_NAME and "
                            + "MPESA_SECURITY_CREDENTIAL (or MPESA_INITIATOR_PASSWORD plus the Safaricom cert).",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return encrypt(mpesa.getInitiatorPassword().trim(), loadCertificate(mpesa.getInitiatorCertificatePath()));
    }

    static String encrypt(String password, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException("Failed to encrypt M-Pesa initiator password: " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static PublicKey loadCertificate(String path) {
        try (InputStream in = openCertificate(path)) {
            if (in == null) {
                throw new ApiException(
                        "M-Pesa initiator certificate not found. Set MPESA_SECURITY_CREDENTIAL "
                                + "or MPESA_INITIATOR_CERT to the Safaricom .cer file.",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory.generateCertificate(in);
            return cert.getPublicKey();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException("Failed to read M-Pesa initiator certificate: " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static InputStream openCertificate(String path) throws Exception {
        if (!isBlank(path)) {
            Path file = Path.of(path.trim());
            if (Files.isRegularFile(file)) {
                return Files.newInputStream(file);
            }
            InputStream classpath = MpesaSecurityCredential.class.getResourceAsStream(
                    path.startsWith("/") ? path : "/" + path);
            if (classpath != null) {
                return classpath;
            }
        }
        InputStream bundled = MpesaSecurityCredential.class.getResourceAsStream("/certs/safaricom-prod.cer");
        if (bundled != null) {
            return bundled;
        }
        return MpesaSecurityCredential.class.getResourceAsStream("/certs/safaricom-sandbox.cer");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
