/*-
 * ============LICENSE_START=======================================================
 * dcae-inventory
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.dcae.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.configuration.UrlConfigurationSourceProvider;
import org.onap.dcae.inventory.clients.DatabusControllerClient;
import org.onap.dcae.inventory.exceptions.mappers.DBIExceptionMapper;
import org.onap.dcae.inventory.providers.NotFoundExceptionMapper;
import org.onap.dcae.inventory.daos.InventoryDAOManager;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.api.DcaeServiceTypesApi;
import io.swagger.api.DcaeServicesApi;
import io.swagger.api.DcaeServicesGroupbyApi;
import io.swagger.api.HealthCheckApi;
import io.swagger.api.ServiceHealthCheckApi;
import io.swagger.api.factories.DcaeServicesApiServiceFactory;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.validation.Validator;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Link;
import java.util.EnumSet;
import java.util.Locale;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * Created by mhwang on 4/11/16.
 */
public class InventoryApplication extends Application<InventoryConfiguration> {

    static final Logger metricsLogger = LoggerFactory.getLogger("metricsLogger");
	static final Logger debugLogger = LoggerFactory.getLogger("debugLogger");
	static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");
    static boolean shouldRemoteFetchConfig = false;
    //static final String configFile = "/opt/config.json";
    static final String configFile = "config.json";

    public static void main(String[] args) throws Exception {
    	metricsLogger.info("DCAE inventory application main Startup");
    	errorLogger.info("DCAE inventory application main Startup");
        // This is here to try to fix a "high" issue caught by Fortify. Did this **plus** setting locale for each of the
        // string comparisons that use `toUpper` because of this StackOverflow post:
        // http://stackoverflow.com/questions/38308777/fixed-fortify-scan-locale-changes-are-reappearing
        Locale.setDefault(Locale.ENGLISH);

        if (args.length < 2 && "server".equals(args[0])) {
            // When the start command is just "server", this will trigger inventory to look for its configuration
            // from Consul's KV store.  The url is hardcoded here which should be used as the "path" variable into
            // the UrlConfigurationSourceProvider.
            String[] customArgs = new String[args.length+1];
            System.arraycopy(args, 0, customArgs, 0, args.length);
            customArgs[args.length] = "http://consul:8500/v1/kv/inventory?raw=true";
            shouldRemoteFetchConfig = true;

            new InventoryApplication().run(customArgs);
        } else {
            // You are here because you want to use the default way of configuring inventory - YAML file.
        	debugLogger.debug("Using local config file -> " + args[1]);
        	errorLogger.info("Using local config file -> " + args[1]);
			createConfigFileFromTemplate(args[1]); 
			args[1] = configFile;
            new InventoryApplication().run(args);
        }
        // revert to using logback.xml:
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
    	context.reset();
    	ContextInitializer initializer = new ContextInitializer(context);
    	initializer.autoConfig();
    	
    	metricsLogger.info("Starting DCAE inventory application...");
    	debugLogger.debug(String.format("Starting DCAE inventory application... args[0]: %s", args[0]));
    	errorLogger.info(String.format("Starting DCAE inventory application... args[0]: %s", args[0]));
    	
    }

    @Override
    public String getName() {
        return "dcae-inventory";
    }

    private static class JsonConfigurationFactoryFactory<T> implements ConfigurationFactoryFactory<T> {
        @Override
        public ConfigurationFactory<T> create(Class<T> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
            return new JsonConfigurationFactory(klass, validator, objectMapper, propertyPrefix);
        }
    }

    @Override
    public void initialize(Bootstrap<InventoryConfiguration> bootstrap) {
        // This Info object was lifted from the Swagger generated io.swagger.api.Bootstrap file. Although it was not generated
        // correctly.
        Info info = new Info().title("DCAE Inventory API").version("0.8.1")
                .description("DCAE Inventory is a web service that provides the following:\n\n1. Real-time data on all DCAE services and their components\n2. Comprehensive details on available DCAE service types\n")
                .contact(new Contact().email("dcae@lists.onap.org"));
        // Swagger/servlet/jax-rs magic!
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setInfo(info);
        beanConfig.setResourcePackage("io.swagger.api");
        beanConfig.setScan(true);

        if (shouldRemoteFetchConfig) {
            // You are here because the configuration is sitting on a remote server in json format
            bootstrap.setConfigurationSourceProvider(new UrlConfigurationSourceProvider());
            bootstrap.setConfigurationFactoryFactory(new JsonConfigurationFactoryFactory<>());
        }
    }

