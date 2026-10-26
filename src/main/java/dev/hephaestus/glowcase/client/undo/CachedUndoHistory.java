package dev.hephaestus.glowcase.client.undo;

import dev.hephaestus.glowcase.client.undo.action.AbstractUndoAction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CachedUndoHistory(
	List<AbstractUndoAction> past,
	@Nullable AbstractUndoAction present,
	List<AbstractUndoAction> future
) {

}
