package edu.neu.csye7255.healthcare.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.neu.csye7255.healthcare.entity.OauthToken;
import edu.neu.csye7255.healthcare.entity.User;
import edu.neu.csye7255.healthcare.model.LoginResponseDTO;
import edu.neu.csye7255.healthcare.repository.OauthTokenRepository;
import edu.neu.csye7255.healthcare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoginService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    OauthTokenRepository tokenRepository;

    @Value("${clientId}")
    String clientId;
    @Value("${clientSecret}")
    String clientSecret;

    public LoginResponseDTO processGrantCode(String code) {
        String accessToken = getOauthAccessTokenGoogle(code);

        User googleUser = getProfileDetailsGoogle(accessToken);
        User user = userRepository.findByEmail(googleUser.getEmail());

        if (user == null) {
            user = registerUser(googleUser.getFirstName(), googleUser.getLastName(), googleUser.getEmail(), googleUser.getPassword());
        }

        return saveTokenForUser(user);

    }

    private LoginResponseDTO saveTokenForUser(User user) {
        LoginResponseDTO dto = generateToken();
        OauthToken token = new OauthToken();
        token.setAccessToken(dto.getAccessToken());
        token.setRefreshToken(dto.getRefreshToken());
        token.setExpirationTime(dto.getExpirationTime());
        token.setUser(user);

        tokenRepository.save(token);
        return dto;
    }

    private LoginResponseDTO generateToken() {
        LoginResponseDTO res = new LoginResponseDTO();
        res.setAccessToken(UUID.randomUUID().toString());
        res.setRefreshToken(UUID.randomUUID().toString());
        res.setExpirationTime(LocalDateTime.now().plusHours(1));
        return res;
    }

    public User registerUser(String firstName, String lastName, String email, String password) {
        User user = new User();
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRole("USER");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        user = userRepository.save(user);

//        sendAccountVerificationMail(user);
        return user;
    }

    private User getProfileDetailsGoogle(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(accessToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);

        String url = "https://www.googleapis.com/oauth2/v2/userinfo";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        JsonObject jsonObject = new Gson().fromJson(response.getBody(), JsonObject.class);

        User user = new User();
        user.setEmail(jsonObject.get("email").toString().replace("\"", ""));
        user.setFirstName(jsonObject.get("name").toString().replace("\"", ""));
        user.setLastName(jsonObject.get("given_name").toString().replace("\"", ""));
        user.setPassword(UUID.randomUUID().toString());

        return user;
    }

    private String getOauthAccessTokenGoogle(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("redirect_uri", "http://localhost:8080/api/v1/auth/grantcode");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("scope", "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile");
        params.add("scope", "https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email");
        params.add("scope", "openid");
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, httpHeaders);

        String url = "https://oauth2.googleapis.com/token";
        String response = restTemplate.postForObject(url, requestEntity, String.class);
        System.out.println(response);
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        System.out.println(jsonObject);
        return jsonObject.get("access_token").toString().replace("\"", "");
    }
}
