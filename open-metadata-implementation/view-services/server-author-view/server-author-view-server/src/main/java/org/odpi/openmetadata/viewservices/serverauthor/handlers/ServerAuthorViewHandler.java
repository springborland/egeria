/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.viewservices.serverauthor.handlers;

import org.odpi.openmetadata.adminservices.client.MetadataAccessPointConfigurationClient;
import org.odpi.openmetadata.adminservices.client.MetadataServerConfigurationClient;
import org.odpi.openmetadata.adminservices.client.OMAGServerConfigurationClient;
import org.odpi.openmetadata.adminservices.configuration.properties.EnterpriseAccessConfig;
import org.odpi.openmetadata.adminservices.configuration.properties.OMAGServerConfig;
import org.odpi.openmetadata.adminservices.configuration.properties.ResourceEndpointConfig;
import org.odpi.openmetadata.adminservices.ffdc.exception.OMAGConfigurationErrorException;
import org.odpi.openmetadata.adminservices.ffdc.exception.OMAGInvalidParameterException;
import org.odpi.openmetadata.adminservices.ffdc.exception.OMAGNotAuthorizedException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.properties.beans.Connection;
import org.odpi.openmetadata.viewservices.serverauthor.api.ffdc.ServerAuthorExceptionHandler;
import org.odpi.openmetadata.viewservices.serverauthor.api.ffdc.ServerAuthorViewErrorCode;
import org.odpi.openmetadata.viewservices.serverauthor.api.ffdc.ServerAuthorViewServiceException;
import org.odpi.openmetadata.viewservices.serverauthor.api.properties.ResourceEndpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The TexViewHandler is initialised with the server the call should be sent to.
 * The handler exposes methods for functionality for the type explorer view
 */
public class ServerAuthorViewHandler {
//    private static final Logger log = LoggerFactory.getLogger(ServerAuthorViewHandler.class);

    /*
     * Specify a constant for the (max) length to which labels will be truncated.
     */
    private static final int TRUNCATED_STRING_LENGTH = 24;


    /*
    * viewServiceOptions should have been validated in the Admin layer.
    * The viewServiceOptions contains a list of resource endpoints that the
    * view service can connect to. It is formatted like this:
    * "resourceEndpoints" : [
                {
                    resourceCategory   : "Platform",
                    resourceName       : "Platform2",
                    resourceRootURL    : "https://localhost:9443"
                },
                {
                    resourceCategory   : "Platform",
                    resourceName       : "Platform1",
                    resourceRootURL    : "https://localhost:8082"
                },
                {
                    resourceCategory   : "Server",
                    resourceName       : "Metadata_Server1",
                    resourceRootURL    : "https://localhost:8082"
                },
                {
                    resourceCategory   : "Server",
                    resourceName       : "Metadata_Server2",
                    resourceRootURL    : "https://localhost:9443"
                }
            ]
    */
    private Map<String, ResourceEndpoint> configuredPlatforms = null;  // map is keyed using platformRootURL
    private Map<String, ResourceEndpoint> configuredServerInstances = null;  // map is keyed using serverName+platformRootURL so each instance is unique
    private String userId = null;
    private String platformURL = null;

    /**
     * Default constructor for ServerAuthorViewHandler
     */
    public ServerAuthorViewHandler() {

    }


    /**
     * ServerAuthorViewHandler constructor with configured resourceEndpoints
     *
     * @param localServerUserId admin userid
     * @param metadataServerURL url of the platform to issue admin calls to store configuration
     * @param resourceEndpoints list of platforms
     */
    public ServerAuthorViewHandler(String localServerUserId, String metadataServerURL, List<ResourceEndpointConfig> resourceEndpoints) {

        this.userId = localServerUserId;
        this.platformURL = metadataServerURL;
        /*
         * Populate map of resources with their endpoints....
         */

        // TODO - It would be desirable to add validation rules to ensure uniqueness etc.

        configuredPlatforms = new HashMap<>();
        configuredServerInstances = new HashMap<>();

        if (resourceEndpoints != null && !resourceEndpoints.isEmpty()) {

            resourceEndpoints.forEach(res -> {

                String resCategory = res.getResourceCategory();
                ResourceEndpoint rep = new ResourceEndpoint(res);

                String resName = null;

                switch (resCategory) {
                    case "Platform":
                        resName = res.getPlatformName();
                        configuredPlatforms.put(resName, rep);
                        break;
                    // At this time there is no need to pick up servers from the config for this view service.
                    default:
                        // Unsupported category is ignored
                        break;

                }
            });
        }
    }

