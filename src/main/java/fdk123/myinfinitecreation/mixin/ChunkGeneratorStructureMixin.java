package fdk123.myinfinitecreation.mixin;

import fdk123.myinfinitecreation.worldgen.StructureGenerationPolicy;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorStructureMixin {
    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void myinfinitecreation$filterStructure(StructureSet.StructureSelectionEntry entry,
            StructureManager structureManager, RegistryAccess registryAccess, RandomState randomState,
            StructureTemplateManager templateManager, long seed, ChunkAccess chunk, ChunkPos chunkPos,
            SectionPos sectionPos, CallbackInfoReturnable<Boolean> cir) {
        entry.structure().unwrapKey().ifPresent(key -> {
            if (!StructureGenerationPolicy.allows(key.location(), seed, chunkPos.x, chunkPos.z)) {
                cir.setReturnValue(false);
            }
        });
    }
}
