package com.campushub.auth.dto;

public record LoginClientContext(
        String userAgent,
        String ipAddress
) {
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_IP_ADDRESS_LENGTH = 45;

    public LoginClientContext {
        userAgent = normalize(userAgent, MAX_USER_AGENT_LENGTH);
        ipAddress = normalize(ipAddress, MAX_IP_ADDRESS_LENGTH);
    }


    private static String normalize(String value, int maxLength) {
        if(value == null) return null;
        String normalized = value.strip();
        if(normalized.isEmpty()) return null;

        return normalized.length() <= maxLength ? normalized :normalized.substring(0, maxLength);
    }
}
