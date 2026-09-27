package dev.candycup.ui;

public record ActionRegion<T>(Bounds bounds, int slotIndex, Object item, T kind) {
}
