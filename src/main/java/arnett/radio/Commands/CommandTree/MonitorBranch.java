package arnett.radio.Commands.CommandTree;

import arnett.radio.Commands.BranchCommand;
import arnett.radio.Commands.SubCommand;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class MonitorBranch extends BranchCommand {

    public MonitorBranch(HashMap<SubCommand, String> map) {
        super(map);
    }

    @Override
    public String getName() {
        return "monitor";
    }

    @Override
    public String getDescription() {
        return "All commands related to monitoring";
    }

    @Override
    public String getSyntax() {
        return "/radio monitor <sub command>";
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.monitor");
    }
}
