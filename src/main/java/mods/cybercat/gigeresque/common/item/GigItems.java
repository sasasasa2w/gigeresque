package mods.cybercat.gigeresque.common.item;

import mods.cybercat.gigeresque.common.Gigeresque;
import mods.cybercat.gigeresque.common.entity.Entities;
import mods.cybercat.gigeresque.common.fluid.GigFluids;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

public class GigItems {

	public static final BucketItem BLACK_FLUID_BUCKET = registerItem("black_fluid_bucket",
			new BucketItem(GigFluids.BLACK_FLUID_STILL,
					new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
	
	public static final SurgeryKitItem SURGERY_KIT = registerItem("surgery_kit",
			new SurgeryKitItem(new Item.Properties().durability(4)));

	public static final SpawnEggItem ALIEN_SPAWN_EGG = registerItem("alien_spawn_egg",
			new SpawnEggItem(Entities.ALIEN, 0x404345, 0x949597, new Item.Properties()));
	public static final SpawnEggItem AQUATIC_ALIEN_SPAWN_EGG = registerItem("aquatic_alien_spawn_egg",
			new SpawnEggItem(Entities.AQUATIC_ALIEN, 0x404345, 0x949597, new Item.Properties()));
	public static final SpawnEggItem AQUATIC_CHESTBURSTER_SPAWN_EGG = registerItem("aquatic_chestburster_spawn_egg",
			new SpawnEggItem(Entities.AQUATIC_CHESTBURSTER, 0xDED29D, 0x2C2B27, new Item.Properties()));
	public static final SpawnEggItem CHESTBURSTER_SPAWN_EGG = registerItem("chestburster_spawn_egg",
			new SpawnEggItem(Entities.CHESTBURSTER, 0xDED29D, 0x2C2B27, new Item.Properties()));
	public static final SpawnEggItem EGG_SPAWN_EGG = registerItem("egg_spawn_egg",
			new SpawnEggItem(Entities.EGG, 0x554E45, 0x4D4932, new Item.Properties()));
	public static final SpawnEggItem FACEHUGGER_SPAWN_EGG = registerItem("facehugger_spawn_egg",
			new SpawnEggItem(Entities.FACEHUGGER, 0xC7B986, 0x516B21, new Item.Properties()));
	public static final SpawnEggItem RUNNER_ALIEN_SPAWN_EGG = registerItem("runner_alien_spawn_egg",
			new SpawnEggItem(Entities.RUNNER_ALIEN, 0x3E230B, 0x623C25, new Item.Properties()));
	public static final SpawnEggItem RUNNERBURSTER_SPAWN_EGG = registerItem("runnerburster_spawn_egg",
			new SpawnEggItem(Entities.RUNNERBURSTER, 0xDED29D, 0x2C2B27, new Item.Properties()));
	public static final SpawnEggItem MUTANT_POPPER_SPAWN_EGG = registerItem("popper_spawn_egg",
			new SpawnEggItem(Entities.MUTANT_POPPER, 0xdeeae9, 0x816d66, new Item.Properties()));
	public static final SpawnEggItem MUTANT_HAMMERPEDE_SPAWN_EGG = registerItem("hammerpede_spawn_egg",
			new SpawnEggItem(Entities.MUTANT_HAMMERPEDE, 0xe3e1d5, 0x826e66, new Item.Properties()));
	public static final SpawnEggItem MUTANT_STALKER_SPAWN_EGG = registerItem("stalker_spawn_egg",
			new SpawnEggItem(Entities.MUTANT_STALKER, 0xcdd7d8, 0x816d66, new Item.Properties()));

//	public static final EngiArmorItem ENGI_ARMOR_HELMET = new EngiArmorItem(ArmorMaterials.DIAMOND, EquipmentSlot.HEAD);
//	public static final EngiArmorItem ENGI_ARMOR_CHESTPLATE = new EngiArmorItem(ArmorMaterials.DIAMOND,
//			EquipmentSlot.CHEST);
//	public static final EngiArmorItem ENGI_ARMOR_LEGGINGS = new EngiArmorItem(ArmorMaterials.DIAMOND,
//			EquipmentSlot.LEGS);
//	public static final EngiArmorItem ENGI_ARMOR_BOOTS = new EngiArmorItem(ArmorMaterials.DIAMOND, EquipmentSlot.FEET);

	public static <I extends Item> I registerItem(String name, I item) {
		return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Gigeresque.MOD_ID, name), item);
	}
}
