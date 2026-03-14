package server.config;

import java.nio.file.Path;

public final class AppConfig {
    private AppConfig(){}

    // ====== SQL Server connection ======
    // SỬA THEO MÁY BẠN
    public static final String DB_URL =
            "jdbc:sqlserver://localhost:1433;databaseName=file_transfer_app;encrypt=true;trustServerCertificate=true;";
    public static final String DB_USER = "sa";
    public static final String DB_PASS = "123456";

    // ====== Socket server ======
    public static final int DEFAULT_PORT = 9090;

    // ====== folder lưu file ở server ======
    public static final Path STORAGE_DIR = Path.of("server_files");
}