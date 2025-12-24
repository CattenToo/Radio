package arnett.radio.Commands.CommandTree;

import arnett.radio.Commands.BranchCommand;
import arnett.radio.Commands.SubCommand;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class GiveBranch extends BranchCommand {

    public GiveBranch(HashMap<SubCommand, String> map) {
        super(map);
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getDescription() {
        return "All commands related to giving items";
    }

    @Override
    public String getSyntax() {
        return "/radio give <sub command>";
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.give");
    }
}