    /**
     * getResourceEndpoints - returns a list of the configured resource endpoints. Does not include discovered resource endpoints.
     *
     * @param userId     userId under which the request is performed
     * @param methodName The name of the method being invoked
     *                   This method will return the resource endpoints that have been configured for the view service
     */
    public Map<String, List<ResourceEndpoint>> getResourceEndpoints(String userId, String methodName) {

        Map<String, List<ResourceEndpoint>> returnMap = new HashMap<>();

        List<ResourceEndpoint> platformList = null;
        List<ResourceEndpoint> serverList = null;

        if (!configuredPlatforms.isEmpty()) {
            platformList = new ArrayList<>();
            platformList.addAll(configuredPlatforms.values());
        }

        if (!configuredServerInstances.isEmpty()) {
            serverList = new ArrayList<>();
            serverList.addAll(configuredServerInstances.values());
        }

        returnMap.put("platformList", platformList);

        return returnMap;
    }


    /**
     * resolvePlatformRootURL
     * <p>
     * This method will look up the configured root URL for the named platform.
     *
     * @param platformName
     * @return resolved platform URL Root
     * @throws org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException
     */
    private String resolvePlatformRootURL(String platformName, String methodName) throws InvalidParameterException {
        String platformRootURL = null;

        if (platformName != null) {
            ResourceEndpoint resource = configuredPlatforms.get(platformName);
            if (resource != null) {
                platformRootURL = resource.getResourceRootURL();
            }
        }
        if (platformName == null || platformRootURL == null) {
            throw new InvalidParameterException(ServerAuthorViewErrorCode.VIEW_SERVICE_NULL_PLATFORM_NAME.getMessageDefinition(),
                                                this.getClass().getName(),
                                                methodName,
                                                "platformName");
        }

        return platformRootURL;
    }

