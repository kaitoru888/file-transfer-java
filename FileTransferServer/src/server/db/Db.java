package server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import server.config.AppConfig;

public class Db {
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASS);
    }
}