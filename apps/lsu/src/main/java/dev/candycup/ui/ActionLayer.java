package dev.candycup.ui;

import java.util.ArrayList;
import java.util.List;

public final class ActionLayer<T> {
    private final List<ActionRegion<T>> regions = new ArrayList<>();

    public void clear() {
        regions.clear();
    }

    public void add(Bounds bounds, int slotIndex, Object item, T kind) {
        regions.add(new ActionRegion<>(bounds, slotIndex, item, kind));
    }

    public ActionRegion<T> at(double mouseX, double mouseY) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            ActionRegion<T> region = regions.get(i);
            if (region.bounds().contains(mouseX, mouseY)) {
                return region;
            }
        }
        return null;
    }
}