    /**
     * Set the local repository to be in memory for the named server
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server being configured
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setInMemLocalRepository(String className, String methodName, String serverToBeConfiguredName
                                       ) throws ServerAuthorViewServiceException {

        try {
            MetadataServerConfigurationClient configurationClient = new MetadataServerConfigurationClient(this.userId,
                                                                                                          serverToBeConfiguredName,
                                                                                                          this.platformURL
            );
            configurationClient.setInMemLocalRepository();
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * Set the local repository to be graph for the named server
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server being configured
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setGraphLocalRepository(String className, String methodName, String serverToBeConfiguredName, Map<String, Object> storageProperties) throws ServerAuthorViewServiceException {
        try {
            MetadataServerConfigurationClient configurationClient = new MetadataServerConfigurationClient(this.userId,
                                                                                                          serverToBeConfiguredName,
                                                                                                          this.platformURL
            );
            configurationClient.setGraphLocalRepository(storageProperties);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * Set the local repository to be read only for the named server
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server being configured
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setReadOnlyLocalRepository(String className, String methodName, String serverToBeConfiguredName) throws ServerAuthorViewServiceException {
        try {
            MetadataServerConfigurationClient configurationClient = new MetadataServerConfigurationClient(this.userId,
                                                                                                          serverToBeConfiguredName,
                                                                                                          this.platformURL
            );
            configurationClient.setReadOnlyLocalRepository();
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * Get the stored configuration for the named server
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeRetrievedName name of the server whose configuration is to be retieved
     * @throws ServerAuthorViewServiceException server author exception
     */
    public OMAGServerConfig getStoredConfiguration(String className, String methodName, String serverToBeRetrievedName) throws ServerAuthorViewServiceException {
        OMAGServerConfig config = null;


        try {
            OMAGServerConfigurationClient adminServicesClient = new OMAGServerConfigurationClient(this.userId,
                                                                                                  serverToBeRetrievedName,
                                                                                                  this.platformURL);
            config = adminServicesClient.getOMAGServerConfig();
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
        return config;
    }
    /**
     * Deploy an OMAG Server configuration onto a target platform
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param destinationPlatformName  platform onto which the server is to be deployed
     * @param serverToBeDeployedName name of the server to deploy
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void deployOMAGServerConfig(String className, String methodName, String destinationPlatformName, String serverToBeDeployedName) throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient adminServicesClient = new OMAGServerConfigurationClient(this.userId,
                                                                                                  serverToBeDeployedName,
                                                                                                  this.platformURL);
            adminServicesClient.deployOMAGServerConfig(this.platformURL);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * Set an OMAG Server's configuration
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param omagServerConfig name of the server whose configuration we are setting
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setOMAGServerConfig(String className, String methodName, String serverToBeConfiguredName, OMAGServerConfig omagServerConfig) throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient adminServicesClient = new OMAGServerConfigurationClient(this.userId,
                                                                                                  serverToBeConfiguredName,
                                                                                                  this.platformURL);
            adminServicesClient.setOMAGServerConfig(omagServerConfig);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * Configure an access service
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param serviceURLMarker identifier of the access service to configure
     * @param accessServiceOptions access service options
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void configureAccessService(String className, String methodName, String serverToBeConfiguredName, String serviceURLMarker, Map<String, Object> accessServiceOptions) throws ServerAuthorViewServiceException {
        try {
            MetadataAccessPointConfigurationClient client = new MetadataAccessPointConfigurationClient(this.userId,
                                                                                                       serverToBeConfiguredName,
                                                                                                       this.platformURL);
            if (accessServiceOptions == null) {
                accessServiceOptions = new HashMap<>();
            }
            client.configureAccessService(serviceURLMarker, accessServiceOptions);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * Configure all access services
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param accessServiceOptions access service options to apply to all access services
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void configureAllAccessServices(String className, String methodName, String serverToBeConfiguredName, Map<String, Object> accessServiceOptions) throws ServerAuthorViewServiceException {
        try {
            MetadataAccessPointConfigurationClient client = new MetadataAccessPointConfigurationClient(this.userId,
                                                                                                       serverToBeConfiguredName,
                                                                                                       this.platformURL);
            if (accessServiceOptions == null) {
                accessServiceOptions = new HashMap<>();
            }
            client.configureAllAccessServices(accessServiceOptions);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * set the Enterprise Access config
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param enterpriseAccessConfig enterprise access config
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setEnterpriseAccessConfig(String className, String methodName, String serverToBeConfiguredName, EnterpriseAccessConfig enterpriseAccessConfig) throws ServerAuthorViewServiceException {
        try {
            MetadataAccessPointConfigurationClient client = new MetadataAccessPointConfigurationClient(this.userId,
                                                                                                       serverToBeConfiguredName,
                                                                                                       this.platformURL);
            client.setEnterpriseAccessConfig(enterpriseAccessConfig);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * set the Event bus
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param connectorProvider connector provider name
     * @param topicURLRoot topic URL root
     * @param configurationProperties configuration properties
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setEventBus(String className, String methodName, String serverToBeConfiguredName, String connectorProvider, String topicURLRoot, Map<String, Object> configurationProperties)
    throws ServerAuthorViewServiceException {
        try {

            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.setEventBus(connectorProvider, topicURLRoot, configurationProperties);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * set the default audit log
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void setDefaultAuditLog(String className, String methodName, String serverToBeConfiguredName) throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.setDefaultAuditLog();
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * add a console audit log
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param supportedSeverities a list of support severities
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void addConsoleAuditLogDestination(String className, String methodName, String serverToBeConfiguredName, List<String> supportedSeverities)
    throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.addConsoleAuditLogDestination(supportedSeverities);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * add a SLF4J audit log
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param supportedSeverities a list of support severities
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void addSLF4JAuditLogDestination(String className, String methodName, String serverToBeConfiguredName, List<String> supportedSeverities)
    throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.addSLF4JAuditLogDestination(supportedSeverities);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * add a File audit log destination
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param supportedSeverities a list of support severities
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void addFileAuditLogDestination(String className, String methodName, String serverToBeConfiguredName, List<String> supportedSeverities)
    throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.addFileAuditLogDestination(supportedSeverities);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * add a Event Topic audit log destination
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param supportedSeverities a list of support severities
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void addEventTopicAuditLogDestination(String className, String methodName, String serverToBeConfiguredName, List<String> supportedSeverities)
    throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.addEventTopicAuditLogDestination(supportedSeverities);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }
    /**
     * add an audit log destination specified by a connection
     * @param className class Name for diagnostic purposes
     * @param methodName current operation
     * @param serverToBeConfiguredName name of the server to being configured
     * @param connection connection to use for the audit log destination
     * @throws ServerAuthorViewServiceException server author exception
     */
    public void addAuditLogDestination(String className, String methodName, String serverToBeConfiguredName, Connection connection)
    throws ServerAuthorViewServiceException {
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToBeConfiguredName,
                                                                                     this.platformURL);
            client.addAuditLogDestination(connection);
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
    }

