package com.mauccio.ffa.models;

import java.util.UUID;

public class Stats {
    private UUID uuid;
    private int kills;
    private int deaths;

    public Stats(UUID uuid, int kills, int deaths) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    @Override
    public String toString() {
        return "Stats{" +
                "uuid=" + uuid +
                ", kills=" + kills +
                ", deaths=" + deaths +
                '}';
    }
}