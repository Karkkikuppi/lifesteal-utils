package dev.candycup.lifestealutils.ui.editor;

import net.minecraft.resources.Identifier;

/**
 * tracks shared state for the HUD editor.
 */
public final class HudEditorState {
   private boolean snapEnabled;
   private boolean snappingActive;
   private boolean snappingStateInitialized;
   private boolean lastSnappingActive;
   private Identifier draggingId;
   private float dragOffsetX;
   private float dragOffsetY;

   /**
    * resets state for a fresh editor session.
    */
   public void reset() {
      draggingId = null;
      dragOffsetX = 0F;
      dragOffsetY = 0F;
      snappingStateInitialized = false;
      lastSnappingActive = false;
      snappingActive = false;
      snapEnabled = false;
   }

   /**
    * toggles the snap setting.
    */
   public void toggleSnapEnabled() {
      snapEnabled = !snapEnabled;
   }

   /**
    * updates snapping state based on the shift modifier.
    *
    * @param shiftDown whether shift is held
    * @return true if the snapping active state changed
    */
   public boolean updateSnapping(boolean shiftDown) {
      snappingActive = snapEnabled ^ shiftDown;
      if (!snappingStateInitialized) {
         lastSnappingActive = snappingActive;
         snappingStateInitialized = true;
         return false;
      }
      if (snappingActive != lastSnappingActive) {
         lastSnappingActive = snappingActive;
         return true;
      }
      return false;
   }

   /**
    * returns whether snapping is currently active.
    *
    * @return true if snapping should be applied
    */
   public boolean isSnappingActive() {
      return snappingActive;
   }

   /**
    * begins dragging an element.
    *
    * @param id      the element identifier
    * @param offsetX the drag offset x
    * @param offsetY the drag offset y
    */
   public void beginDrag(Identifier id, float offsetX, float offsetY) {
      this.draggingId = id;
      this.dragOffsetX = offsetX;
      this.dragOffsetY = offsetY;
   }

   /**
    * clears the drag state.
    */
   public void clearDrag() {
      draggingId = null;
   }

   /**
    * checks if the given id is the active drag target.
    *
    * @param id the element id
    * @return true if dragging
    */
   public boolean isDragging(Identifier id) {
      return draggingId != null && draggingId.equals(id);
   }

   /**
    * checks if any element is being dragged.
    *
    * @return true if dragging
    */
   public boolean isDraggingAny() {
      return draggingId != null;
   }

   /**
    * returns the drag offset x.
    *
    * @return drag offset x
    */
   public float dragOffsetX() {
      return dragOffsetX;
   }

   /**
    * returns the drag offset y.
    *
    * @return drag offset y
    */
   public float dragOffsetY() {
      return dragOffsetY;
   }
}
