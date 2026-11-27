package com.just.AiAutomationTranslater.translatorservice.auth_user.service.impl;

import com.just.AiAutomationTranslater.translatorservice.auth_user.dto.LoginRequest;
import com.just.AiAutomationTranslater.translatorservice.auth_user.dto.LoginResponse;
import com.just.AiAutomationTranslater.translatorservice.auth_user.dto.RegisterRequest;
import com.just.AiAutomationTranslater.translatorservice.auth_user.dto.UserDTO;
import com.just.AiAutomationTranslater.translatorservice.auth_user.entity.RefreshToken;
import com.just.AiAutomationTranslater.translatorservice.auth_user.entity.User;
import com.just.AiAutomationTranslater.translatorservice.auth_user.repository.RefreshTokenRepository;
import com.just.AiAutomationTranslater.translatorservice.auth_user.repository.UserRepository;
import com.just.AiAutomationTranslater.translatorservice.exceptions.ConflictException;
import com.just.AiAutomationTranslater.translatorservice.exceptions.NotFoundException;
import com.just.AiAutomationTranslater.translatorservice.exceptions.UnauthorizedAccessException;
import com.just.AiAutomationTranslater.translatorservice.res.Response;
import com.just.AiAutomationTranslater.translatorservice.role.entity.Role;
import com.just.AiAutomationTranslater.translatorservice.role.repository.RoleRepository;
import com.just.AiAutomationTranslater.translatorservice.security.JwtService;
import com.just.AiAutomationTranslater.translatorservice.security.TokenHasher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private User testUser;
    private Role userRole;
    private LoginRequest validLoginRequest;
    private RegisterRequest validRegisterRequest;
    private String testEmail;
    private String testPassword;
    private String testAccessToken;
    private String testRefreshToken;
    private String hashedRefreshToken;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testPassword = "Password123!";
        testAccessToken = "access-token-jwt";
        testRefreshToken = "refresh-token-jwt";
        hashedRefreshToken = TokenHasher.sha256(testRefreshToken);

        userRole = Role.builder()
                .id(1L)
                .roleName("USER")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email(testEmail)
                .password("encoded-password")
                .enabled(true)
                .failedAttempts(0)
                .lockUntil(null)
                .lastLoginAttempt(null)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail(testEmail);
        validLoginRequest.setPassword(testPassword);

        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail(testEmail);
        validRegisterRequest.setPassword(testPassword);
    }

    @Nested
    @DisplayName("login() Tests")
    class LoginTests {

        @Test
        @DisplayName("login_whenValidCredentials_returnsLoginResponseWithTokens")
        void login_whenValidCredentials_returnsLoginResponseWithTokens() {
            // Arrange
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(testPassword, testUser.getPassword())).willReturn(true);
            given(jwtService.generateAccessToken(testEmail)).willReturn(testAccessToken);
            given(jwtService.generateRefreshToken(testEmail)).willReturn(testRefreshToken);
            given(jwtService.getAccessTokenExpirationSeconds()).willReturn(900L);
            given(jwtService.getRefreshTokenExpirationSeconds()).willReturn(2592000L);

            UserDTO userDTO = new UserDTO();
            userDTO.setId(1L);
            userDTO.setUsername("testuser");
            userDTO.setEmail(testEmail);
            given(modelMapper.map(testUser, UserDTO.class)).willReturn(userDTO);

            // Act
            Response<LoginResponse> response = authService.login(validLoginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo("Login Successful");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getToken()).isEqualTo(testAccessToken);
            assertThat(response.getData().getRefreshToken()).isEqualTo(testRefreshToken);
            assertThat(response.getData().getExpiresIn()).isEqualTo(900L);
            assertThat(response.getData().getRefreshExpiresIn()).isEqualTo(2592000L);
            assertThat(response.getData().getUser()).isEqualTo(userDTO);
            assertThat(response.getData().getRoles()).containsExactly("USER");

            // Verify user was updated and saved (failed attempts reset)
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedAttempts()).isZero();
            assertThat(savedUser.getLockUntil()).isNull();
            assertThat(savedUser.getLastLoginAttempt()).isNotNull();

            // Verify refresh token was saved
            verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
            RefreshToken savedToken = refreshTokenCaptor.getValue();
            assertThat(savedToken.getToken()).isEqualTo(hashedRefreshToken);
            assertThat(savedToken.getUserEmail()).isEqualTo(testEmail);
            assertThat(savedToken.isRevoked()).isFalse();
            assertThat(savedToken.getExpiryDate()).isNotNull();

            verifyNoMoreInteractions(userRepository, refreshTokenRepository, jwtService, passwordEncoder, modelMapper);
        }

        @Test
        @DisplayName("login_whenEmailNotFound_throwsUnauthorizedAccessException")
        void login_whenEmailNotFound_throwsUnauthorizedAccessException() {
            // Arrange
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Invalid email or password");

            verify(userRepository).findByEmail(testEmail.toLowerCase());
            verifyNoMoreInteractions(userRepository, passwordEncoder, jwtService, refreshTokenRepository, modelMapper);
        }

        @Test
        @DisplayName("login_whenUserDisabled_throwsUnauthorizedAccessException")
        void login_whenUserDisabled_throwsUnauthorizedAccessException() {
            // Arrange
            User disabledUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(false)
                    .failedAttempts(0)
                    .lockUntil(null)
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(disabledUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Your account is disabled");

            verify(userRepository).findByEmail(testEmail.toLowerCase());
            verifyNoMoreInteractions(userRepository, passwordEncoder, jwtService, refreshTokenRepository, modelMapper);
        }

        @Test
        @DisplayName("login_whenAccountLocked_throwsUnauthorizedAccessException")
        void login_whenAccountLocked_throwsUnauthorizedAccessException() {
            // Arrange
            User lockedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(true)
                    .failedAttempts(5)
                    .lockUntil(Instant.now().plusSeconds(300))
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(lockedUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Invalid email or password");

            verify(userRepository).findByEmail(testEmail.toLowerCase());
            verifyNoMoreInteractions(userRepository, passwordEncoder, jwtService, refreshTokenRepository, modelMapper);
        }

        @Test
        @DisplayName("login_whenLockExpired_resetsLockAndAllowsLogin")
        void login_whenLockExpired_resetsLockAndAllowsLogin() {
            // Arrange
            User lockedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(true)
                    .failedAttempts(5)
                    .lockUntil(Instant.now().minusSeconds(300))
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(lockedUser));
            given(passwordEncoder.matches(testPassword, lockedUser.getPassword())).willReturn(true);
            given(jwtService.generateAccessToken(testEmail)).willReturn(testAccessToken);
            given(jwtService.generateRefreshToken(testEmail)).willReturn(testRefreshToken);
            given(jwtService.getAccessTokenExpirationSeconds()).willReturn(900L);
            given(jwtService.getRefreshTokenExpirationSeconds()).willReturn(2592000L);
            given(modelMapper.map(any(User.class), eq(UserDTO.class))).willReturn(new UserDTO());

            // Act
            Response<LoginResponse> response = authService.login(validLoginRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getFailedAttempts()).isZero();
            assertThat(savedUser.getLockUntil()).isNull();
        }

        @Test
        @DisplayName("login_whenInvalidPassword_incrementsFailedAttemptsAndLocksAfter5")
        void login_whenInvalidPassword_incrementsFailedAttemptsAndLocksAfter5() {
            // Arrange
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(testPassword, testUser.getPassword())).willReturn(false);

            // Act & Assert - First 4 attempts
            for (int i = 1; i <= 4; i++) {
                assertThatThrownBy(() -> authService.login(validLoginRequest))
                        .isInstanceOf(UnauthorizedAccessException.class)
                        .hasMessage("Invalid email or password");

                verify(userRepository, org.mockito.Mockito.times(i)).save(userCaptor.capture());
                List<User> allSavedUsers = userCaptor.getAllValues();
                User savedUser = allSavedUsers.getLast();
                assertThat(savedUser.getFailedAttempts()).isEqualTo(i);
                assertThat(savedUser.getLockUntil()).isNull();
            }

            // 5th attempt - should lock account
            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Invalid email or password");

            verify(userRepository, org.mockito.Mockito.times(5)).save(userCaptor.capture());
            List<User> allSavedUsers = userCaptor.getAllValues();
            User savedUser = allSavedUsers.getLast();
            assertThat(savedUser.getFailedAttempts()).isEqualTo(5);
            assertThat(savedUser.getLockUntil()).isNotNull();
            assertThat(savedUser.getLockUntil()).isAfter(Instant.now());
            assertThat(savedUser.getLockUntil()).isBeforeOrEqualTo(Instant.now().plusSeconds(900)); // 15 minutes
        }

        @Test
        @DisplayName("login_whenEmailHasWhitespace_normalizesEmail")
        void login_whenEmailHasWhitespace_normalizesEmail() {
            // Arrange
            LoginRequest requestWithWhitespace = new LoginRequest();
            requestWithWhitespace.setEmail("  " + testEmail + "  ");
            requestWithWhitespace.setPassword(testPassword);

            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(testPassword, testUser.getPassword())).willReturn(true);
            given(jwtService.generateAccessToken(testEmail)).willReturn(testAccessToken);
            given(jwtService.generateRefreshToken(testEmail)).willReturn(testRefreshToken);
            given(jwtService.getAccessTokenExpirationSeconds()).willReturn(900L);
            given(jwtService.getRefreshTokenExpirationSeconds()).willReturn(2592000L);
            given(modelMapper.map(any(User.class), eq(UserDTO.class))).willReturn(new UserDTO());

            // Act
            authService.login(requestWithWhitespace);

            // Assert
            verify(userRepository).findByEmail(testEmail.toLowerCase());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"invalid-email", "test@", "@example.com"})
        @DisplayName("login_whenEmailIsInvalid_throwsUnauthorizedAccessException")
        void login_whenEmailIsInvalid_throwsUnauthorizedAccessException(String invalidEmail) {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setEmail(invalidEmail);
            request.setPassword(testPassword);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("register() Tests")
    class RegisterTests {

        @Test
        @DisplayName("register_whenValidRequest_returnsUserDTO")
        void register_whenValidRequest_returnsUserDTO() {
            // Arrange
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());
            given(roleRepository.findByRoleName("USER")).willReturn(Optional.of(userRole));
            given(passwordEncoder.encode(testPassword)).willReturn("encoded-password");

            User savedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(true)
                    .failedAttempts(0)
                    .lockUntil(null)
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserDTO userDTO = new UserDTO();
            userDTO.setId(1L);
            userDTO.setUsername("testuser");
            userDTO.setEmail(testEmail);
            given(modelMapper.map(any(User.class), eq(UserDTO.class))).willReturn(userDTO);

            // Act
            Response<UserDTO> response = authService.register(validRegisterRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.getMessage()).isEqualTo("User registered successfully");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getId()).isEqualTo(1L);
            assertThat(response.getData().getUsername()).isEqualTo("testuser");
            assertThat(response.getData().getEmail()).isEqualTo(testEmail);

            verify(userRepository).save(userCaptor.capture());
            User createdUser = userCaptor.getValue();
            assertThat(createdUser.getUsername()).isEqualTo("testuser");
            assertThat(createdUser.getEmail()).isEqualTo(testEmail.toLowerCase());
            assertThat(createdUser.getPassword()).isEqualTo("encoded-password");
            assertThat(createdUser.isEnabled()).isTrue();
            assertThat(createdUser.getFailedAttempts()).isZero();
            assertThat(createdUser.getLockUntil()).isNull();
            assertThat(createdUser.getLastLoginAttempt()).isNull();
            assertThat(createdUser.getRoles()).hasSize(1);
            assertThat(createdUser.getRoles().iterator().next().getRoleName()).isEqualTo("USER");
        }

        @Test
        @DisplayName("register_whenEmailAlreadyExists_throwsConflictException")
        void register_whenEmailAlreadyExists_throwsConflictException() {
            // Arrange
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email already exists");

            verify(userRepository).findByEmail(testEmail.toLowerCase());
            verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder, modelMapper);
        }

        @Test
        @DisplayName("register_whenDefaultRoleNotFound_throwsNotFoundException")
        void register_whenDefaultRoleNotFound_throwsNotFoundException() {
            // Arrange
            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());
            given(roleRepository.findByRoleName("USER")).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Default role User not found");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"short", "nouppercase1!", "NOLOWERCASE1!", "NoDigits!", "NoSpecialChar1"})
        @DisplayName("register_whenPasswordInvalid_throwsIllegalArgumentException")
        void register_whenPasswordInvalid_throwsIllegalArgumentException(String invalidPassword) {
            // Arrange
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail(testEmail);
            request.setPassword(invalidPassword);

            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());
            given(roleRepository.findByRoleName("USER")).willReturn(Optional.of(userRole));

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("register_whenEmailHasWhitespace_normalizesEmail")
        void register_whenEmailHasWhitespace_normalizesEmail() {
            // Arrange
            RegisterRequest requestWithWhitespace = new RegisterRequest();
            requestWithWhitespace.setUsername("testuser");
            requestWithWhitespace.setEmail("  " + testEmail + "  ");
            requestWithWhitespace.setPassword(testPassword);

            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());
            given(roleRepository.findByRoleName("USER")).willReturn(Optional.of(userRole));
            given(passwordEncoder.encode(testPassword)).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willReturn(User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(true)
                    .failedAttempts(0)
                    .lockUntil(null)
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build());
            given(modelMapper.map(any(User.class), eq(UserDTO.class))).willReturn(new UserDTO());

            // Act
            authService.register(requestWithWhitespace);

            // Assert
            verify(userRepository).findByEmail(testEmail.toLowerCase());
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(testEmail.toLowerCase());
        }
    }

    @Nested
    @DisplayName("refreshToken() Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken_whenValidToken_returnsNewTokens")
        void refreshToken_whenValidToken_returnsNewTokens() {
            // Arrange
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";
            String hashedNewRefreshToken = TokenHasher.sha256(newRefreshToken);

            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn(testEmail);

            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                    .id(1L)
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));
            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
            given(jwtService.generateRefreshToken(testEmail)).willReturn(newRefreshToken);
            given(jwtService.generateAccessToken(testEmail)).willReturn(newAccessToken);
            given(jwtService.getAccessTokenExpirationSeconds()).willReturn(900L);
            given(jwtService.getRefreshTokenExpirationSeconds()).willReturn(2592000L);
            given(modelMapper.map(any(User.class), eq(UserDTO.class))).willReturn(new UserDTO());

            // Act
            Response<LoginResponse> response = authService.refreshToken(testRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo("Token refreshed successfully");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getToken()).isEqualTo(newAccessToken);
            assertThat(response.getData().getRefreshToken()).isEqualTo(newRefreshToken);

            // Verify both saves (old token revoked + new token created)
            verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(refreshTokenCaptor.capture());
            List<RefreshToken> allSavedTokens = refreshTokenCaptor.getAllValues();

            // First save: old token revoked
            RefreshToken savedOldToken = allSavedTokens.getFirst();
            assertThat(savedOldToken.isRevoked()).isTrue();
            assertThat(savedOldToken.getRevokedDate()).isNotNull();
            assertThat(savedOldToken.getReplacedByToken()).isEqualTo(hashedNewRefreshToken);

            // Second save: new token created
            RefreshToken savedNewToken = allSavedTokens.get(1);
            assertThat(savedNewToken.getToken()).isEqualTo(hashedNewRefreshToken);
            assertThat(savedNewToken.getUserEmail()).isEqualTo(testEmail);
            assertThat(savedNewToken.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("refreshToken_whenTokenNotFound_throwsUnauthorizedAccessException")
        void refreshToken_whenTokenNotFound_throwsUnauthorizedAccessException() {
            // Arrange
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn(testEmail);
            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);
            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(testRefreshToken))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("refreshToken_whenTokenSubjectMismatch_throwsUnauthorizedAccessException")
        void refreshToken_whenTokenSubjectMismatch_throwsUnauthorizedAccessException() {
            // Arrange
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn("other@example.com");
            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(testRefreshToken))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Token subject mismatch");
        }

        @Test
        @DisplayName("refreshToken_whenTokenRevoked_revokesAllUserTokensAndThrows")
        void refreshToken_whenTokenRevoked_revokesAllUserTokensAndThrows() {
            // Arrange
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn(testEmail);
            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(true)
                    .build();

            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(testRefreshToken))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Revoked token reused. Session terminated.");

            verify(refreshTokenRepository).revokeAllByUserEmail(testEmail);
        }

        @Test
        @DisplayName("refreshToken_whenTokenExpired_throwsUnauthorizedAccessException")
        void refreshToken_whenTokenExpired_throwsUnauthorizedAccessException() {
            // Arrange
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn(testEmail);
            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(testRefreshToken))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Expired refresh token");
        }

        @Test
        @DisplayName("refreshToken_whenUserNotFound_throwsUnauthorizedAccessException")
        void refreshToken_whenUserNotFound_throwsUnauthorizedAccessException() {
            // Arrange
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn(testEmail);
            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));
            given(userRepository.findByEmail(testEmail)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(testRefreshToken))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("refreshToken_whenUserDisabled_throwsUnauthorizedAccessException")
        void refreshToken_whenUserDisabled_throwsUnauthorizedAccessException() {
            // Arrange
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.getSubject()).willReturn(testEmail);
            given(jwtService.parseRefreshClaims(testRefreshToken)).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            User disabledUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(false)
                    .failedAttempts(0)
                    .lockUntil(null)
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));
            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(disabledUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(testRefreshToken))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("Account is disabled");
        }

        @Test
        @DisplayName("refreshToken_whenJwtException_returnsErrorResponse")
        void refreshToken_whenJwtException_returnsErrorResponse() {
            // Arrange
            given(jwtService.parseRefreshClaims(testRefreshToken))
                    .willThrow(new JwtException("Invalid token"));

            // Act
            Response<LoginResponse> response = authService.refreshToken(testRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getMessage()).isEqualTo("Invalid or expired refresh token");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("refreshToken_whenIllegalArgumentException_returnsErrorResponse")
        void refreshToken_whenIllegalArgumentException_returnsErrorResponse() {
            // Arrange
            given(jwtService.parseRefreshClaims(testRefreshToken))
                    .willThrow(new IllegalArgumentException("Invalid token"));

            // Act
            Response<LoginResponse> response = authService.refreshToken(testRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getMessage()).isEqualTo("Invalid or expired refresh token");
        }
    }

    @Nested
    @DisplayName("logout() Tests")
    class LogoutTests {

        @Test
        @DisplayName("logout_whenValidToken_revokesTokenAndReturnsSuccess")
        void logout_whenValidToken_revokesTokenAndReturnsSuccess() {
            // Arrange
            RefreshToken storedToken = RefreshToken.builder()
                    .id(1L)
                    .token(hashedRefreshToken)
                    .userEmail(testEmail)
                    .expiryDate(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.of(storedToken));

            // Act
            Response<Void> response = authService.logout(testRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo("Logged out successfully");

            verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
            RefreshToken savedToken = refreshTokenCaptor.getValue();
            assertThat(savedToken.isRevoked()).isTrue();
            assertThat(savedToken.getRevokedDate()).isNotNull();
        }

        @Test
        @DisplayName("logout_whenTokenNotFound_returnsErrorResponse")
        void logout_whenTokenNotFound_returnsErrorResponse() {
            // Arrange
            given(refreshTokenRepository.findByToken(hashedRefreshToken)).willReturn(Optional.empty());

            // Act
            Response<Void> response = authService.logout(testRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getMessage()).isEqualTo("Invalid refresh token");
        }

        @Test
        @DisplayName("logout_whenExceptionThrown_returnsErrorResponse")
        void logout_whenExceptionThrown_returnsErrorResponse() {
            // Arrange
            given(refreshTokenRepository.findByToken(hashedRefreshToken))
                    .willThrow(new RuntimeException("Database error"));

            // Act
            Response<Void> response = authService.logout(testRefreshToken);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getMessage()).isEqualTo("Invalid refresh token");
        }
    }

    @Nested
    @DisplayName("validatePasswordStrength() Tests")
    class ValidatePasswordStrengthTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"short", "1234567", "nouppercase1!", "NOLOWERCASE1!", "NoDigits!", "NoSpecialChar1"})
        @DisplayName("validatePasswordStrength_whenInvalid_throwsIllegalArgumentException")
        void validatePasswordStrength_whenInvalid_throwsIllegalArgumentException(String invalidPassword) {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail(testEmail);
            request.setPassword(invalidPassword);

            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());
            given(roleRepository.findByRoleName("USER")).willReturn(Optional.of(userRole));

            // Act & Assert - validation happens after role lookup
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class);

            // Verify no user save or other operations occurred
            verify(userRepository).findByEmail(testEmail.toLowerCase());
            verify(roleRepository).findByRoleName("USER");
            verifyNoMoreInteractions(userRepository, roleRepository, passwordEncoder, modelMapper);
        }

        @Test
        @DisplayName("validatePasswordStrength_whenValid_doesNotThrow")
        void validatePasswordStrength_whenValid_doesNotThrow() {
            // Arrange
            String validPassword = "ValidPass123!";
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail(testEmail);
            request.setPassword(validPassword);

            given(userRepository.findByEmail(testEmail.toLowerCase())).willReturn(Optional.empty());
            given(roleRepository.findByRoleName("USER")).willReturn(Optional.of(userRole));
            given(passwordEncoder.encode(validPassword)).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willReturn(User.builder()
                    .id(1L)
                    .username("testuser")
                    .email(testEmail)
                    .password("encoded-password")
                    .enabled(true)
                    .failedAttempts(0)
                    .lockUntil(null)
                    .lastLoginAttempt(null)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build());
            given(modelMapper.map(any(User.class), eq(UserDTO.class))).willReturn(new UserDTO());

            // Act & Assert - should not throw
            Response<UserDTO> response = authService.register(request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        }
    }
}