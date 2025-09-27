package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.block.BlockState;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class PopupBlockEntity extends GlowcaseBlockEntity {
	public static final NodeParser PARSER = TagParser.DEFAULT;
	public String title = "";
	public List<Text> lines = new ArrayList<>();
	public TextBlockEntity.TextAlignment textAlignment = TextBlockEntity.TextAlignment.CENTER;
	public int color = 0xFFFFFFFF;
	public boolean renderDirty = true;

	public PopupBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.POPUP_BLOCK_ENTITY.get(), pos, state);
		lines.add(Text.empty());
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		view.putString("title", this.title);
		view.putInt("color", this.color);

		view.put("text_alignment", TextBlockEntity.TextAlignment.CODEC, this.textAlignment);

		view.put("lines", TextCodecs.CODEC.listOf(), this.lines);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		this.title = view.getString("title", "");
		this.color = view.getInt("color", 0xFFFFFF);

		this.textAlignment = view.read("text_alignment", TextBlockEntity.TextAlignment.CODEC).orElse(TextBlockEntity.TextAlignment.CENTER);

		this.lines = new ArrayList<>(view.read("lines", TextCodecs.CODEC.listOf()).orElse(List.of(Text.empty())));

		this.renderDirty = true;
	}

	public String getRawLine(int i) {
		var line = this.lines.get(i);

		if (line.getStyle() == null) {
			return line.getString();
		}

		var insert = line.getStyle().getInsertion();

		if (insert == null) {
			return line.getString();
		}
		return insert;
	}

	public void addRawLine(int i, String string) {
		var parsed = PARSER.parseText(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.add(i, Text.literal(string));
		} else {
			this.lines.add(i, Text.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public void setRawLine(int i, String string) {
		var parsed = PARSER.parseText(string, ParserContext.of());

		if (parsed.getString().equals(string)) {
			this.lines.set(i, Text.literal(string));
		} else {
			this.lines.set(i, Text.empty().append(parsed).setStyle(Style.EMPTY.withInsertion(string)));
		}
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}
}
