package ch.bergturbenthal.coreos.manager.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.bouncycastle.operator.OperatorException;

public interface KeyGenerator {
	public String rootCertificate(String domainName) throws OperatorException, GeneralSecurityException, IOException;
}
