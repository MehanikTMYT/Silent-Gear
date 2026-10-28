package net.silentchaos512.gear.crafting.recipe.smithing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.common.CommonHooks;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.gear.material.MaterialInstance;
import net.silentchaos512.gear.gear.part.PartInstance;
import net.silentchaos512.gear.setup.gear.PartTypes;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.gear.util.GearHelper;

import java.util.Objects;
import java.util.Optional;

/** @noinspection OptionalUsedAsFieldOrParameterType*/
public class CoatingSmithingRecipe extends GearSmithingRecipe {
    public static final RecipeSerializer<CoatingSmithingRecipe> SERIALIZER = createSerializer(CoatingSmithingRecipe::new);

    public CoatingSmithingRecipe(Ingredient gearItem, Optional<Ingredient> template, Optional<Ingredient> addition) {
        super(gearItem, template, addition);
    }

    @Override
    protected boolean canApplyTo(SmithingRecipeInput input) {
        // Re-coating (replacing an existing coating) is legitimate; only a missing coat target
        // is a no-op that must not consume ingredients.
        return GearData.getPartOfType(input.base(), PartTypes.MAIN.get()) != null;
    }

    @Override
    protected boolean applyUpgrade(ItemStack stackToModify, ItemStack upgradeItem) {
        if (GearData.getPartOfType(stackToModify, PartTypes.MAIN.get()) == null) {
            return false;
        }

        MaterialInstance material = MaterialInstance.from(upgradeItem);
        if (material == null) {
            return false;
        }
        GearType gearType = GearHelper.getType(stackToModify);
        if (gearType.isGear()) {
            PartTypes.COATING.get().getCompoundPartItem(gearType).ifPresent(cpi -> {
                ItemStack partItem = cpi.create(material, 1);
                // Unfortunately this deletes the old part; can't get a player here
                GearData.addOrReplacePart(stackToModify, Objects.requireNonNull(PartInstance.from(partItem)));
            });

            stackToModify.setDamageValue(0);
            GearData.recalculateGearData(stackToModify, CommonHooks.getCraftingPlayer()); // Crafting player is always null?
            return true;
        }
        return false;
    }

    @Override
    public RecipeSerializer<CoatingSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
