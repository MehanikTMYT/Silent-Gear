package ru.mehtm.silentgear.qa;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.silentchaos512.gear.crafting.recipe.smithing.UpgradeSmithingRecipe;
import net.silentchaos512.gear.gear.part.PartInstance;
import net.silentchaos512.gear.util.GearData;

/** Tests actual recipe results after the server has loaded part definitions. */
@Mod("sgqa")
public final class UpgradeProbe {
    public UpgradeProbe() { NeoForge.EVENT_BUS.addListener(this::started); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("SGQA PASS " + message);
    }
    private void started(ServerStartedEvent event) {
        try {
            var sword = BuiltInRegistries.ITEM.getOptional(Identifier.parse("silentgear:sword")).orElseThrow();
            var upgrade = BuiltInRegistries.ITEM.getOptional(Identifier.parse("silentgear:magnetic_upgrade")).orElseThrow();
            ItemStack base = new ItemStack(sword);
            ItemStack addition = new ItemStack(upgrade);
            PartInstance part = PartInstance.from(addition);
            check(part != null && part.isValid(), "datapack upgrade part resolved");
            check(part.get().canAddToGear(base, part), "upgrade supports sword");
            var recipe = new UpgradeSmithingRecipe(Ingredient.of(sword), Optional.empty(), Optional.of(Ingredient.of(upgrade)));
            var input = new SmithingRecipeInput(ItemStack.EMPTY, base, addition);
            check(recipe.matches(input, event.getServer().overworld()), "valid recipe matches");
            ItemStack result = recipe.assemble(input);
            check(!result.isEmpty() && GearData.hasPart(result, part.get()), "assembled result contains installed upgrade");
            check(!GearData.hasPart(base, part.get()), "input weapon is unchanged");
            check(addition.getCount() == 1, "assembly does not mutate ingredient");
            check(result.getCount() == 1, "result count is one");
            var duplicate = new SmithingRecipeInput(ItemStack.EMPTY, result, addition);
            check(!recipe.matches(duplicate, event.getServer().overworld()), "duplicate upgrade cannot consume ingredients");
            check(recipe.assemble(duplicate).isEmpty(), "duplicate assembly rejected defensively");
            var invalid = new SmithingRecipeInput(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_SWORD), addition);
            check(!recipe.matches(invalid, event.getServer().overworld()), "non-gear input rejected");
            check(recipe.assemble(invalid).isEmpty(), "non-gear assembly rejected defensively");
            var wrongAddition = new SmithingRecipeInput(ItemStack.EMPTY, base, new ItemStack(Items.DIRT));
            check(!recipe.matches(wrongAddition, event.getServer().overworld()), "non-upgrade ingredient rejected");
            check(recipe.assemble(wrongAddition).isEmpty(), "non-upgrade assembly rejected defensively");
            System.out.println("SGQA COMPLETE");
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("SGQA FAIL");
        }
    }
}
