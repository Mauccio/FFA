package com.mauccio.ffa.managers;

import com.mauccio.ffa.models.Stats;

import java.sql.*;
import java.util.UUID;

public class SQLiteManager {
    private Connection connection;

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");

            java.io.File folder = new java.io.File("plugins/FFA");
            if (!folder.exists()) folder.mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:plugins/FFA/ffa.db");
            createTable();
            System.out.println("Connected to SQLite.");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("SQLite connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS stats (" +
                "uuid TEXT PRIMARY KEY," +
                "kills INTEGER," +
                "deaths INTEGER" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveStats(Stats stats) {
        String sql = "INSERT OR REPLACE INTO stats (uuid, kills, deaths) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, stats.getUuid().toString());
            pstmt.setInt(2, stats.getKills());
            pstmt.setInt(3, stats.getDeaths());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Stats loadStats(UUID uuid) {
        String sql = "SELECT kills, deaths FROM stats WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Stats(uuid, rs.getInt("kills"), rs.getInt("deaths"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Stats(uuid, 0, 0);
    }

    public void addKill(UUID uuid, int amount) {
        String sql = "UPDATE stats SET kills = kills + ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, uuid.toString());
            int rows = pstmt.executeUpdate();

            if (rows == 0) {
                insertNew(uuid, amount, 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDeath(UUID uuid, int amount) {
        String sql = "UPDATE stats SET deaths = deaths + ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, uuid.toString());
            int rows = pstmt.executeUpdate();

            if (rows == 0) {
                insertNew(uuid, 0, amount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertNew(UUID uuid, int kills, int deaths) {
        String insert = "INSERT INTO stats (uuid, kills, deaths) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
            insertStmt.setString(1, uuid.toString());
            insertStmt.setInt(2, kills);
            insertStmt.setInt(3, deaths);
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}