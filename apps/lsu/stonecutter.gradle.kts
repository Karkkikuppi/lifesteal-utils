plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    // id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "1.21.11"

/*
// Make newer versions be published last
stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}
 */

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        string(current.parsed <= "1.21.8") {
            // used in context of GLFW.glfwGetMouseButton
            replace("getWindow().handle()", "getWindow().getWindow()")
        }
        string(current.version >= "26.1") {
            replace("net.minecraft.client.gui.GuiGraphics", "net.minecraft.client.gui.GuiGraphicsExtractor")
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("method = \"render\"", "method = \"extractRenderState\"")
            replace("public void render(GuiGraphics", "public void extractRenderState(GuiGraphicsExtractor")
            replace("renderBackground", "extractBackground")
            replace("renderMenuBackground", "extractMenuBackground")
            replace("renderListBackground", "extractListBackground")
            replace("renderListSeparators", "extractListSeparators")
            replace("renderContent", "extractContent")
            replace("public void renderBlurredBackground(", "protected void extractBlurredBackground(")
            replace("protected void renderTooltip(", "protected void extractTooltip(")
            replace("renderTooltip(GuiGraphics", "extractTooltip(GuiGraphicsExtractor")
            replace("this.renderTooltip(", "this.extractTooltip(")
            replace(".render(guiGraphics,", ".extractRenderState(guiGraphics,")
            replace(".render(graphics,", ".extractRenderState(graphics,")
            replace("super.render(", "super.extractRenderState(")
            replace("drawCenteredString", "centeredText")
            replace("guiGraphics.drawString(", "guiGraphics.text(")
            replace("graphics.drawString(", "graphics.text(")
            replace("drawContext.drawString(", "drawContext.text(")
            replace("context.graphics().drawString(", "context.graphics().text(")
            replace("net.fabricmc.fabric.api.client.command.v2.ClientCommandManager", "net.fabricmc.fabric.api.client.command.v2.ClientCommands")
            replace("ClientCommandManager", "ClientCommands")
            replace("net.fabricmc.fabric.api.client.keybinding.v1", "net.fabricmc.fabric.api.client.keymapping.v1")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace("registerKeyBinding", "registerKeyMapping")
            replace("PlayerFaceRenderer.draw", "PlayerFaceExtractor.extractRenderState")
            replace("PlayerFaceRenderer", "PlayerFaceExtractor")
            replace("tabNavigationBar.setWidth", "tabNavigationBar.updateWidth")
            replace("net.minecraft.client.GuiMessageTag", "net.minecraft.client.multiplayer.chat.GuiMessageTag")
            replace("net.minecraft.world.inventory.ClickType", "net.minecraft.world.inventory.ContainerInput")
            replace("ClickType.", "ContainerInput.")
            replace("guiGraphics.renderItem", "guiGraphics.item")
            replace("graphics.renderItem", "graphics.item")
            replace("gameMode.handleInventoryMouseClick", "gameMode.handleContainerInput")
        }
    }
}
