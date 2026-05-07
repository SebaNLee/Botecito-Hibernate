package ar.edu.itba.paw.webapp.form.nuevo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuthFormValidationTest {

    @Test
    public void testRegisterFormPasswordConfirmationValidation() {
        final RegisterForm form = new RegisterForm();
        form.setPassword("password123");
        form.setConfirmPassword("different-password");

        Assertions.assertFalse(form.isPasswordConfirmationValid());
    }

    @Test
    public void testRegisterFormPasswordConfirmationAllowsBlankValuesForFieldValidators() {
        final RegisterForm form = new RegisterForm();
        form.setPassword("");
        form.setConfirmPassword("different-password");

        Assertions.assertTrue(form.isPasswordConfirmationValid());
    }

    @Test
    public void testPasswordResetFormPasswordConfirmationValidation() {
        final PasswordResetForm form = new PasswordResetForm();
        form.setPassword("new-password");
        form.setConfirmPassword("different-password");

        Assertions.assertFalse(form.isPasswordConfirmationValid());
    }
}
