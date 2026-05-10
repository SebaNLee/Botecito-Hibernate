package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.nuevo.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.nuevo.RegisterForm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthModelMapperTest {

    private AuthModelMapper authModelMapper;

    @BeforeEach
    public void setUp() {
        authModelMapper = new AuthModelMapper();
    }

    @Test
    public void testFromRegisterFormMapsUserFieldsWithoutRawPassword() {
        final RegisterForm form = new RegisterForm();
        form.setGivenName(" Ada ");
        form.setLastName(" Lovelace ");
        form.setEmail(" ada@example.com ");
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setPaymentAlias("alias");
        form.setPreferredLanguage("en");

        final UserModel user = authModelMapper.fromRegisterForm(form);

        Assertions.assertEquals("Ada", user.getGivenName());
        Assertions.assertEquals("Lovelace", user.getLastName());
        Assertions.assertEquals("ada@example.com", user.getEmail());
        Assertions.assertEquals("alias", user.getPaymentAlias());
        Assertions.assertEquals(PreferredLanguageModel.EN, user.getPreferredLanguage());
        Assertions.assertNull(user.getPasswordHash());
        Assertions.assertNull(user.getPasswordRecoveryToken());
    }

    @Test
    public void testFromPasswordRecoveryRequestFormMapsTrimmedEmail() {
        final PasswordRecoveryRequestForm form = new PasswordRecoveryRequestForm();
        form.setEmail(" ada@example.com ");

        final UserModel user = authModelMapper.fromPasswordRecoveryRequestForm(form);

        Assertions.assertEquals("ada@example.com", user.getEmail());
    }

    @Test
    public void testFromPasswordResetFormMapsTokenWithoutRawPassword() {
        final PasswordResetForm form = new PasswordResetForm();
        form.setToken(" token ");
        form.setPassword("new-password");
        form.setConfirmPassword("new-password");

        final UserModel user = authModelMapper.fromPasswordResetForm(form);

        Assertions.assertEquals("token", user.getPasswordRecoveryToken());
        Assertions.assertNull(user.getPasswordHash());
    }
}
