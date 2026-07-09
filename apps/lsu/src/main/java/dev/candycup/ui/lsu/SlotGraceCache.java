package dev.candycup.ui.lsu;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SlotGraceCache {
   private final Map<Integer, Entry> entries = new HashMap<>();
   private int containerId = Integer.MIN_VALUE;
   private String title = "";

   public void beginScope(int containerId, String title) {
      String normalizedTitle = title == null ? "" : title;
      if (this.containerId == containerId && Objects.equals(this.title, normalizedTitle)) {
         return;
      }
      this.containerId = containerId;
      this.title = normalizedTitle;
      entries.clear();
   }

   public void capture(int containerId, String title, SlotSnapshot snapshot) {
      beginScope(containerId, title);
      if (snapshot == null || snapshot.empty()) {
         return;
      }
      entries.put(snapshot.slotIndex(), new Entry(containerId, this.title, snapshot.visualCopy()));
   }

   public SlotSnapshot resolve(int containerId, String title, SlotSnapshot liveSnapshot) {
      beginScope(containerId, title);
      if (liveSnapshot == null) {
         return null;
      }
      Entry entry = entries.get(liveSnapshot.slotIndex());
      if (!liveSnapshot.empty()) {
         entries.remove(liveSnapshot.slotIndex());
         return liveSnapshot;
      }
      if (entry == null) {
         return liveSnapshot;
      }
      return entry.snapshot();
   }

   private record Entry(int containerId, String title, SlotSnapshot snapshot) {
   }
}
