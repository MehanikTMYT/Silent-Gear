package net.silentchaos512.gear.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.GearTool;
import net.silentchaos512.gear.api.material.IMaterialCategory;
import net.silentchaos512.gear.api.material.Material;
import net.silentchaos512.gear.block.alloymaker.AlloyMakerInfo;
import net.silentchaos512.gear.client.ClientRecipeHelper;
import net.silentchaos512.gear.crafting.ingredient.PartMaterialIngredient;
import net.silentchaos512.gear.crafting.recipe.ToolActionRecipe;
import net.silentchaos512.gear.crafting.recipe.alloy.*;
import net.silentchaos512.gear.crafting.recipe.salvage.SalvagingRecipe;
import net.silentchaos512.gear.gear.material.MaterialInstance;
import net.silentchaos512.gear.item.CraftingItems;
import net.silentchaos512.gear.item.RepairKitItem;
import net.silentchaos512.gear.item.SingleMaterialItem;
import net.silentchaos512.gear.setup.SgBlocks;
import net.silentchaos512.gear.setup.SgItems;
import net.silentchaos512.gear.setup.SgRecipes;
import net.silentchaos512.gear.setup.SgRegistries;
import net.silentchaos512.gear.setup.gear.PartTypes;
import net.silentchaos512.gear.util.Const;
import net.silentchaos512.gear.util.IngredientUtils;
import net.silentchaos512.gear.util.TextUtil;
import net.silentchaos512.lib.crafting.recipe.CraftingRecipeExtension;
import net.silentchaos512.lib.util.NameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * EMI plugin (fleet 26.1.2: replaces the JEI plugin, see src/disabled/.../compat/jei). Same content as the JEI plugin:
 * gear crafting hints, alloy makers (+ examples), grading, salvaging, stone anvil, repair kit hints, info pages,
 * custom material subtypes and material aliases. JEI's recipe click areas have no EMI equivalent; the machines are
 * registered as workstations instead.
 */
@EmiEntrypoint
public class SGearEmiPlugin implements EmiPlugin {
    static final EmiRecipeCategory GEAR_CRAFTING = category("gear_crafting", EmiStack.of(SgItems.BLUEPRINT_PACKAGE), "group.gearCrafting");
    static final EmiRecipeCategory ALLOY_FABRIC = category("alloy_making/fabric", EmiStack.of(SgBlocks.REFABRICATOR), "group.alloy_making.fabric");
    static final EmiRecipeCategory ALLOY_GEM = category("alloy_making/gem", EmiStack.of(SgBlocks.RECRYSTALLIZER), "group.alloy_making.gem");
    static final EmiRecipeCategory ALLOY_METAL = category("alloy_making/metal", EmiStack.of(SgBlocks.ALLOY_FORGE), "group.alloy_making.metal");
    static final EmiRecipeCategory ALLOY_SUPER = category("alloy_making/super", EmiStack.of(SgBlocks.SUPER_MIXER), "group.alloy_making.super");
    static final EmiRecipeCategory GRADING = category("grading", EmiStack.of(SgBlocks.MATERIAL_GRADER), "group.grading");
    static final EmiRecipeCategory SALVAGING = category("salvaging", EmiStack.of(SgBlocks.SALVAGER), "group.salvaging");
    static final EmiRecipeCategory TOOL_ACTION = category("tool_action", EmiStack.of(SgBlocks.STONE_ANVIL), "group.tool_action");

