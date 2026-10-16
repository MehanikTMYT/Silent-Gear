package net.silentchaos512.gear.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.material.Material;
import net.silentchaos512.gear.block.alloymaker.AlloyMakerInfo;
import net.silentchaos512.gear.block.grader.GraderScreen;
import net.silentchaos512.gear.block.salvager.SalvagerScreen;
import net.silentchaos512.gear.crafting.ingredient.IGearIngredient;
import net.silentchaos512.gear.crafting.recipe.ToolActionRecipe;
import net.silentchaos512.gear.crafting.recipe.alloy.AlloyRecipe;
import net.silentchaos512.gear.crafting.recipe.salvage.SalvagingRecipe;
import net.silentchaos512.gear.setup.SgBlocks;
import net.silentchaos512.gear.setup.SgRegistries;
import net.silentchaos512.gear.setup.SgTags;
import net.silentchaos512.gear.util.IngredientUtils;
import net.silentchaos512.lib.crafting.recipe.CraftingRecipeExtension;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/** EMI displays; layouts (textures, slot positions, arrows) follow the former JEI categories. */
final class SGearEmiRecipes {
    private SGearEmiRecipes() {}

    private static EmiIngredient items(List<ItemStack> stacks) {
        List<EmiIngredient> list = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) list.add(EmiStack.of(stack));
        return EmiIngredient.of(list);
    }

    /** Shaped/shapeless gear crafting with the per-slot material hints that JEI drew under the grid. */
    static final class GearCrafting extends BasicEmiRecipe {
        private static final Identifier TEXTURE = SilentGear.getId("textures/gui/gear_crafting_jei.png");
        private final List<Component> hints = new ArrayList<>();

        GearCrafting(Identifier id, CraftingRecipeExtension recipe) {
            super(SGearEmiPlugin.GEAR_CRAFTING, id, 160, 132);
            List<Ingredient> ingredients = recipe.placementInfo().ingredients();
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i);
                this.inputs.add(EmiIngredient.of(ingredient));
                if (ingredient.getCustomIngredient() instanceof IGearIngredient gear) {
                    int index = i;
                    gear.getJeiHint().ifPresent(text -> hints.add(Component.literal((index + 1) + ": ").append(text)));
                }
            }
            this.outputs.add(EmiStack.of(recipe.getResultForDisplay().create()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(TEXTURE, 0, 0, 160, 132, 0, 0);
            for (int y = 0; y < 3; ++y) {
                for (int x = 0; x < 3; ++x) {
                    int index = x + y * 3;
                    widgets.addSlot(index < inputs.size() ? inputs.get(index) : EmiStack.EMPTY, x * 18, y * 18).drawBack(false);
                }
            }
            widgets.addSlot(outputs.get(0), 94, 18).drawBack(false).recipeContext(this);
            if (!hints.isEmpty()) {
                float scale = hints.size() > 5 ? 0.75f : 1f;
                widgets.addDrawable(0, 56, 160, 76, (graphics, mouseX, mouseY, delta) -> {
                    Matrix3x2fStack matrix = graphics.pose();
                    matrix.pushMatrix();
                    matrix.scale(scale, scale, matrix);
                    Font font = Minecraft.getInstance().font;
                    int ty = 0;
                    for (Component line : hints) {
                        graphics.text(font, line.getVisualOrderText(), 0, ty, -1, true);
                        ty += 10;
                    }
                    matrix.popMatrix();
                });
            }
        }
    }

    static final class Alloy extends BasicEmiRecipe {
        private static final Identifier TEXTURE = SilentGear.getId("textures/gui/alloy_maker.png");
        private static final int X0 = 15, Y0 = 29;
        private final int slotCount;

        Alloy(EmiRecipeCategory category, AlloyMakerInfo<?> info, Identifier id, AlloyRecipe recipe) {
            super(category, id, 147 - X0, 56 - Y0);
            List<Ingredient> ingredients = recipe.placementInfo().ingredients();
            this.slotCount = Math.min(info.getInputSlotCount(), ingredients.size());
            for (int i = 0; i < slotCount; ++i) {
                this.inputs.add(items(IngredientUtils.getValidItems(ingredients.get(i))));
            }
            this.outputs.add(EmiStack.of(recipe.getResultForRecipeDisplay()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(TEXTURE, 0, 0, width, height, X0, Y0);
            widgets.addAnimatedTexture(TEXTURE, 93 - X0, 34 - Y0, 24, 17, 176, 14, 10_000, true, false, false);
            for (int i = 0; i < slotCount; ++i) {
                widgets.addSlot(inputs.get(i), 18 * i + 16 - X0, 34 - Y0).drawBack(false);
            }
            widgets.addSlot(outputs.get(0), 125 - X0, 34 - Y0).drawBack(false).recipeContext(this);
        }
    }

    /** Material grader: any material + any catalyst (no output; grading is random). */
    static final class Grading extends BasicEmiRecipe {
        private static final int X0 = 24, Y0 = 33;

        Grading(Identifier id) {
            super(SGearEmiPlugin.GRADING, id, 43 - X0, 72 - Y0);
            List<ItemStack> materials = new ArrayList<>();
            for (Material material : SgRegistries.MATERIAL.getValues(true)) {
                materials.addAll(IngredientUtils.getValidItems(material.getIngredient()));
            }
            this.inputs.add(items(materials));
            this.catalysts.add(EmiIngredient.of(SgTags.Items.GRADER_CATALYSTS));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(GraderScreen.TEXTURE, 0, 0, width, height, X0, Y0);
            widgets.addSlot(inputs.get(0), 1, 1).drawBack(false);
            widgets.addSlot(catalysts.get(0), 1, 21).drawBack(false);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }
    }

    static final class Salvaging extends BasicEmiRecipe {
        private static final int X0 = 9, Y0 = 17;

        Salvaging(Identifier id, SalvagingRecipe recipe) {
            super(SGearEmiPlugin.SALVAGING, id, 114 - X0, 69 - Y0);
            this.inputs.add(items(IngredientUtils.getItemList(recipe.getIngredient())));
            List<ItemStackTemplate> results = recipe.getPossibleResultsForDisplay();
            for (int i = 0; i < 9 && i < results.size(); ++i) {
                this.outputs.add(EmiStack.of(results.get(i).create()));
            }
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(SalvagerScreen.TEXTURE, 0, 0, width, height, X0, Y0);
            widgets.addAnimatedTexture(SalvagerScreen.TEXTURE, 32 - X0, 34 - Y0, 24, 17, 176, 14, 10_000, true, false, false);
            widgets.addSlot(inputs.get(0), 8 - X0, 34 - Y0).drawBack(false);
            for (int i = 0; i < outputs.size(); ++i) {
                widgets.addSlot(outputs.get(i), 18 * (i % 3) + 61 - X0, 18 * (i / 3) + 16 - Y0).drawBack(false).recipeContext(this);
            }
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }
    }

    /** Stone anvil: tool + ingredient -> result. */
    static final class ToolAction extends BasicEmiRecipe {
        private static final Identifier TEXTURE = SilentGear.getId("textures/gui/tool_action_jei.png");

        ToolAction(Identifier id, ToolActionRecipe recipe) {
            super(SGearEmiPlugin.TOOL_ACTION, id, 100, 41);
            this.catalysts.add(EmiIngredient.of(recipe.getTool()));
            this.inputs.add(EmiIngredient.of(recipe.getIngredient()));
            this.outputs.add(EmiStack.of(recipe.getResult().create()));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(TEXTURE, 0, 0, 100, 41, 0, 0);
            widgets.addSlot(catalysts.get(0), 2, 2).drawBack(false);
            widgets.addSlot(inputs.get(0), 21, 2).drawBack(false);
            widgets.addSlot(EmiStack.of(SgBlocks.STONE_ANVIL), 21, 21).drawBack(false).catalyst(true);
            widgets.addSlot(outputs.get(0), 75, 11).drawBack(false).recipeContext(this);
        }
    }
}
