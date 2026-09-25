package myau.command.commands;

import myau.OpenMyau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class TargetCommand extends Command {
    public TargetCommand() {
        super(new ArrayList<>(Arrays.asList("enemy", "e", "target")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            String subCommand = args.get(1).toLowerCase(Locale.ROOT);
            switch (subCommand) {
                case "add":
                    if (args.size() < 3) {
                        ChatUtil.sendFormatted(
                                String.format("%sUsage: .%s add <&oname&r>&r", OpenMyau.clientName, args.get(0).toLowerCase(Locale.ROOT))
                        );
                        return;
                    }
                    String added = OpenMyau.targetManager.add(args.get(2));
                    if (added == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is already in your enemy list&r", OpenMyau.clientName, args.get(2)));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your enemy list&r", OpenMyau.clientName, added));
                    return;
                case "remove":
                    if (args.size() < 3) {
                        ChatUtil.sendFormatted(
                                String.format("%sUsage: .%s remove <&oname&r>&r", OpenMyau.clientName, args.get(0).toLowerCase(Locale.ROOT))
                        );
                        return;
                    }
                    String removed = OpenMyau.targetManager.remove(args.get(2));
                    if (removed == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is not in your enemy list&r", OpenMyau.clientName, args.get(2)));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your enemy list&r", OpenMyau.clientName, removed));
                    return;
                case "list":
                    ArrayList<String> list = OpenMyau.targetManager.getPlayers();
                    if (list.isEmpty()) {
                        ChatUtil.sendFormatted(String.format("%sNo enemies&r", OpenMyau.clientName));
                        return;
                    }
                    ChatUtil.sendFormatted(String.format("%sEnemies:&r", OpenMyau.clientName));
                    for (String player : list) {
                        ChatUtil.sendRaw(String.format(ChatColors.formatColor("   &o%s&r"), player));
                    }
                    return;
                case "clear":
                    OpenMyau.targetManager.clear();
                    ChatUtil.sendFormatted(String.format("%sCleared your enemy list&r", OpenMyau.clientName));
                    return;
            }
        }
        ChatUtil.sendFormatted(
                String.format("%sUsage: .%s <&oadd&r/&oremove&r/&olist&r/&oclear&r>&r", OpenMyau.clientName, args.get(0).toLowerCase(Locale.ROOT))
        );
    }
}
