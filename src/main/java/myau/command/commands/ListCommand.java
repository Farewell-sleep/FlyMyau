package myau.command.commands;

import myau.OpenMyau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super(new ArrayList<>(Arrays.asList("list", "l", "modules", "myau")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!OpenMyau.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sModules:&r", OpenMyau.clientName));
            for (Module module : OpenMyau.moduleManager.modules.values()) {
                ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
            }
        }
    }
}
