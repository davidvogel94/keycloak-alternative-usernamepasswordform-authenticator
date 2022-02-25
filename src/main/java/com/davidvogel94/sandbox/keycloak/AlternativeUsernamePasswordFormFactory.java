package com.davidvogel94.sandbox.keycloak;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.DisplayTypeAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;


public class AlternativeUsernamePasswordFormFactory extends UsernamePasswordFormFactory implements AuthenticatorFactory, DisplayTypeAuthenticatorFactory {

    public static final String PROVIDER_ID = "alternative-username-password-form";
    public static final AlternativeUsernamePasswordForm SINGLETON = new AlternativeUsernamePasswordForm();
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Alternative Username Password Form";
    }

    @Override
    public String getHelpText() {
        return "Validates a username and password from login form. Informs the user that their account is disabled (e.g. from permanent lockout due to brute force attack) if and only if they supply the correct credentials.";
    }
}
