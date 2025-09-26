package dev.hephaestus.glowcase.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces.TextFormattingScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Keyboard.class)
public class KeyboardMixin {

	@ModifyExpressionValue(
		method = "onKey",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/NarratorManager;isActive()Z")
	)
	private boolean preventNarratorToggleOnTextFormattingScreens(boolean original) {
		// prevents the narrator from being toggled when pressing "Ctrl+B" to hotkey bold formatting in the text block
		boolean preventNarratorOnTextScreen = false;
		if(MinecraftClient.getInstance().currentScreen instanceof TextFormattingScreen textFormattingScreen) {
			preventNarratorOnTextScreen = textFormattingScreen.useTheAntiNarratorinator();
		}
		return original && !preventNarratorOnTextScreen;
	}

}
