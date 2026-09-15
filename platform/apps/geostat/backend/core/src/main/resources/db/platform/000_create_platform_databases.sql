/* Run once as a SQL Server principal with CREATE ANY DATABASE permission. */
IF DB_ID(N'geostat-data') IS NULL
    CREATE DATABASE [geostat-data];

IF DB_ID(N'geostat-archive') IS NULL
    CREATE DATABASE [geostat-archive];
