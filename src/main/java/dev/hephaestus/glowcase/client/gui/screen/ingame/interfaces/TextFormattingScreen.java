package dev.hephaestus.glowcase.client.gui.screen.ingame.interfaces;

import dev.hephaestus.glowcase.client.util.ColorUtil;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A screen which is intended to be used for editing and formatting text.<br><br>
 *
 * Includes options for inserting <a href="https://placeholders.pb4.eu/user/quicktext/">TextPlaceholder's QuickText Tags</a> (e.g. color & formatting), inserting default formatting options, and handling formatting key presses.<br><br>
 *
 * Note that this interface is used to check for formatting hotkey conflicts in mixin(s) (e.g. {@link dev.hephaestus.glowcase.mixin.client.KeyboardMixin} to prevent narrator on Ctrl+B).
 *
 * @see TextFormattingScreen#insertTag(TextTag)
 * @see TextFormattingScreen#formattingKeyPressed(int, int, int)
 * @author Superkat32
 */
public interface TextFormattingScreen {

	SelectionManager getSelectionManager();

	/**
	 * Inserts a TextPlaceholder QuickText Tag - uses the shortest alias for its name.
	 * @param tag The TextPlaceholder QuickText Tag to insert.
	 */
	default void insertTag(TextTag tag) {
		this.insertTag(tag, true);
	}

	/**
	 * Inserts a TextPlaceholder QuickText Tag.
	 * @param tag The TextPlaceholder QuickText Tag to insert.
	 * @param findShortest If the shortest alias for the tag should be used instead of the exact name given.
	 */
	default void insertTag(TextTag tag, boolean findShortest) {
		if(tag == null) return;

		String name = findShortest ? findShortestTagName(tag) : tag.name();
		this.insertTagName(name);
	}

	/**
	 * Inserts TextPlaceholder QuickText Tag with a given name.
	 * @param tagName The name of the tag - used directly in between the angle brackets.
	 */
	default void insertTagName(String tagName) {
		String stringedTagStart = "<" + tagName + ">";
		String stringedTagEnd = "</" + tagName + ">";
		insertAroundSelection(stringedTagStart, stringedTagEnd);
	}

	/**
	 * Insert a color as a hex code tag.
	 */
	default void insertColorHexTag(int color) {
		this.insertTagName(ColorUtil.getHexCode(color));
	}

	/**
	 * Insert a formatting option with its name (e.g. {@link Formatting#BLUE} as "{@literal <blue></blue>}"
	 */
	default void insertFormattingTag(Formatting formatting) {
		this.insertTag(TagRegistry.SAFE.getTag(formatting.getName()), false);
	}

	/**
	 * Insert two strings around the currently selected(if any) text. Will automatically move the cursor back and keep the text selected.
	 */
	default void insertAroundSelection(String beforeSelection, String afterSelection) {
		SelectionManager selectionManager = this.getSelectionManager();
		int selStart = selectionManager.getSelectionStart();
		int selEnd = selectionManager.getSelectionEnd();
		if(selStart != selEnd) { // any amount of text is selected(highlighted blue)
			int selAmount = Math.abs(selEnd - selStart);
			selectionManager.moveCursor(selStart < selEnd ? 0 : -selAmount, false, SelectionManager.SelectionType.CHARACTER);
			selectionManager.insert(beforeSelection);
			selectionManager.moveCursor(selAmount, false, SelectionManager.SelectionType.CHARACTER);
			selectionManager.insert(afterSelection);
			selectionManager.moveCursor(-afterSelection.length(), false, SelectionManager.SelectionType.CHARACTER);
			selectionManager.setSelection(selStart + beforeSelection.length(), selEnd + beforeSelection.length());
		} else { // no text is selected
			selectionManager.insert(beforeSelection + afterSelection);
			selectionManager.moveCursor(-afterSelection.length(), false, SelectionManager.SelectionType.CHARACTER);
		}
	}

	default boolean formattingKeyPressed(int keyCode, int scanCode, int modifiers) {
		if(!Screen.hasControlDown()) return false;

		if(keyCode == GLFW.GLFW_KEY_B) {
			this.insertBold();
			return true;
		} else if(keyCode == GLFW.GLFW_KEY_I) {
			this.insertItalic();
			return true;
		} else if(keyCode == GLFW.GLFW_KEY_U) {
			this.insertUnderline();
			return true;
		} else if(keyCode == GLFW.GLFW_KEY_5 || keyCode == GLFW.GLFW_KEY_S) {
			// There isn't really a commonly agreed upon hotkey for strikethrough, unlike everything else
			// "5" and "S" were (apparently, seemingly) common among different platforms though,
			// so that's that ¯\_(ツ)_/¯
			this.insertStrikethrough();
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_O) {
			this.insertObfuscated();
			return true;
		}

		return false;
//		switch (keyCode) { // super cursed to the point where I don't think I like it
//			case GLFW.GLFW_KEY_B -> insertBold();
//			case GLFW.GLFW_KEY_I -> insertItalic();
//			case GLFW.GLFW_KEY_U -> insertUnderline();
//			case GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_S -> insertStrikethrough();
//			case GLFW.GLFW_KEY_O -> insertObfuscated();
//			default -> {
//				return false;
//			}
//		}
//		return true;
	}

	/**
	 * @param tag The text tag who's shortest aliases should be found.
	 * @return The shortest alias String name of the given TextPlaceholder QuickText tag <br><br>
	 * e.g. "{@literal <underline></underline>}" changes into "{@literal <u></u>}"
	 */
	default String findShortestTagName(TextTag tag) {
		if(tag.aliases().length <= 1) return tag.name();
		return Arrays.stream(tag.aliases()).min(Comparator.comparing(String::length)).get();
	}

	// Default formatting insert methods (available here for easy modifying later if needed, or easy lambda setups)
	//region Default Formatting Insert Methods
	default void insertBold() {
		this.insertTag(TagRegistry.SAFE.getTag("bold"));
	}

	default void insertItalic() {
		this.insertTag(TagRegistry.SAFE.getTag("italic"));
	}

	default void insertStrikethrough() {
		this.insertTag(TagRegistry.SAFE.getTag("strikethrough"));
	}

	default void insertUnderline() {
		this.insertTag(TagRegistry.SAFE.getTag("underline"));
	}

	default void insertObfuscated() {
		this.insertTag(TagRegistry.SAFE.getTag("obfuscated"));
	}
	//endregion

	/**
	 * @return If this screen should prevent the player from accidentally having the narrator narrate their narrate-able gameplay.<br><br>
	 * I don't know if this will ever be needed to be false, but we have it as an option now I guess.
	 * @see dev.hephaestus.glowcase.mixin.client.KeyboardMixin
	 */
	default boolean useTheAntiNarratorinator() {
		return true;
	}

}
