package arnett.radio.Commands.CommandTree;

import arnett.radio.Commands.BranchCommand;
import arnett.radio.Commands.SubCommand;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class ManageBranch extends BranchCommand {

    public ManageBranch(HashMap<SubCommand, String> map) {
        super(map);
    }

    @Override
    public String getName() {
        return "manage";
    }

    @Override
    public String getDescription() {
        return "All commands related to managing connections";
    }

    @Override
    public String getSyntax() {
        return "/radio manage <sub command>";
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.manage");
    }
}
