package dev.candycup.lifestealutils.features.alliances;

import java.util.ArrayList;
import java.util.List;

public final class AllianceModels {
    private AllianceModels() {
    }

    public static class AllianceRecord {
        public String clientId = "";
        public String serverId = "";
        public boolean published = false;
        public boolean canEdit = true;
        public String ownerUuid = "";
        public String subscriptionPermission = "MEMBERS";
        public long createdAt = 0L;
        public long updatedAt = 0L;
        public long lastSyncedAt = 0L;
        public String syncState = "LOCAL";
        public String source = "local";
        public String lastUsedListId = "";
        public int order = 0;
        public AllianceData data = new AllianceData();
    }

    public static class AllianceData {
        public String name = "";
        public String description = "";
        public int color = 0x55FF55;
        public List<AlliancePlayerList> lists = new ArrayList<>();
    }

    public static class AlliancePlayerList {
        public String id = "";
        public String name = "";
        public String prefix = "";
        public int prefixColor = 0x55FF55;
        public int nameColor = 0xFFFFFF;
        public List<AllianceMember> members = new ArrayList<>();
    }

    public static class AllianceMember {
        public String uuid = "";
        public long addedAt = 0L;
    }
}
