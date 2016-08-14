package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.bouncycastle.operator.OperatorException;

public interface KeyGenerator {
	String[] apiKey(String domainName, List<String> dnsNames, List<String> ipAddresses) throws OperatorException, IOException, GeneralSecurityException;

	String rootCertificate(String domainName) throws OperatorException, GeneralSecurityException, IOException;
}
