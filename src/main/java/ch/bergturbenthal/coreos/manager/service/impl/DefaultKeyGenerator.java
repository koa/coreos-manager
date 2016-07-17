package ch.bergturbenthal.coreos.manager.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.bergturbenthal.coreos.manager.config.Configuration;
import ch.bergturbenthal.coreos.manager.service.KeyGenerator;
import lombok.Cleanup;

@Service
public class DefaultKeyGenerator implements KeyGenerator {
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private final KeyPairGenerator keyGen;
	private final char[] keyPassword;
	private final File keystoreFile;
	private final KeyStore ks;
	private final SecureRandom random;
	private final File serialFile;

	@Autowired
	public DefaultKeyGenerator(final Configuration configuration)	throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException,
																																NoSuchProviderException, OperatorException, UnrecoverableKeyException {
		final File localData = configuration.getLocalData();
		if (!localData.exists()) {
			localData.mkdirs();
		}
		keystoreFile = new File(localData, "keystore");
		serialFile = new File(localData, "serial");
		ks = KeyStore.getInstance(KeyStore.getDefaultType());
		keyPassword = configuration.getKeyPassword();
		if (keystoreFile.exists()) {
			@Cleanup
			final FileInputStream is = new FileInputStream(keystoreFile);
			ks.load(is, keyPassword);
		} else {
			ks.load(null, null);
		}

		keyGen = KeyPairGenerator.getInstance("RSA", BC);
		random = SecureRandom.getInstance("SHA1PRNG");
		keyGen.initialize(2048, random);
	}

	private PrivateKeyEntry getRootKey(final String domainName)	throws KeyStoreException,
																															IOException,
																															OperatorCreationException,
																															CertificateException,
																															FileNotFoundException,
																															NoSuchAlgorithmException,
																															GeneralSecurityException {
		final String rootKeyName = "root." + domainName;
		if (!ks.containsAlias(rootKeyName)) {
			final int validDays = 365 * 10;

			final KeyPair pair = keyGen.generateKeyPair();
			final PrivateKey rootKey = pair.getPrivate();
			final PublicKey publicKey = pair.getPublic();

			final X509Certificate cert = signKey(publicKey, rootKey, domainName, validDays);
			final PrivateKeyEntry entry = new PrivateKeyEntry(rootKey, new Certificate[] { cert });
			storeEntry(rootKeyName, entry);
			return entry;
		} else {
			return (PrivateKeyEntry) ks.getEntry(rootKeyName, new KeyStore.PasswordProtection(keyPassword));
		}
	}

	private synchronized BigInteger nextSerial() throws IOException {
		final BigInteger newValue = readCurrentSerial().add(BigInteger.ONE);
		final File tempFile = File.createTempFile("serial", ".new", serialFile.getParentFile());
		try {
			@Cleanup
			final PrintWriter writer = new PrintWriter(tempFile);
			writer.println(newValue);
			tempFile.renameTo(serialFile);
		} finally {
			tempFile.delete();
		}
		return newValue;

	}

	private BigInteger readCurrentSerial() throws IOException {
		if (!serialFile.exists()) {
			return BigInteger.ZERO;
		}
		@Cleanup
		final BufferedReader reader = new BufferedReader(new FileReader(serialFile));
		final String line = reader.readLine();
		return new BigInteger(line);

	}

	@Override
	public String rootCertificate(final String domainName) throws OperatorException, GeneralSecurityException, IOException {
		final PrivateKeyEntry privateKeyEntry = getRootKey(domainName);
		final StringWriter writer = new StringWriter();
		{
			@Cleanup
			final PemWriter pemWriter = new PemWriter(writer);
			for (final Certificate cert : privateKeyEntry.getCertificateChain()) {
				pemWriter.writeObject(new JcaMiscPEMGenerator(cert));
			}
		}
		return writer.toString();
	}

	private X509Certificate signKey(final PublicKey publicKey, final PrivateKey signKey, final String keyName, final int validDays)	throws IOException,
																																																																	OperatorCreationException,
																																																																	CertificateException,
																																																																	GeneralSecurityException {

		final JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();

		final X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
		nameBuilder.addRDN(BCStyle.OU, "None");
		nameBuilder.addRDN(BCStyle.O, "ca");
		nameBuilder.addRDN(BCStyle.CN, keyName);
		final X500Name issuer = nameBuilder.build();
		final BigInteger serial = nextSerial();
		final Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1));
		final Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(validDays));
		final X500Name subject = nameBuilder.build();
		@Cleanup
		final ASN1InputStream asn1InputStream = new ASN1InputStream(publicKey.getEncoded());
		final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(asn1InputStream.readObject());
		final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(signKey);
		final X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);
		x509v3CertificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		x509v3CertificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(publicKey));
		x509v3CertificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(publicKey));
		x509v3CertificateBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));
		final X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(signer);
		final X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(x509CertificateHolder);
		return cert;
	}

	private void storeEntry(final String rootKeyName, final PrivateKeyEntry entry)	throws KeyStoreException,
																																									FileNotFoundException,
																																									IOException,
																																									NoSuchAlgorithmException,
																																									CertificateException {
		ks.setEntry(rootKeyName, entry, new KeyStore.PasswordProtection(keyPassword));
		@Cleanup
		final FileOutputStream stream = new FileOutputStream(keystoreFile);
		ks.store(stream, keyPassword);
	}
}
