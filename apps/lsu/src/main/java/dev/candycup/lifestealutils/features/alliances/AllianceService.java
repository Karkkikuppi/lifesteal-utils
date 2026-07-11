package dev.candycup.lifestealutils.features.alliances;


import dev.candycup.lifestealutils.gaia.GaiaConsentController;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AllianceService {
    private static final List<AllianceModels.AllianceRecord> localAlliances = new ArrayList<>();
    private static final List<AllianceModels.AllianceRecord> remoteAlliances = new ArrayList<>();

    private AllianceService() {
    }

    public static void initialize() {
        reloadFromDisk();
    }

    public static synchronized void reloadFromDisk() {
        localAlliances.clear();
        localAlliances.addAll(AllianceStorageService.loadAll());
        normalizeLocalOrder();
    }

    public static synchronized List<AllianceModels.AllianceRecord> listAll() {
        normalizeLocalOrder();
        ArrayList<AllianceModels.AllianceRecord> out = new ArrayList<>(localAlliances.size() + remoteAlliances.size());
        out.addAll(localAlliances);
        out.addAll(remoteAlliances);
        out.sort(Comparator.comparingInt(alliance -> alliance.order));
        return out;
    }

    public static synchronized List<AllianceModels.AllianceRecord> listEditable() {
        ArrayList<AllianceModels.AllianceRecord> out = new ArrayList<>();
        for (AllianceModels.AllianceRecord alliance : listAll()) {
            if (alliance.canEdit) {
                out.add(alliance);
            }
        }
        out.sort(Comparator.comparingInt(alliance -> alliance.order));
        return out;
    }

    public static synchronized AllianceModels.AllianceRecord findByClientId(String clientId) {
        for (AllianceModels.AllianceRecord alliance : listAll()) {
            if (Objects.equals(alliance.clientId, clientId)) {
                return alliance;
            }
        }
        return null;
    }

    public static synchronized AllianceModels.AllianceRecord findByServerId(String serverId) {
        for (AllianceModels.AllianceRecord alliance : listAll()) {
            if (Objects.equals(alliance.serverId, serverId)) {
                return alliance;
            }
        }
        return null;
    }

    public static synchronized AllianceModels.AllianceRecord findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String needle = name.trim().toLowerCase(Locale.ROOT);
        AllianceModels.AllianceRecord exact = null;
        for (AllianceModels.AllianceRecord alliance : listAll()) {
            String current = alliance.data == null ? "" : alliance.data.name;
            String lowered = current == null ? "" : current.toLowerCase(Locale.ROOT);
            if (lowered.equals(needle)) {
                exact = alliance;
                break;
            }
        }
        if (exact != null) {
            return exact;
        }
        for (AllianceModels.AllianceRecord alliance : listAll()) {
            String current = alliance.data == null ? "" : alliance.data.name;
            String lowered = current == null ? "" : current.toLowerCase(Locale.ROOT);
            if (lowered.contains(needle)) {
                return alliance;
            }
        }
        return null;
    }

    public static synchronized void save(AllianceModels.AllianceRecord record) {
        if (record == null) {
            return;
        }
        if (isRemoteOnly(record)) {
            AllianceModels.AllianceRecord localExisting = findInLocalByClientId(record.clientId);
            if (localExisting != null) {
                AllianceStorageService.delete(record.clientId);
                localAlliances.removeIf(alliance -> Objects.equals(alliance.clientId, record.clientId));
                normalizeLocalOrder();
            }
            upsertRemote(record);
            return;
        }

        AllianceStorageService.save(record);
        boolean replaced = false;
        for (int i = 0; i < localAlliances.size(); i++) {
            if (Objects.equals(localAlliances.get(i).clientId, record.clientId)) {
                localAlliances.set(i, record);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            localAlliances.add(record);
        }
        normalizeLocalOrder();
    }

    public static synchronized void delete(String clientId) {
        AllianceModels.AllianceRecord local = findInLocalByClientId(clientId);
        if (local != null) {
            AllianceStorageService.delete(clientId);
            localAlliances.removeIf(alliance -> Objects.equals(alliance.clientId, clientId));
            normalizeLocalOrder();
            return;
        }
        remoteAlliances.removeIf(alliance -> Objects.equals(alliance.clientId, clientId));
    }

    public static synchronized void deleteByServerId(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return;
        }
        AllianceModels.AllianceRecord alliance = findByServerId(serverId);
        if (alliance == null) {
            return;
        }
        delete(alliance.clientId);
    }

    public static synchronized void reorder(List<String> orderedClientIds) {
        int index = 0;
        for (String id : orderedClientIds) {
            AllianceModels.AllianceRecord alliance = findInLocalByClientId(id);
            if (alliance == null) {
                continue;
            }
            alliance.order = index++;
            AllianceStorageService.save(alliance);
        }
        for (AllianceModels.AllianceRecord alliance : localAlliances) {
            if (!orderedClientIds.contains(alliance.clientId)) {
                alliance.order = index++;
                AllianceStorageService.save(alliance);
            }
        }
        normalizeLocalOrder();
    }

    public static synchronized boolean addMember(AllianceModels.AllianceRecord alliance, String listId, String memberUuid) {
        if (alliance == null || alliance.data == null || alliance.data.lists == null) {
            return false;
        }
        String normalized = memberUuid == null ? "" : memberUuid.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        AllianceModels.AlliancePlayerList list = resolveList(alliance, listId);
        if (list == null) {
            return false;
        }
        for (AllianceModels.AllianceMember member : list.members) {
            if (member.uuid != null && member.uuid.equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        AllianceModels.AllianceMember member = new AllianceModels.AllianceMember();
        member.uuid = normalized;
        member.addedAt = System.currentTimeMillis();
        list.members.add(member);
        alliance.lastUsedListId = list.id;
        save(alliance);
        return true;
    }

    public static synchronized boolean removeMember(AllianceModels.AllianceRecord alliance, String memberUuid) {
        if (alliance == null || alliance.data == null || alliance.data.lists == null) {
            return false;
        }
        String normalized = memberUuid == null ? "" : memberUuid.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        boolean changed = false;
        for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            changed |= list.members.removeIf(member -> member.uuid != null && member.uuid.equalsIgnoreCase(normalized));
        }
        if (changed) {
            save(alliance);
        }
        return changed;
    }

    public static synchronized int totalMembers(AllianceModels.AllianceRecord alliance) {
        if (alliance == null || alliance.data == null || alliance.data.lists == null) {
            return 0;
        }
        int count = 0;
        for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
            count += list.members.size();
        }
        return count;
    }

    public static synchronized AllianceModels.AllianceRecord createLocal(String name) {
        AllianceModels.AllianceRecord record = new AllianceModels.AllianceRecord();
        record.clientId = AllianceIdGenerator.newClientId();
        record.order = localAlliances.size();
        record.canEdit = true;
        record.subscriptionPermission = GaiaConsentController.isGaiaAdvancedFeaturesEnabled() ? "ANYONE" : "MEMBERS";
        record.published = false;
        record.source = "local";
        record.syncState = "LOCAL";
        if (Minecraft.getInstance().player != null) {
            record.ownerUuid = Minecraft.getInstance().player.getUUID().toString();
        }
        record.data = new AllianceModels.AllianceData();
        record.data.name = name == null || name.isBlank() ? "New Alliance" : name.trim();
        record.data.description = "";
        record.data.color = 0x55FF55;
        AllianceModels.AlliancePlayerList defaultList = new AllianceModels.AlliancePlayerList();
        defaultList.id = AllianceIdGenerator.newListId();
        defaultList.name = "Members";
        defaultList.prefix = "";
        defaultList.prefixColor = record.data.color;
        defaultList.nameColor = 0xFFFFFF;
        record.data.lists.add(defaultList);
        record.lastUsedListId = defaultList.id;
        save(record);
        return record;
    }

    public static synchronized AllianceModels.AlliancePlayerList resolveList(AllianceModels.AllianceRecord alliance, String listIdOrName) {
        if (alliance == null || alliance.data == null || alliance.data.lists == null || alliance.data.lists.isEmpty()) {
            return null;
        }
        if (listIdOrName != null && !listIdOrName.isBlank()) {
            String needle = listIdOrName.trim().toLowerCase(Locale.ROOT);
            for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
                if (list.id != null && list.id.equalsIgnoreCase(needle)) {
                    return list;
                }
                if (list.name != null && list.name.toLowerCase(Locale.ROOT).equals(needle)) {
                    return list;
                }
            }
        }
        if (alliance.lastUsedListId != null && !alliance.lastUsedListId.isBlank()) {
            for (AllianceModels.AlliancePlayerList list : alliance.data.lists) {
                if (alliance.lastUsedListId.equals(list.id)) {
                    return list;
                }
            }
        }
        return null;
    }

    public static synchronized void upsertRemote(AllianceModels.AllianceRecord record) {
        if (record == null) {
            return;
        }
        boolean replaced = false;
        for (int i = 0; i < remoteAlliances.size(); i++) {
            AllianceModels.AllianceRecord existing = remoteAlliances.get(i);
            if (Objects.equals(existing.clientId, record.clientId)
                    || (!record.serverId.isBlank() && Objects.equals(existing.serverId, record.serverId))) {
                remoteAlliances.set(i, record);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            remoteAlliances.add(record);
        }
    }

    private static AllianceModels.AllianceRecord findInLocalByClientId(String clientId) {
        for (AllianceModels.AllianceRecord alliance : localAlliances) {
            if (Objects.equals(alliance.clientId, clientId)) {
                return alliance;
            }
        }
        return null;
    }

    private static boolean isRemoteOnly(AllianceModels.AllianceRecord record) {
        return "remote".equalsIgnoreCase(record.source) || record.published;
    }

    private static void normalizeLocalOrder() {
        localAlliances.sort(Comparator.comparingInt(alliance -> alliance.order));
        for (int i = 0; i < localAlliances.size(); i++) {
            AllianceModels.AllianceRecord alliance = localAlliances.get(i);
            if (alliance.order != i) {
                alliance.order = i;
                AllianceStorageService.save(alliance);
            }
        }
    }
}