    /**
     * Get the active configuration of the named server
     * @param className class name used for diagnostics
     * @param methodName the current operation
     * @param serverToRetrieveName the server to retrive name
     * @return the activate configuration or
     * @throws ServerAuthorViewServiceException a server author exception
     */
    public OMAGServerConfig getActiveConfiguration(String className, String methodName, String serverToRetrieveName)
    throws ServerAuthorViewServiceException {
        OMAGServerConfig config = null;
        try {
            OMAGServerConfigurationClient client = new OMAGServerConfigurationClient(this.userId,
                                                                                     serverToRetrieveName,
                                                                                     this.platformURL);
            config = client.getOMAGServerInstanceConfig();
        } catch (OMAGNotAuthorizedException error) {
            throw ServerAuthorExceptionHandler.mapOMAGUserNotAuthorizedException(className, methodName, error);
        } catch (OMAGInvalidParameterException error) {
            throw ServerAuthorExceptionHandler.mapOMAGInvalidParameterException(className, methodName, error);
        } catch (OMAGConfigurationErrorException error) {
            throw ServerAuthorExceptionHandler.mapOMAGConfigurationErrorException(className, methodName, error);
        }
        return config;
    }

    /**
     * Activate the stored configuration for the named server on the named destination platform
     *
     * @param className               class name for diagnostics
     * @param methodName              action being performed
     * @param destinationPlatformName name of the platform name on which the configuration should be activated
     * @param serverToBeActivatedName server name of the configuration to activate
     */
    public void activateWithStoredConfig(String className, String methodName, String
            destinationPlatformName, String serverToBeActivatedName) {
        // TODO not yet implemented

    }

    /**
     * Deactivate the stored configuration for the named server on the named destination platform permanently. The configuration associated with this
     * server is deleted, so it cannot be activated again.
     * <p>
     * To activate this server again on this platform, a new configuration must be created, which can then be activated.
     *
     * @param className                 class name for diagnostics
     * @param methodName                action being performed
     * @param destinationPlatformName   name of the platform name on which the configuration should be activated
     * @param serverToBeDeactivatedName server name of the configuration to deactivate
     */
    public void deactivateServerPermanently(String className, String methodName, String
            destinationPlatformName, String serverToBeDeactivatedName) {
        // TODO not yet implemented
    }

    /**
     * Deactivate the stored configuration for the named server on the named destination platform temporarily. The configuration
     * still exists after this operation, so it can be activated again
     *
     * @param className                 class name for diagnostics
     * @param methodName                action being performed
     * @param destinationPlatformName   name of the platform name on which the configuration should be activated
     * @param serverToBeDeactivatedName server name of the configuration to deactivate
     */
    public void deactivateServerTemporarily(String className, String methodName, String destinationPlatformName, String serverToBeDeactivatedName) {
        // TODO not yet implemented
    }
}

