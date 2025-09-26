package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SpriteBlockEntity extends GlowcaseBlockEntity {
	protected String sprite = "arrow";
	protected @Nullable ItemStack renderItem = null;
	public int rotation = 0;
	public TextBlockEntity.ZOffset zOffset = TextBlockEntity.ZOffset.BACK;
	public int color = 0xFFFFFF;
	public float scale = 1;

	public SpriteBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.SPRITE_BLOCK_ENTITY.get(), pos, state);
	}

	public void setSprite(String newSprite) {
		sprite = newSprite;
		if (newSprite.contains(":")) {
			Optional<Item> item = Registries.ITEM.getOptionalValue(Identifier.tryParse(newSprite));
			renderItem = item.map(ItemStack::new).orElse(null);
		} else {
			renderItem = null;
		}
	}

	public String getSprite() {
		return sprite;
	}

	@Nullable
	public ItemStack getRenderItem() {
		return renderItem;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		view.putString("sprite", this.sprite);
		view.putInt("rotation", this.rotation);
		view.put("z_offset", TextBlockEntity.ZOffset.CODEC, this.zOffset);
		view.putInt("color", this.color);
		view.putFloat("scale", this.scale);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		setSprite(view.getString("sprite", "arrow"));
		this.rotation = view.getInt("rotation", 0);
		this.zOffset = view.read("z_offset", TextBlockEntity.ZOffset.CODEC).orElse(TextBlockEntity.ZOffset.BACK);
		this.color = view.getInt("color", 0xFFFFFF);
		this.scale = view.getFloat("scale", 1);
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
		markDirty();
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
	}
}
