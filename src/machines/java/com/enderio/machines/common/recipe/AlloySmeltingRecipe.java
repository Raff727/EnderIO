package com.enderio.machines.common.recipe;

import com.enderio.EnderIO;
import com.enderio.core.common.recipes.CountedIngredient;
import com.enderio.core.common.recipes.OutputStack;
import com.enderio.machines.common.blockentity.AlloySmelterBlockEntity;
import com.enderio.machines.common.init.MachineRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AlloySmeltingRecipe implements MachineRecipe<AlloySmeltingRecipe.ContainerWrapper> {

    private final ResourceLocation id;
    private final List<CountedIngredient> inputs;
    private final ItemStack output;
    private final int energy;
    private final float experience;

    public AlloySmeltingRecipe(ResourceLocation id, List<CountedIngredient> inputs, ItemStack output, int energy, float experience) {
        this.id = id;
        this.inputs = inputs;
        this.output = output;
        this.energy = energy;
        this.experience = experience;
    }

    /**
     * Get the inputs for the alloy smelting recipe.
     */
    public List<CountedIngredient> getInputs() {
        return inputs;
    }

    /**
     * Get the amount of experience generated by the recipe.
     */
    public float getExperience() {
        return experience;
    }

    @Override
    public int getBaseEnergyCost() {
        return energy;
    }

    @Override
    public boolean matches(ContainerWrapper container, Level level) {
        boolean[] matched = new boolean[3];

        // Iterate over the slots
        for (int i = 0; i < 3; i++) {
            // Iterate over the inputs
            for (int j = 0; j < 3; j++) {
                // If this ingredient has been matched already, continue
                if (matched[j])
                    continue;

                if (j < inputs.size()) {
                    // If we expect an input, test we have a match for it.
                    if (inputs.get(j).test(AlloySmelterBlockEntity.INPUTS.get(i).getItemStack(container))) {
                        matched[j] = true;
                    }
                } else if (container.getItem(i) == ItemStack.EMPTY) {
                    // If we don't expect an input, make sure we have a blank for it.
                    matched[j] = true;
                }
            }
        }

        // If we matched all our ingredients, we win!
        for (int i = 0; i < 3; i++) {
            if (!matched[i])
                return false;
        }

        return true;
    }

    @Override
    public List<OutputStack> craft(ContainerWrapper container, RegistryAccess registryAccess) {
         return List.of(OutputStack.of(output.copy()));
    }

    @Override
    public List<OutputStack> getResultStacks(RegistryAccess registryAccess) {
        return List.of(OutputStack.of(output.copy()));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MachineRecipes.ALLOY_SMELTING.serializer().get();
    }

    @Override
    public RecipeType<?> getType() {
        return MachineRecipes.ALLOY_SMELTING.type().get();
    }

    /**
     * The recipe container.
     * This acts as additional context.
     */
    public static class ContainerWrapper extends RecipeWrapper {

        private int inputsTaken;

        public ContainerWrapper(IItemHandlerModifiable inv) {
            super(inv);
        }

        public int getInputsTaken() {
            return inputsTaken;
        }

        public void setInputsTaken(int inputsTaken) {
            this.inputsTaken = inputsTaken;
        }
    }

    public static class Serializer implements RecipeSerializer<AlloySmeltingRecipe> {

        @Override
        public AlloySmeltingRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            // Load ingredients
            JsonArray jsonInputs = serializedRecipe.getAsJsonArray("inputs");
            List<CountedIngredient> inputs = new ArrayList<>(jsonInputs.size());
            for (int i = 0; i < jsonInputs.size(); i++) {
                inputs.add(i, CountedIngredient.fromJson(jsonInputs.get(i).getAsJsonObject()));
            }

            // Load result, energy and experience.
            ItemStack result = CraftingHelper.getItemStack(serializedRecipe.getAsJsonObject("result"), false);
            int energy = serializedRecipe.get("energy").getAsInt();
            float experience = serializedRecipe.get("experience").getAsFloat();
            return new AlloySmeltingRecipe(recipeId, inputs, result, energy, experience);
        }

        @Nullable
        @Override
        public AlloySmeltingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            try {
                List<CountedIngredient> ingredients = buffer.readList(CountedIngredient::fromNetwork);
                ItemStack result = buffer.readItem();
                int energy = buffer.readInt();
                float experience = buffer.readFloat();
                return new AlloySmeltingRecipe(recipeId, ingredients, result, energy, experience);
            } catch (Exception ex) {
                EnderIO.LOGGER.error("Error reading alloy smelting recipe from packet.", ex);
                throw ex;
            }
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AlloySmeltingRecipe recipe) {
            try {
                buffer.writeCollection(recipe.inputs, (buf, ing) -> ing.toNetwork(buf));
                buffer.writeItem(recipe.output);
                buffer.writeInt(recipe.energy);
                buffer.writeFloat(recipe.experience);
            } catch (Exception ex) {
                EnderIO.LOGGER.error("Error writing alloy smelting recipe to packet.", ex);
                throw ex;
            }
        }
    }
}
