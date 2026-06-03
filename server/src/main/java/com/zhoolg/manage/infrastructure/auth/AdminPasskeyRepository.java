package com.zhoolg.manage.infrastructure.auth;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.zhoolg.manage.service.UserDirectoryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class AdminPasskeyRepository implements CredentialRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserDirectoryService userDirectory;

    public AdminPasskeyRepository(JdbcTemplate jdbcTemplate, UserDirectoryService userDirectory) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDirectory = userDirectory;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userDirectory.findByLoginName(username)
                .map(account -> findByUserId(account.id()).stream()
                        .map(row -> PublicKeyCredentialDescriptor.builder()
                                .id(byteArray(row.credentialId()))
                                .build())
                        .collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userDirectory.findByLoginName(username).map(account -> userHandle(account.id()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        long userId = userId(userHandle);
        return userDirectory.findById(userId).map(UserDirectoryService.Account::loginName);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        long userId = userId(userHandle);
        return findByCredentialId(credentialId.getBase64Url()).stream()
                .filter(row -> row.userId() == userId)
                .findFirst()
                .map(row -> toRegisteredCredential(row, userHandle));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return findByCredentialId(credentialId.getBase64Url()).stream()
                .map(row -> toRegisteredCredential(row, userHandle(row.userId())))
                .collect(Collectors.toSet());
    }

    public void saveCredential(
            long userId,
            String username,
            String credentialId,
            String publicKeyCose,
            long signatureCount,
            String displayName
    ) {
        jdbcTemplate.update("""
                        INSERT INTO sys_passkey_credential
                            (user_id, username, credential_id, public_key_cose, signature_count, display_name, enabled)
                        VALUES (?, ?, ?, ?, ?, ?, 1)
                        ON DUPLICATE KEY UPDATE
                            user_id = VALUES(user_id),
                            username = VALUES(username),
                            public_key_cose = VALUES(public_key_cose),
                            signature_count = VALUES(signature_count),
                            display_name = VALUES(display_name),
                            enabled = 1
                        """,
                userId,
                username,
                credentialId,
                publicKeyCose,
                signatureCount,
                displayName == null || displayName.isBlank() ? "Passkey" : displayName.trim()
        );
    }

    public void updateSignatureCount(String credentialId, long signatureCount) {
        jdbcTemplate.update("""
                        UPDATE sys_passkey_credential
                        SET signature_count = ?, last_used_time = ?, update_time = ?
                        WHERE credential_id = ? AND enabled = 1
                        """,
                signatureCount,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                credentialId
        );
    }

    public List<CredentialSummary> listByUserId(long userId) {
        return jdbcTemplate.query("""
                SELECT id, username, display_name, last_used_time, create_time
                FROM sys_passkey_credential
                WHERE user_id = ? AND enabled = 1
                ORDER BY COALESCE(last_used_time, create_time) DESC, id DESC
                """, this::summaryRow, userId);
    }

    public boolean disableForUser(long id, long userId) {
        int updated = jdbcTemplate.update("""
                        UPDATE sys_passkey_credential
                        SET enabled = 0, update_time = ?
                        WHERE id = ? AND user_id = ? AND enabled = 1
                        """,
                Timestamp.valueOf(LocalDateTime.now()),
                id,
                userId
        );
        return updated > 0;
    }

    public ByteArray userHandle(long userId) {
        return new ByteArray(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
    }

    private long userId(ByteArray userHandle) {
        byte[] bytes = userHandle.getBytes();
        if (bytes.length != Long.BYTES) {
            return -1;
        }
        return ByteBuffer.wrap(bytes).getLong();
    }

    private List<CredentialRow> findByUserId(long userId) {
        return jdbcTemplate.query("""
                SELECT user_id, username, credential_id, public_key_cose, signature_count
                FROM sys_passkey_credential
                WHERE user_id = ? AND enabled = 1
                """, this::row, userId);
    }

    private List<CredentialRow> findByCredentialId(String credentialId) {
        return jdbcTemplate.query("""
                SELECT user_id, username, credential_id, public_key_cose, signature_count
                FROM sys_passkey_credential
                WHERE credential_id = ? AND enabled = 1
                """, this::row, credentialId);
    }

    private RegisteredCredential toRegisteredCredential(CredentialRow row, ByteArray userHandle) {
        return RegisteredCredential.builder()
                .credentialId(byteArray(row.credentialId()))
                .userHandle(userHandle)
                .publicKeyCose(byteArray(row.publicKeyCose()))
                .signatureCount(row.signatureCount())
                .build();
    }

    private ByteArray byteArray(String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid passkey byte array", ex);
        }
    }

    private CredentialRow row(ResultSet rs, int rowNum) throws SQLException {
        return new CredentialRow(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("credential_id"),
                rs.getString("public_key_cose"),
                rs.getLong("signature_count")
        );
    }

    private CredentialSummary summaryRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastUsed = rs.getTimestamp("last_used_time");
        Timestamp createTime = rs.getTimestamp("create_time");
        return new CredentialSummary(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                lastUsed == null ? null : lastUsed.toLocalDateTime(),
                createTime == null ? null : createTime.toLocalDateTime()
        );
    }

    public record CredentialSummary(
            long id,
            String username,
            String displayName,
            LocalDateTime lastUsedTime,
            LocalDateTime createTime
    ) {
    }

    private record CredentialRow(
            long userId,
            String username,
            String credentialId,
            String publicKeyCose,
            long signatureCount
    ) {
    }
}
