package sb.postgresqlaad;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import java.util.*;
import java.util.logging.Logger;

import com.microsoft.aad.msal4j.*;
import javax.security.auth.kerberos.KerberosPrincipal;

public class AADJDBCDriver implements java.sql.Driver {

  private final static Logger LOGGER = Logger.getLogger(AADJDBCDriver.class.getName());

  private final static String DRIVER_ALIAS = ":postgresqlaad:";
  private final static String DRIVER_URL_PREFIX = "jdbc" + DRIVER_ALIAS;

  private final static String PROPERTY_PASSWORD = "password";
  private final static String PROPERTY_USER = "user";
  private final static String PROPERTY_AAD_AUTHENTICATION = "aadAuthentication";
  private final static String PROPERTY_AAD_CLIENT_ID = "aadClientId";
  private final static String PROPERTY_AAD_TENANT_ID = "aadTenantId";

  private final static String POSTGRESQL_DRIVER_ALIAS = ":postgresql:";
  private final static String POSTGRESQL_DRIVER_CLASS= "org.postgresql.Driver";
  private final static String POSTGRESQL_DRIVER_APP_ID = "1950a258-227b-4e31-a9cf-717495945fc2";
  private final static String POSTGRESQL_DRIVER_AUTHORITY_BASE= "https://login.microsoftonline.com/";

  static {
    try {
      DriverManager.registerDriver(new AADJDBCDriver());
    } catch(Exception e) {
      throw new RuntimeException("Can't register driver!", e);
    }
  }

  private Driver _postgreSqlDriver;
  private PublicClientApplication _publicClientApplication;

