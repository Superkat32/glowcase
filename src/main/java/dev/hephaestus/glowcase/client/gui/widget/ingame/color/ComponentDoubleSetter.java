package dev.hephaestus.glowcase.client.gui.widget.ingame.color;

@FunctionalInterface
public interface ComponentDoubleSetter {
	void apply(float mouseHorizontalLerp, float mouseVerticalLerp);
}
