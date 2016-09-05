package ch.bergturbenthal.coreos.manager;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertGenTest {

	public static void main(final String[] args) throws OperatorException, GeneralSecurityException, IOException {
		final KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
		rsa.initialize(4096);
		final KeyPair kp = rsa.generateKeyPair();

		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 1);

		final byte[] pk = kp.getPublic().getEncoded();
		final SubjectPublicKeyInfo bcPk = SubjectPublicKeyInfo.getInstance(pk);

		final X509v1CertificateBuilder certGen = new X509v1CertificateBuilder(new X500Name("CN=CA Cert"),
																																					BigInteger.ONE,
																																					new Date(),
																																					cal.getTime(),
																																					new X500Name("CN=CA Cert"),
																																					bcPk);

		final X509CertificateHolder certHolder = certGen.build(new JcaContentSignerBuilder("SHA1withRSA").build(kp.getPrivate()));

		System.out.println("CA CERT");

		final Encoder encoder = Base64.getEncoder();

		System.out.println("-----BEGIN CERTIFICATE-----");
		System.out.println(new String(encoder.encode(certHolder.getEncoded())));
		System.out.println("-----END CERTIFICATE-----");

		System.exit(0);
	}

}
