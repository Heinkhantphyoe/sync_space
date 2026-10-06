package com.hkp.sync_space.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.hkp.sync_space.common.ApiException;

@Service
public class JwtService {

	private static final Pattern SUBJECT = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern EXPIRY = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
	}

	public String createToken(UUID userId) {
		long expiresAt = Instant.now().plusMillis(properties.expirationMs()).getEpochSecond();
		String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
		String payload = encode("{\"sub\":\"" + userId + "\",\"exp\":" + expiresAt + "}");
		String signingInput = header + "." + payload;
		return signingInput + "." + encode(sign(signingInput));
	}

	public UUID parseUserId(String token) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw unauthorized();
		}
		byte[] expected = sign(parts[0] + "." + parts[1]);
		byte[] actual;
		try {
			actual = DECODER.decode(parts[2]);
		}
		catch (IllegalArgumentException exception) {
			throw unauthorized();
		}
		if (!MessageDigest.isEqual(expected, actual)) {
			throw unauthorized();
		}
		String json;
		try {
			json = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException exception) {
			throw unauthorized();
		}
		Matcher subject = SUBJECT.matcher(json);
		Matcher expiry = EXPIRY.matcher(json);
		if (!subject.find() || !expiry.find()) {
			throw unauthorized();
		}
		long expiresAt = Long.parseLong(expiry.group(1));
		if (Instant.now().getEpochSecond() >= expiresAt) {
			throw unauthorized();
		}
		try {
			return UUID.fromString(subject.group(1));
		}
		catch (IllegalArgumentException exception) {
			throw unauthorized();
		}
	}

	private byte[] sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception exception) {
			throw new IllegalStateException("Could not sign token", exception);
		}
	}

	private static String encode(String value) {
		return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String encode(byte[] value) {
		return ENCODER.encodeToString(value);
	}

	private static ApiException unauthorized() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
	}

}
