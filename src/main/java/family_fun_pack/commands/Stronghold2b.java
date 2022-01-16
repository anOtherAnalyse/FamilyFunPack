package family_fun_pack.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Array;


@SideOnly(Side.CLIENT)
public class Stronghold2b extends Command {

    public Stronghold2b() {
        super("stronghold");
    }

    public String usage() {
        return this.getName();
    }

    private int DistSquared(int x1, int z1, int x2, int z2) {
        int diffX = x1 - x2;
        int diffZ = z1 - z2;
        return (diffX * diffX + diffZ * diffZ);
    }

    public String execute(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.player.dimension == 1) return "dont you feel stupid... dont you feel a little ashamed...";

        int[][] endPortalCoords = {{1888, -32}, {-560, 1504}, {2064, -4400}, {-4992, -512}, {2960, 4208}, {-3200,4480}, {-5568,608}, {-2496,5296}};

        int closestX = endPortalCoords[0][0];
        int closestZ = endPortalCoords[0][1];
        int shortestDistance = DistSquared(mc.player.getPosition().getX(), mc.player.getPosition().getZ(), endPortalCoords[0][0], endPortalCoords[0][1]);
        for (int i = 0; i < endPortalCoords.length; i++) {
            System.out.println(i);
            int d = DistSquared(mc.player.getPosition().getX(), mc.player.getPosition().getZ(), endPortalCoords[i][0], endPortalCoords[i][1]);
            if (d < shortestDistance) {
                closestX = endPortalCoords[i][0];
                closestZ = endPortalCoords[i][1];
                shortestDistance = d;

            }
        }

        return String.format("Nearest stronghold around (%d, %d) overworld", closestX, closestZ);


    }
}