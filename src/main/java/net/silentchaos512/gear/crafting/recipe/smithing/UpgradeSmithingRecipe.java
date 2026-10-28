package net.silentchaos512.gear.crafting.recipe.smithing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.common.CommonHooks;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.gear.part.PartInstance;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.gear.util.GearHelper;

import java.util.Optional;

/** @noinspection OptionalUsedAsFieldOrParameterType*/
public class UpgradeSmithingRecipe extends GearSmithingRecipe {
    public static final RecipeSerializer<UpgradeSmithingRecipe> SERIALIZER = createSerializer(UpgradeSmithingRecipe::new);

    public UpgradeSmithingRecipe(Ingredient gearItem, Optional<Ingredient> template, Optional<Ingredient> addition) {
        super(gearItem, template, addition);
    }

    @Override
    protected boolean canApplyTo(SmithingRecipeInput input) {
        PartInstance part = PartInstance.from(input.addition());
        // Non-part addition: no extra restriction, vanilla ingredient matching decides.
        if (part == null) {
            return true;
        }
        return part.isValid() && part.get().canAddToGear(input.base(), part) && !GearData.hasPart(input.base(), part.get());
    }

    @Override
    protected boolean applyUpgrade(ItemStack stackToModify, ItemStack upgradeItem) {
        PartInstance part = PartInstance.from(upgradeItem);
        if (part == null) {
            return false;
        }
        GearType gearType = GearHelper.getType(stackToModify);
        if (gearType.isGear() && part.isValid() && part.get().canAddToGear(stackToModify, part) && !GearData.hasPart(stackToModify, part.get())) {
            GearData.addPart(stackToModify, part);
            GearData.recalculateGearData(stackToModify, CommonHooks.getCraftingPlayer());
            return true;
        }
        return false;
    }

    @Override
    public RecipeSerializer<UpgradeSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
