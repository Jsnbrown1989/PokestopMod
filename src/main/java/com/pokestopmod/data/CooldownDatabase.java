package com.pokestopmod.data;

import java.sql.*;
import java.util.*;

public class CooldownDatabase {
    private static final String DB_URL = "jdbc:sqlite:config/pokestop/cooldowns.db";

    public CooldownDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cooldowns (
                    player_uuid TEXT NOT NULL,
                    pokestop_name TEXT NOT NULL,
                    timestamp LONG NOT NULL,
                    PRIMARY KEY (player_uuid, pokestop_name)
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Long getLastClaim(UUID player, String pokestop) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement stmt = conn.prepareStatement("""
                SELECT timestamp FROM cooldowns WHERE player_uuid = ? AND pokestop_name = ?
            """);
            stmt.setString(1, player.toString());
            stmt.setString(2, pokestop);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("timestamp");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setClaim(UUID player, String pokestop, long timestamp) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement stmt = conn.prepareStatement("""
                INSERT OR REPLACE INTO cooldowns (player_uuid, pokestop_name, timestamp)
                VALUES (?, ?, ?)
            """);
            stmt.setString(1, player.toString());
            stmt.setString(2, pokestop);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
