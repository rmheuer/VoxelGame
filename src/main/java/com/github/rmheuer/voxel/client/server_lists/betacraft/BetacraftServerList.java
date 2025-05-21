package com.github.rmheuer.voxel.client.server_lists.betacraft;

import com.github.rmheuer.azalea.io.IOUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class BetacraftServerList {
    private static final URL LIST_URL;
    static {
        try {
            LIST_URL = new URL("https://api.betacraft.uk/v2/server_list");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ServerEntry> get() throws IOException {
        String json = IOUtil.readToString(LIST_URL.openStream());

        List<ServerEntry> entries = new ArrayList<>();
        try {
            JsonArray root = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement elem : root) {
                JsonObject entry = elem.getAsJsonObject();

                // Only Classic protocol version 7 is supported currently
                String protocol = entry.get("protocol").getAsString();
                if (!protocol.equals("classic_7"))
                    continue;

                String name = entry.get("name").getAsString();
                int playerCount = entry.get("online_players").getAsInt();
                int maxPlayers = entry.get("max_players").getAsInt();
                String address = entry.get("socket").getAsString();

                entries.add(new ServerEntry(name, playerCount, maxPlayers, address));
            }
        } catch (NullPointerException | ClassCastException e) {
            throw new IOException("Malformed Betacraft server list response", e);
        }

        return entries;
    }
}
