*Warning: beta-only - this driver has not been full tested beyond a simple connect and query*

# sample-postgres-jdbc-driver-with-aad-support

A JDBC Driver wrapped around the standard PostgreSQL JDBC Driver that provides Azure AD Authentication for connecting to Azure Database for Postgre SQL, as described in [Connect with Managed Identity to Azure Database for PostgreSQL](https://docs.microsoft.com/en-us/azure/postgresql/howto-connect-with-managed-identity).

## Properties

This JDBC driver supports all the Postgre SQL JDBC Driver properties and an additional, Azure AD driver properties.

|Property               |Description                                                                                        |
|-----------------------|---------------------------------------------------------------------------------------------------|
|aadAuthentication      |Azure AD acquisition flow - the only currently supported value is _ActiveDirectoryManagedIdentity_ |
|aadClientId            |Client ID of User-assigned Managed Identity.                                                       |

## Driver URL

Use `jdbc:postgresqlaad:` in place of `jdbc:postgresql:` in the JDBC URL.

For example: `jdbc:postgresqlaad://<server>.postgres.database.azure.com/<database>`

## System-assigned Managed Identity Example

```java
String url = "jdbc:postgresqlaad://<server>.postgres.database.azure.com/<database>";
Properties props = new Properties();
props.setProperty("user", "<user>@<server>");
props.setProperty("aadAuthentication", "ActiveDirectoryManagedIdentity");
conn = DriverManager.getConnection(url, props);            
Connection connection = DriverManager.getConnection(url, properties);
```

## User-assigned Managed Identity Example

```java
String url = "jdbc:postgresqlaad://<server>.postgres.database.azure.com/<database>";
Properties props = new Properties();
props.setProperty("user", "<user>@<server>");
props.setProperty("aadAuthentication", "ActiveDirectoryManagedIdentity");
props.setProperty("aadClientId", "<client-id>");
conn = DriverManager.getConnection(url, props);            
Connection connection = DriverManager.getConnection(url, properties);
```

## Active Directory Interactive Example

```java
String url = "jdbc:postgresqlaad://<server>.postgres.database.azure.com/<database>";
Properties props = new Properties();
props.setProperty("user", "<user>@<server>");
props.setProperty("aadAuthentication", "ActiveDirectoryInteractive");
props.setProperty("aadAuthority", "https://login.microsoftonline.com/<tenant-id>/");
conn = DriverManager.getConnection(url, props);            
Connection connection = DriverManager.getConnection(url, properties);
```