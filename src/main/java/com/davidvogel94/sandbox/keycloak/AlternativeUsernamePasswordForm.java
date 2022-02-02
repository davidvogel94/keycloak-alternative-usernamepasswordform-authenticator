package com.davidvogel94.sandbox.keycloak;

import static org.keycloak.services.validation.Validation.FIELD_PASSWORD;
import static org.keycloak.services.validation.Validation.FIELD_USERNAME;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;


public class AlternativeUsernamePasswordForm extends UsernamePasswordForm implements Authenticator {
    protected static ServicesLogger log = ServicesLogger.LOGGER;

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel")) {
            log.debug("Form was canceled.");
            context.cancelLogin();
            return;
        }

        // Handle authentication failure
        if (!validateForm(context, formData)) {
            log.debug("Oops! User is not valid : wrong credentials or unknown");

            UserModel user = getUser(context, formData);
            
            if (user != null && !user.isEnabled()) {
                // Don't tell the user that they're disabled unless they got their password correct
                if(validatePassword(context, user, formData, true)) {
                    log.debug("Login was successful, however user account is disabled.");
                    context.getEvent().error(Errors.USER_DISABLED);
                    Response challengeResponse = challenge(context, Messages.ACCOUNT_DISABLED);
                    context.failureChallenge(AuthenticationFlowError.USER_DISABLED, challengeResponse);
                    return;
                }
                
                // Fallback to invalid user checks below if login failed
            } 

            log.debug("Resetting the flow, all stops here.");
            context.getEvent().error(Errors.USER_NOT_FOUND);

            Response challengeResponse = challenge(context, Messages.INVALID_USER);
            context.failureChallenge(AuthenticationFlowError.UNKNOWN_USER, challengeResponse);
            return;
        }

        log.debug("User is valid and authenticated.");
        context.success();
    }


    // This is copied more or less from the method of the same name in the UsernamePasswordForm class in the Keycloak source code
    //      I copied it here because I couldn't access the method in the base class as it is private
    private UserModel getUser(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_USERNAME);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return null;
        }

        // remove leading and trailing whitespace
        username = username.trim();

        context.getEvent().detail(Details.USERNAME, username);
        context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
        try {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException mde) {
            ServicesLogger.LOGGER.modelDuplicateException(mde);

            // Could happen during federation import
            if (mde.getDuplicateFieldName() != null && mde.getDuplicateFieldName().equals(UserModel.EMAIL)) {
                setDuplicateUserChallenge(context, Errors.EMAIL_IN_USE, Messages.EMAIL_EXISTS, AuthenticationFlowError.INVALID_USER);
            } else {
                setDuplicateUserChallenge(context, Errors.USERNAME_IN_USE, Messages.USERNAME_EXISTS, AuthenticationFlowError.INVALID_USER);
            }
            return user;
        }

        testInvalidUser(context, user);
        return user;
    }


    // This is pretty much a verbatim copy of the base class method, except it doesn't to brute force checks
    @Override
    public boolean validatePassword(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData, boolean clearUser) {
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()) {
            return badPasswordHandler(context, user, clearUser,true);
        }

        if (password != null && !password.isEmpty() && context.getSession().userCredentialManager().isValid(context.getRealm(), user, UserCredentialModel.password(password))) {
            return true;
        } else {
            return badPasswordHandler(context, user, clearUser,false);
        }
    }

    // Verbatim copy of the base class, except I couldn't use the base class method as it is private so I had to copy it here
    private boolean badPasswordHandler(AuthenticationFlowContext context, UserModel user, boolean clearUser,boolean isEmptyPassword) {
        context.getEvent().user(user);
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
        Response challengeResponse = challenge(context, getDefaultChallengeMessage(context), FIELD_PASSWORD);
        if(isEmptyPassword) {
            context.forceChallenge(challengeResponse);
        }else{
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }

        if (clearUser) {
            context.clearUser();
        }
        return false;
    }
}