package com.psoft.clinic.keycloak;

import com.psoft.clinic.keycloak.dto.KeycloakTokenResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final KeycloakProperties props;
    private final RestTemplate restTemplate;

    @PostConstruct
    void initLogs() {
        log.info("KC internal base url='{}'", props.getInternalBaseUrl());
        log.info("KC realm='{}'", props.getRealm());
        log.info("KC clientId(login)='{}'", props.getClientId());
        log.info("KC adminClientId='{}'", props.getAdminClientId());
    }

    private String baseUrl() {
        String base = props.getInternalBaseUrl();
        if (base == null) throw new IllegalStateException("keycloak.internal-base-url está null");
        base = base.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!(base.startsWith("http://") || base.startsWith("https://"))) {
            throw new IllegalStateException("keycloak.internal-base-url tem de começar por http(s):// -> '" + base + "'");
        }
        return base;
    }

    private URI realmTokenUri(String realm) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl())
                .path("/realms/{realm}/protocol/openid-connect/token")
                .buildAndExpand(realm)
                .toUri();
    }

    private URI adminRealmBaseUri() {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl())
                .path("/admin/realms/{realm}")
                .buildAndExpand(props.getRealm())
                .toUri();
    }

    private URI adminUsersUri() {
        return UriComponentsBuilder
                .fromUri(adminRealmBaseUri())
                .path("/users")
                .build()
                .toUri();
    }

    // ==========================================================
    // 1) Admin token via SERVICE ACCOUNT (client_credentials)
    // ==========================================================
    public String getAdminAccessToken() {

        if (props.getAdminClientId() == null || props.getAdminClientId().isBlank()
                || props.getAdminClientSecret() == null || props.getAdminClientSecret().isBlank()) {
            throw new IllegalStateException("Faltam keycloak.admin-client-id / keycloak.admin-client-secret no properties");
        }

        URI url = realmTokenUri(props.getRealm()); // <- realm clinic (não master)

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getAdminClientId());
        form.add("client_secret", props.getAdminClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        ResponseEntity<KeycloakTokenResponse> resp =
                restTemplate.postForEntity(url, req, KeycloakTokenResponse.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Falha ao obter admin token (service account): " + resp.getStatusCode());
        }

        return resp.getBody().getAccessToken();
    }

    // ==========================================================
    // 2) Criar user + password + limpar required actions + role
    // ==========================================================
    public String createUser(String email, String name, String rawPassword, String roleName) {

        String adminToken = getAdminAccessToken();

        // ---- tratar nome (first / last) ----
        String firstName;
        String lastName;

        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            String[] parts = trimmed.split("\\s+");
            firstName = parts[0];
            lastName = (parts.length > 1)
                    ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length))
                    : "N/A";
        } else {
            firstName = "User";
            lastName = "N/A";
        }

        // 2.1 criar user
        URI createUrl = adminUsersUri();

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", email);
        payload.put("email", email);
        payload.put("enabled", true);
        payload.put("emailVerified", true);
        payload.put("requiredActions", List.of());
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        ResponseEntity<Void> resp = restTemplate.exchange(createUrl, HttpMethod.POST, req, Void.class);

        if (resp.getStatusCode() == HttpStatus.CONFLICT) {
            throw new RuntimeException("Utilizador já existe no Keycloak: " + email);
        }
        if (resp.getStatusCode() != HttpStatus.CREATED && !resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro a criar user no Keycloak: " + resp.getStatusCode());
        }

        URI location = resp.getHeaders().getLocation();
        String userId;
        if (location == null) {
            userId = findUserIdByUsername(email, adminToken);
        } else {
            String loc = location.toString();
            userId = loc.substring(loc.lastIndexOf("/") + 1);
        }

        setPassword(userId, rawPassword, adminToken);
        clearRequiredActions(userId, adminToken);
        assignRealmRole(userId, roleName, adminToken);

        return userId;
    }


    public String findUserIdByUsername(String username, String adminToken) {

        URI url = UriComponentsBuilder
                .fromUri(adminUsersUri())
                .queryParam("username", username) // NÃO pré-encodar
                .queryParam("exact", "true")
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<List> resp = restTemplate.exchange(url, HttpMethod.GET, req, List.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().isEmpty()) {
            throw new RuntimeException("Não consegui encontrar user no Keycloak: " + username);
        }

        Map first = (Map) resp.getBody().get(0);
        Object id = first.get("id");
        if (id == null) throw new RuntimeException("User encontrado mas sem id: " + username);
        return id.toString();
    }

    private void setPassword(String userId, String rawPassword, String adminToken) {

        URI url = UriComponentsBuilder
                .fromUri(adminRealmBaseUri())
                .path("/users/{id}/reset-password")
                .buildAndExpand(userId)
                .toUri();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "password");
        payload.put("value", rawPassword);
        payload.put("temporary", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, req, Void.class);

        if (resp.getStatusCode() != HttpStatus.NO_CONTENT && !resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro a definir password no Keycloak: " + resp.getStatusCode());
        }
    }

    private void clearRequiredActions(String userId, String adminToken) {

        URI url = UriComponentsBuilder
                .fromUri(adminRealmBaseUri())
                .path("/users/{id}")
                .buildAndExpand(userId)
                .toUri();

        Map<String, Object> payload = new HashMap<>();
        payload.put("requiredActions", List.of()); // limpa

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);

        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, req, Void.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro a limpar requiredActions do user: " + resp.getStatusCode());
        }
    }

    private void assignRealmRole(String userId, String roleName, String adminToken) {

        URI roleUrl = UriComponentsBuilder
                .fromUri(adminRealmBaseUri())
                .path("/roles/{role}")
                .buildAndExpand(roleName)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> getReq = new HttpEntity<>(headers);

        ResponseEntity<Map> roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, getReq, Map.class);
        if (!roleResp.getStatusCode().is2xxSuccessful() || roleResp.getBody() == null) {
            throw new RuntimeException("Role não existe no Keycloak: " + roleName);
        }

        List<Map<String, Object>> roles = List.of(roleResp.getBody());

        URI mapUrl = UriComponentsBuilder
                .fromUri(adminRealmBaseUri())
                .path("/users/{id}/role-mappings/realm")
                .buildAndExpand(userId)
                .toUri();

        HttpHeaders postHeaders = new HttpHeaders();
        postHeaders.setBearerAuth(adminToken);
        postHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<Map<String, Object>>> postReq = new HttpEntity<>(roles, postHeaders);

        ResponseEntity<Void> assignResp = restTemplate.exchange(mapUrl, HttpMethod.POST, postReq, Void.class);
        if (assignResp.getStatusCode() != HttpStatus.NO_CONTENT && !assignResp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro a atribuir role ao user: " + assignResp.getStatusCode());
        }
    }

    // ==========================================================
    // 3) Login token (password grant) -> client "postman"
    // ==========================================================
    public KeycloakTokenResponse login(String username, String password) {

        URI url = realmTokenUri(props.getRealm());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("username", username);
        form.add("password", password);

        if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
            form.add("client_secret", props.getClientSecret());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        ResponseEntity<KeycloakTokenResponse> resp =
                restTemplate.postForEntity(url, req, KeycloakTokenResponse.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Credenciais inválidas ou client mal configurado no Keycloak.");
        }

        return resp.getBody();
    }
}
