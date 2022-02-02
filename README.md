forked from https://github.com/hatemzidi/keycloak-alternative-usernamepasswordform-authenticator

# keycloak-usernamepasswordform-authenticator
this custom authenticator implementation extends the original ``Username Password Form`` execuation flow in Keycloak.  
It will inform the user if their account has been locked if they get the username/password correct, but will only provide the generic "incorrect user/password" message if not.

# Deployment
```shell script
mvn clean package
cp target/*.jar $KEYCLOAK_HOME/standalone/deployments/
```

# Keycloak Configuration

1. Go to Authentication menu
2. Create or edit a custom flow
3. Add execution
4. Pick up from the list the ``Alternative Username Password Form``
