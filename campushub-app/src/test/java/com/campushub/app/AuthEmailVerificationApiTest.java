package com.campushub.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthEmailVerificationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private JavaMailSender javaMailSender;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(true);

        doReturn(1L).when(stringRedisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        );
    }

    @Test
    void registrationSendsInitialVerificationEmail()
            throws Exception {

        TestAccount account = newTestAccount();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(account)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(
                        "$.data.emailVerificationRequired"
                ).value(true));

        var messageCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        SimpleMailMessage.class
                );

        verify(javaMailSender).send(
                messageCaptor.capture()
        );

        SimpleMailMessage message =
                messageCaptor.getValue();

        assertThat(message.getTo())
                .containsExactly(account.email());

        assertThat(message.getText())
                .contains(
                        "http://localhost:3000/verify-email?token="
                );
    }

    @Test
    void unverifiedLoginAutomaticallyResendsVerificationEmail()
            throws Exception {

        TestAccount account = newTestAccount();

        // 第一封：注册完成后的 initial verification
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(account)))
                .andExpect(status().isCreated());

        // 第二封：正确密码登录未验证账户时自动 resend
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(account)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_1008"));

        verify(javaMailSender, times(2))
                .send(any(SimpleMailMessage.class));
    }

    private TestAccount newTestAccount() {
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        return new TestAccount(
                "api." + suffix,
                "api." + suffix + "@example.com",
                "CampusHub!123"
        );
    }

    private String registerBody(TestAccount account) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                account.username(),
                account.email(),
                account.password()
        );
    }

    private String loginBody(TestAccount account) {
        return """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(
                account.email(),
                account.password()
        );
    }

    private record TestAccount(
            String username,
            String email,
            String password
    ) {
    }
}