    private static EmiRecipeCategory category(String path, EmiStack icon, String langKey) {
        Component name = TextUtil.translate("jei", langKey);
        return new EmiRecipeCategory(SilentGear.getId(path), icon) {
            @Override
            public Component getName() {
                return name;
            }
        };
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(GEAR_CRAFTING);
        registry.addCategory(ALLOY_FABRIC);
        registry.addCategory(ALLOY_GEM);
        registry.addCategory(ALLOY_METAL);
        registry.addCategory(ALLOY_SUPER);
        registry.addCategory(GRADING);
        registry.addCategory(SALVAGING);
        registry.addCategory(TOOL_ACTION);

        registry.addWorkstation(GEAR_CRAFTING, EmiStack.of(Blocks.CRAFTING_TABLE));
        registry.addWorkstation(ALLOY_FABRIC, EmiStack.of(SgBlocks.REFABRICATOR));
        registry.addWorkstation(ALLOY_GEM, EmiStack.of(SgBlocks.RECRYSTALLIZER));
        registry.addWorkstation(ALLOY_METAL, EmiStack.of(SgBlocks.ALLOY_FORGE));
        registry.addWorkstation(ALLOY_SUPER, EmiStack.of(SgBlocks.SUPER_MIXER));
        registry.addWorkstation(GRADING, EmiStack.of(SgBlocks.MATERIAL_GRADER));
        registry.addWorkstation(SALVAGING, EmiStack.of(SgBlocks.SALVAGER));
        registry.addWorkstation(TOOL_ACTION, EmiStack.of(SgBlocks.STONE_ANVIL));

        // Custom ingots/gems differ only by their material component (JEI subtype interpreter)
        Comparison byMaterial = Comparison.compareData(stack -> {
            MaterialInstance material = SingleMaterialItem.getMaterial(stack.getItemStack());
            return material != null ? material.getId() : null;
        });
        registry.setDefaultComparison(SgItems.CUSTOM_GEM.get(), byMaterial);
        registry.setDefaultComparison(SgItems.CUSTOM_INGOT.get(), byMaterial);

        // Recipes arrive with the datapack sync (ClientRecipeHelper); read them after all plugins registered
        registry.addDeferredRecipes(consumer -> {
            // Repair kit hints
            for (RepairKitItem item : SgItems.getItems(RepairKitItem.class)) {
                EmiIngredient material = EmiIngredient.of(new Ingredient(PartMaterialIngredient.of(PartTypes.MAIN.get())));
                consumer.accept(new EmiCraftingRecipe(List.of(EmiStack.of(item), material, material, material), EmiStack.of(item),
                        synthetic("repair_kit/" + NameUtils.fromItem(item).getPath()), true));
            }

            for (RecipeHolder<?> holder : ClientRecipeHelper.getRecipes(RecipeType.CRAFTING)) {
                if (holder.value() instanceof CraftingRecipeExtension recipe) {
                    consumer.accept(new SGearEmiRecipes.GearCrafting(holder.id().identifier(), recipe));
                }
            }
            for (RecipeHolder<?> holder : ClientRecipeHelper.getRecipes(SgRecipes.TOOL_ACTION_TYPE.get())) {
                consumer.accept(new SGearEmiRecipes.ToolAction(holder.id().identifier(), (ToolActionRecipe) holder.value()));
            }
            addAlloys(consumer, ALLOY_FABRIC, Const.FABRIC_ALLOY_MAKER_INFO, SgRecipes.ALLOY_MAKING_FABRIC_TYPE.get(), FabricAlloyRecipe::new, "fabric");
            addAlloys(consumer, ALLOY_GEM, Const.GEM_ALLOY_MAKER_INFO, SgRecipes.ALLOY_MAKING_GEM_TYPE.get(), GemAlloyRecipe::new, "gem");
            addAlloys(consumer, ALLOY_METAL, Const.METAL_ALLOY_MAKER_INFO, SgRecipes.ALLOY_MAKING_METAL_TYPE.get(), MetalAlloyRecipe::new, "metal");
            addAlloys(consumer, ALLOY_SUPER, Const.SUPER_MIXER_INFO, SgRecipes.ALLOY_MAKING_SUPER_TYPE.get(), SuperAlloyRecipe::new, "super");

            consumer.accept(new SGearEmiRecipes.Grading(synthetic("grading")));

            for (RecipeHolder<?> holder : ClientRecipeHelper.getRecipes(SgRecipes.SALVAGING_TYPE.get())) {
                consumer.accept(new SGearEmiRecipes.Salvaging(holder.id().identifier(), (SalvagingRecipe) holder.value()));
            }

            addInfo(consumer, CraftingItems.RED_CARD_UPGRADE);
            addInfo(consumer, CraftingItems.SPOON_UPGRADE);
            for (Item item : SgItems.getItems(item -> item instanceof GearTool)) {
                addInfo(consumer, item);
            }
        });

        // Material aliases ("materials", "materials/<category>")
        for (Material material : SgRegistries.MATERIAL.getValues(true)) {
            List<EmiIngredient> stacks = new ArrayList<>();
            IngredientUtils.getItemList(material.getIngredient()).forEach(stack -> stacks.add(EmiStack.of(stack)));
            if (stacks.isEmpty()) continue;
            EmiIngredient ingredient = EmiIngredient.of(stacks);
            registry.addAlias(ingredient, Component.literal("materials"));
            for (IMaterialCategory category : material.getCategories(MaterialInstance.of(material))) {
                registry.addAlias(ingredient, Component.literal("materials/" + category.getName()));
            }
        }
    }

    private static <R extends AlloyRecipe> void addAlloys(java.util.function.Consumer<dev.emi.emi.api.recipe.EmiRecipe> consumer,
                                                          EmiRecipeCategory category, AlloyMakerInfo<?> info, RecipeType<?> type,
                                                          BiFunction<AlloyRecipe.Result, List<Ingredient>, R> factory, String name) {
        for (RecipeHolder<?> holder : ClientRecipeHelper.getRecipes(type)) {
            consumer.accept(new SGearEmiRecipes.Alloy(category, info, holder.id().identifier(), (AlloyRecipe) holder.value()));
        }
        for (int i = 2; i <= 4; ++i) {
            consumer.accept(new SGearEmiRecipes.Alloy(category, info, synthetic("alloy_making/" + name + "/example_" + i),
                    AlloyRecipe.makeExample(info, i, factory)));
        }
    }

    private static void addInfo(java.util.function.Consumer<dev.emi.emi.api.recipe.EmiRecipe> consumer, ItemLike item) {
        Identifier name = NameUtils.fromItem(item);
        String key = "jei." + name.getNamespace() + "." + name.getPath() + ".desc";
        consumer.accept(new EmiInfoRecipe(List.of(EmiStack.of(item)), List.of(Component.translatable(key)), synthetic("info/" + name.getPath())));
    }

    /** EMI convention for generated recipes: path starts with '/'. */
    static Identifier synthetic(String path) {
        return SilentGear.getId("/" + path);
    }
}
