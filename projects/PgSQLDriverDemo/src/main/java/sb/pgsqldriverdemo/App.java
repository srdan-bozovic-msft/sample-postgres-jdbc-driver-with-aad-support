/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sb.pgsqldriverdemo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


/**
 *
 * @author cloudSA
 */
public class App{

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     */
    private Connection connectUAMI() {
        
        Connection conn = null;
        try {        
            String url = "jdbc:postgresqlaad://aaddemopg.postgres.database.azure.com/postgres";
            Properties props = new Properties();
            props.setProperty("user", "demopguami@aaddemopg");
            props.setProperty("aadAuthentication", "ActiveDirectoryManagedIdentity");
            props.setProperty("aadClientId", "e93cc508-1a65-45d3-8420-d34e6eafc6a7");
            conn = DriverManager.getConnection(url, props);            

            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }
   
    private Connection connectSAMI() {
        
        Connection conn = null;
        try {        
            String url = "jdbc:postgresqlaad://aaddemopg.postgres.database.azure.com/postgres";
            Properties props = new Properties();
            props.setProperty("user", "demopgsami@aaddemopg");
            props.setProperty("aadAuthentication", "ActiveDirectoryManagedIdentity");
            conn = DriverManager.getConnection(url, props);            

            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }

    private Connection connectInteractive() {
        
        Connection conn = null;
        try {        
            String url = "jdbc:postgresqlaad://aadauthpg.postgres.database.azure.com/postgres";
            Properties props = new Properties();
            props.setProperty("user", "PGAADAdmins@aadauthpg");
            props.setProperty("aadAuthentication", "ActiveDirectoryInteractive");
            props.setProperty("aadTenantId", "3312f4d4-a202-41eb-a696-5b3bc1c7ac36");
            conn = DriverManager.getConnection(url, props);            

            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }
    
    private Connection connectIntegrated() {
        System.setProperty("sun.security.jgss.native","true");
        Connection conn = null;
        try {        
            String url = "jdbc:postgresqlaad://aadauthpg.postgres.database.azure.com/postgres";
            Properties props = new Properties();
            props.setProperty("user", "PGAADAdmins@aadauthpg");
            props.setProperty("aadAuthentication", "ActiveDirectoryIntegrated");
            props.setProperty("aadTenantId", "3312f4d4-a202-41eb-a696-5b3bc1c7ac36");
            conn = DriverManager.getConnection(url, props);            

            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }
    
    /**
     * Get PostgreSQLVersion
     */
    public void getVersion() {
        String SQL = "SELECT version();";

        try (Connection conn = connectSAMI();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL)) {
            rs.next();
            System.out.println(rs.getString(1));
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       
        App app = new App();
        app.getVersion();
        app.getVersion();
        app.getVersion();
    }
}