  public AADJDBCDriver() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    _postgreSqlDriver = (Driver) Class.forName(POSTGRESQL_DRIVER_CLASS).newInstance();
  }

  public static String generateAuthTokenManagedIdentity(String clientId) {
    try
    {
        String token = null;

        String msiUrl = "http://169.254.169.254/metadata/identity/oauth2/token?api-version=2018-02-01&resource=https%3A%2F%2Fossrdbms-aad.database.windows.net";
        if(clientId != "" && clientId != null){
            msiUrl += "&client_id=" + clientId;
        }
        
        URL msiEndpoint = new URL(msiUrl);
        HttpURLConnection con = (HttpURLConnection) msiEndpoint.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Metadata", "true");
 
        if (con.getResponseCode()!=200) {
            throw new Exception("Error calling managed identity token endpoint.");
        }
 
        InputStream responseStream = con.getInputStream();
 
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(responseStream);
 
        while(!parser.isClosed()){
            JsonToken jsonToken = parser.nextToken();
 
            if(JsonToken.FIELD_NAME.equals(jsonToken)){
                String fieldName = parser.getCurrentName();
                jsonToken = parser.nextToken();
                
                if("access_token".equals(fieldName)){
                    String accesstoken = parser.getValueAsString();
                    token = accesstoken;
                }
            }
        }
        
        return token;
    }
    catch(Exception e)
    {
        return "";
    }        
  }

  public String generateAuthTokenInteractive(String tenantId) {
    try
    { 
        if(_publicClientApplication == null){
            String authority = POSTGRESQL_DRIVER_AUTHORITY_BASE + tenantId + "/";
            _publicClientApplication = PublicClientApplication.builder(POSTGRESQL_DRIVER_APP_ID)
                .authority(authority)
                .build();        
        }

        Set<IAccount> accountsInCache = _publicClientApplication.getAccounts().join();

        IAccount account = null;
        if(!accountsInCache.isEmpty()){
             account = accountsInCache.iterator().next();
        }

        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                        .builder(Collections.singleton("https://ossrdbms-aad.database.windows.net/.default"))
                        .account(account)
                        .build();

                result = _publicClientApplication.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {

                InteractiveRequestParameters parameters = InteractiveRequestParameters
                    .builder(new URI("http://localhost"))
                    .scopes(Collections.singleton("https://ossrdbms-aad.database.windows.net/.default"))
                    .build();

                result = _publicClientApplication.acquireToken(parameters).join();
            } else {
                throw ex;
            }
        }
        
        return result.accessToken();
    }
    catch(Exception e)
    {
        System.out.println(e.getMessage());
        return "";
    }        
  }

    public String generateAuthTokenIntegrated(String tenantId) {
    try
    { 
        if(_publicClientApplication == null){
            String authority = POSTGRESQL_DRIVER_AUTHORITY_BASE + tenantId + "/";
            _publicClientApplication = PublicClientApplication.builder(POSTGRESQL_DRIVER_APP_ID)
                .authority(authority)
                .build();        
        }

        Set<IAccount> accountsInCache = _publicClientApplication.getAccounts().join();

        IAccount account = null;
        if(!accountsInCache.isEmpty()){
             account = accountsInCache.iterator().next();
        }

        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                        .builder(Collections.singleton("https://ossrdbms-aad.database.windows.net/.default"))
                        .account(account)
                        .build();

                result = _publicClientApplication.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {
                KerberosPrincipal kerberosPrincipal = new KerberosPrincipal("username");
                String user = kerberosPrincipal.getName();
                
                IntegratedWindowsAuthenticationParameters parameters = IntegratedWindowsAuthenticationParameters
                    .builder(Collections.singleton("https://ossrdbms-aad.database.windows.net/.default"), user)
                    .build();

                result = _publicClientApplication.acquireToken(parameters).join();
            } else {
                throw ex;
            }
        }
        
        return result.accessToken();
    }
    catch(Exception e)
    {
        System.out.println(e.getMessage());
        return "";
    }        
  }

  public boolean acceptsURL(String url) throws SQLException {
    return url != null && url.startsWith(DRIVER_URL_PREFIX);
  }

  public Connection connect(String url, Properties properties) throws SQLException {
    if(!acceptsURL(url)) {
      throw new SQLException("Invalid url: '" + url + "'");
    }
    String postgreSQLUrl = url.replace(DRIVER_ALIAS, POSTGRESQL_DRIVER_ALIAS);
    URI uri = URI.create(postgreSQLUrl.substring(5));

    String authentication = properties.getProperty(PROPERTY_AAD_AUTHENTICATION);
    
    
    if("ActiveDirectoryManagedIdentity".equals(authentication)){
        String password = generateAuthTokenManagedIdentity(
          properties.getProperty(PROPERTY_AAD_CLIENT_ID)
        );    

        properties.setProperty(PROPERTY_PASSWORD, password);
    }
    else if("ActiveDirectoryInteractive".equals(authentication)){
        String password = generateAuthTokenInteractive(
          properties.getProperty(PROPERTY_AAD_TENANT_ID)
        );    

        properties.setProperty(PROPERTY_PASSWORD, password);
    }
    else if("ActiveDirectoryIntegrated".equals(authentication)){
        String password = generateAuthTokenIntegrated(
          properties.getProperty(PROPERTY_AAD_TENANT_ID)
        );    

        properties.setProperty(PROPERTY_PASSWORD, password);
    }
    return _postgreSqlDriver.connect(postgreSQLUrl, properties);
  }

  public int getMajorVersion() {
    return _postgreSqlDriver.getMajorVersion();
  }

  public int getMinorVersion() {
    return _postgreSqlDriver.getMinorVersion();
  }

  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return _postgreSqlDriver.getParentLogger();
  }

  public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException {
    DriverPropertyInfo[] info = _postgreSqlDriver.getPropertyInfo(url, properties);
    if(info != null) {
      ArrayList<DriverPropertyInfo> infoList = new ArrayList<DriverPropertyInfo>(Arrays.asList(info));
      infoList.add(new DriverPropertyInfo(PROPERTY_AAD_CLIENT_ID, null));
      infoList.add(new DriverPropertyInfo(PROPERTY_AAD_TENANT_ID, null));
      infoList.add(new DriverPropertyInfo(PROPERTY_AAD_AUTHENTICATION, null));
      info = infoList.toArray(new DriverPropertyInfo[infoList.size()]);
    }
    return info;
  }

  public boolean jdbcCompliant() {
    return _postgreSqlDriver.jdbcCompliant();
  }

  private String getUsernameFromUriOrProperties(URI uri, Properties properties) {
    String username = properties.getProperty(PROPERTY_USER);

    if(username == null) {
      final String userInfo = uri.getUserInfo();
      if(userInfo != null) {
        username = userInfo.split(":")[0];
      }
    }

    return username;
  }
}