    @Override
    public void run(InventoryConfiguration configuration, Environment environment) {
        debugLogger.info("Starting DCAE inventory application");
        errorLogger.info("Starting DCAE inventory application");
        debugLogger.info(String.format("DB driver properties: %s", configuration.getDataSourceFactory().getProperties().toString()));
        errorLogger.info(String.format("DB driver properties: %s", configuration.getDataSourceFactory().getProperties().toString()));
        InventoryDAOManager.getInstance().setup(environment, configuration);
        InventoryDAOManager.getInstance().initialize();

        // Add filter for CORS support for DCAE dashboard
        // http://jitterted.com/tidbits/2014/09/12/cors-for-dropwizard-0-7-x/
        // https://gist.github.com/yunspace/07d80a9ac32901f1e149#file-dropwizardjettycrossoriginintegrationtest-java-L11
        FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORSFilter", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");

        // Want to serialize Link in a way we like
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Link.class, new LinkSerializer());
        environment.getObjectMapper().registerModule(simpleModule);

        // Setup Databus controller client
        // Used by the dcae-services API
        if (configuration.getDatabusControllerConnection().getRequired()) {
            final Client clientDatabusController = new JerseyClientBuilder(environment).using(configuration.getJerseyClientConfiguration())
                    .build("DatabusControllerClient");
            clientDatabusController.register(HttpAuthenticationFeature.basicBuilder().credentials(
                    configuration.getDatabusControllerConnection().getMechId(),
                    configuration.getDatabusControllerConnection().getPassword()).build());
            final DatabusControllerClient databusControllerClient = new DatabusControllerClient(clientDatabusController,
                    configuration.getDatabusControllerConnection());
            DcaeServicesApiServiceFactory.setDatabusControllerClient(databusControllerClient);
            debugLogger.info("Use of DCAE controller client is required. Turned on.");
            errorLogger.info("Use of DCAE controller client is required. Turned on.");
        } else {
            debugLogger.warn("Use of DCAE controller client is *not* required. Turned off.");
            errorLogger.warn("Use of DCAE controller client is *not* required. Turned off.");
        }

        environment.jersey().register(NotFoundExceptionMapper.class);
        environment.jersey().register(DBIExceptionMapper.UnableToObtainConnectionExceptionMapper.class);
        environment.jersey().register(DBIExceptionMapper.UnableToExecuteStatementExceptionMapper.class);
        environment.jersey().register(DBIExceptionMapper.UnableToCreateStatementExceptionMapper.class);

        environment.jersey().register(new DcaeServicesApi());
        environment.jersey().register(new DcaeServiceTypesApi());
        environment.jersey().register(new DcaeServicesGroupbyApi());
        environment.jersey().register(new HealthCheckApi());
        environment.jersey().register(new ServiceHealthCheckApi());

        // https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-Jersey-2.X-Project-Setup-1.5
        environment.jersey().register(new ApiListingResource());
        environment.jersey().register(new SwaggerSerializers());
    }
    
    private static void createConfigFileFromTemplate (String templateFile) {
		debugLogger.debug("createConfigFileFromTemplate file: " + templateFile);
		errorLogger.debug("createConfigFileFromTemplate file: " + templateFile);

		try {
			JSONObject configTemplate = new JSONObject ( new JSONTokener ( new FileInputStream ( new File ( templateFile ) ) ) );
			JSONObject dB = configTemplate.getJSONObject("database");

			String dBUserEnvVarName = dB.getString("user").substring(1);
			String dBUser = envVarVal(dBUserEnvVarName);

			if ( null != dBUser ) {
				dB.put("user", dBUser);
			} 

			String dBPwdEnvVarName = dB.getString("password").substring(1);
			String dBPwd = envVarVal(dBPwdEnvVarName);

			if ( null != dBPwd ) {
				dB.put("password", dBPwd);
			} 

			String dBUrlEnvVarName = dB.getString("url").substring(1);
			String dBUrl = envVarVal(dBUrlEnvVarName);

			if ( null != dBUrl ) {
				dB.put("url", dBUrl);
			}

			JSONObject server = configTemplate.getJSONObject("server");
			JSONArray applicationConnectors = server.getJSONArray("applicationConnectors");
			String serverPortEnvVarName = applicationConnectors.getJSONObject(0).getString("port").substring(1);
			String serverPort = envVarVal(serverPortEnvVarName);

			if ( null != serverPort ) {
				applicationConnectors.getJSONObject(0).put("port", serverPort);
			}
			
			if ( applicationConnectors.getJSONObject(0).getString("type").equalsIgnoreCase("https") ) {
				
				String keyStorePathEnvVarName = applicationConnectors.getJSONObject(0).getString("keyStorePath").substring(1);
				String keyStorePath = envVarVal(keyStorePathEnvVarName);
				if (null != keyStorePath) {
					applicationConnectors.getJSONObject(0).put("keyStorePath", keyStorePath);
				}
				String keyStorePasEnvVarName = applicationConnectors.getJSONObject(0).getString("keyStorePassword").substring(1);
				String keyStorePas = envVarVal(keyStorePasEnvVarName);
				if (null != keyStorePas) {
					applicationConnectors.getJSONObject(0).put("keyStorePassword", getFileContents(keyStorePas));
				} 
			}
			
			JSONArray adminConnectors = server.getJSONArray("adminConnectors");
			String serverAdminPortEnvVarName = adminConnectors.getJSONObject(0).getString("port").substring(1);
			String serverAdminPort = envVarVal(serverAdminPortEnvVarName);

			if ( null != serverAdminPort ) {
				adminConnectors.getJSONObject(0).put("port", serverAdminPort);
			}

			debugLogger.info("creating new configTemplate.toString(): " + configTemplate.toString() + ".");
			errorLogger.info("creating new configTemplate.toString(): " + configTemplate.toString() + ".");
			
			debugLogger.info("Creating config file: " + configFile + ".");
			errorLogger.info("Creating config file: " + configFile + ".");

			FileWriter fileWriter = new FileWriter(configFile);
			fileWriter.write(configTemplate.toString());
			fileWriter.flush();


		} catch (JSONException | FileNotFoundException e) {
			e.printStackTrace();
			debugLogger.error("Exiting. createConfigFileFromTemplate() JSONException | FileNotFoundException: Message: " + e.getMessage() + 
					" Trace: " + e.getStackTrace());
			errorLogger.error("Exiting. createConfigFileFromTemplate() JSONException | FileNotFoundException: Message: " + e.getMessage() + 
					" Trace: " + e.getStackTrace());
			System.exit(1);
		} catch ( Exception e ) {
			e.printStackTrace();
			debugLogger.error("Exiting. createConfigFileFromTemplate() Exception: Message: " + e.getMessage() + " Trace: " + e.getStackTrace());
			errorLogger.error("Exiting. createConfigFileFromTemplate() Exception: Message: " + e.getMessage() + " Trace: " + e.getStackTrace());
			System.exit(1);
		}
	}

	private static String envVarVal (String envVarName) {

		String envVarVal;

		try {
			if ( null == envVarName || envVarName.isEmpty() ) {
				envVarVal = null;
			}

			envVarVal = System.getenv(envVarName);

			if ( null == envVarVal || envVarVal.isEmpty() ) {
				envVarVal = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			debugLogger.error("isEnvVarValid() Exception for envVarName: " + envVarName + ". Exception message: Message: " + e.getMessage() + 
					" Trace: " + e.getStackTrace());
			errorLogger.error("isEnvVarValid() Exception for envVarName: " + envVarName + ". Exception message: Message: " + e.getMessage() + 
					" Trace: " + e.getStackTrace());
			envVarVal = null;
		}

		if ( null == envVarVal ) {
			debugLogger.error("Exiting. envVarVal(): missing env. variable: " + envVarName);
			errorLogger.error("Exiting. envVarVal(): missing env. variable: " + envVarName);
			System.exit(1);
		}

		debugLogger.info("envVarName: " + envVarName + " = " + envVarVal);
		errorLogger.info("envVarName: " + envVarName + " = " + envVarVal);

		return envVarVal;
	}
	
	public static String getFileContents (String filename) {

		File f = new File(filename);
		try {
			byte[] bytes = Files.readAllBytes(f.toPath());
			return new String(bytes,"UTF-8").trim();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			debugLogger.error("getFileContents() FileNotFoundException for filename: " + filename + ". Exiting. Exception: Message: " + e.getMessage() +
					" Trace: " + e.getStackTrace());
			errorLogger.error("getFileContents() FileNotFoundException for filename: " + filename + ". Exiting. Exception: Message: " + e.getMessage() +
					" Trace: " + e.getStackTrace());
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			debugLogger.error("getFileContents() IOException for filename: " + filename + ". Exiting. Exception: Message: " + e.getMessage() + 
					" Trace: " + e.getStackTrace());
			errorLogger.error("getFileContents() IOException for filename: " + filename + ". Exiting. Exception: Message: " + e.getMessage() + 
					" Trace: " + e.getStackTrace());
			System.exit(1);
		}
		return "";

	}

}
