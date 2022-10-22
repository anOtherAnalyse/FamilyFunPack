package family_fun_pack.commands;

import family_fun_pack.FamilyFunPack;
import family_fun_pack.modules.CommandsModule;
import family_fun_pack.network.PacketListener;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/* Dump all commands usage */

@SideOnly(Side.CLIENT)
public class HelpCommand extends Command {

  public HelpCommand() {
    super("help");
  }

  public String usage() {
    return this.getName();
  }

  public String execute(String[] args) {
    final StringBuilder sb = new StringBuilder();
    final List<Command> list = new ArrayList<>(CommandsModule.getCommands().getCommands());

    list.sort(Comparator.comparing(Command::getName));

    for (Command command : list) {
        sb.append("\n").append(CommandsModule.ESCAPE_CHARACTER).append(command.usage());
    }

    return sb.toString().replaceFirst("\n", "");
  }
}
