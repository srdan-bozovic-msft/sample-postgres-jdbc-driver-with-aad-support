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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

public class AADJDBCDriver implements java.sql.Driver {

  private final static Logger LOGGER = Logger.getLogger(AADJDBCDriver.class.getName());

  private final static String DRIVER_ALIAS = ":postgresqlaad:";
  private final static String DRIVER_URL_PREFIX = "jdbc" + DRIVER_ALIAS;

  private final static String PROPERTY_PASSWORD = "password";
  private final static String PROPERTY_USER = "user";
  private final static String PROPERTY_AAD_AUTHENTICATION = "aadAuthentication";
  private final static String PROPERTY_AAD_CLIENT_ID = "aadClientId";

  private final static String POSTGRESQL_DRIVER_ALIAS = ":postgresql:";
  private final static String POSTGRESQL_DRIVER_CLASS= "org.postgresql.Driver";

  static {
    try {
      DriverManager.registerDriver(new AADJDBCDriver());
    } catch(Exception e) {
      throw new RuntimeException("Can't register driver!", e);
    }
  }

  private Driver _postgreSqlDriver;

  public AADJDBCDriver() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    _postgreSqlDriver = (Driver) Class.forName(POSTGRESQL_DRIVER_CLASS).newInstance();
  }

  public static String generateAuthToken(String clientId) {
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
        String password = generateAuthToken(
          properties.getProperty(PROPERTY_AAD_CLIENT_ID)
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