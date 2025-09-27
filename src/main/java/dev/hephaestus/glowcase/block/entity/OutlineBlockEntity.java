package dev.hephaestus.glowcase.block.entity;

import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;

public class OutlineBlockEntity extends GlowcaseBlockEntity {
	public Vec3i offset = Vec3i.ZERO;
	public Vec3i scale = new Vec3i(1, 1, 1);
	public int color = 0xFFFFFF;

	public OutlineBlockEntity(BlockPos pos, BlockState state) {
		super(Glowcase.OUTLINE_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);

		view.put("offset", Vec3i.CODEC, this.offset);
		view.put("scale", Vec3i.CODEC, this.scale);
		view.putInt("color", this.color);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);

		this.offset = view.read("offset", Vec3i.CODEC).orElse(Vec3i.ZERO);
		this.scale = view.read("scale", Vec3i.CODEC).orElseGet(() -> new Vec3i(1, 1, 1));
		this.color = view.getInt("color", 0xFFFFFF);
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getColor() {
		return this.color;
	}
}
